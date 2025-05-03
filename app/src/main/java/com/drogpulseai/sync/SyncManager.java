package com.drogpulseai.sync;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.workers.ProductSyncWorker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Gestionnaire de synchronisation pour les produits
 * Singleton qui détecte les changements de connectivité et synchronise les produits en attente
 */
public class SyncManager {
    private static final String TAG = "SyncManager";

    // Nom des préférences partagées
    private static final String PREF_NAME = "sync_manager";
    private static final String KEY_PENDING_PRODUCTS = "pending_products";

    // Instance singleton
    private static SyncManager instance;

    // Contexte de l'application
    private final Application application;

    // Préférences partagées
    private final SharedPreferences preferences;

    // Gson pour sérialisation/désérialisation
    private final Gson gson;

    // Récepteur de broadcast pour les changements de connectivité
    private final BroadcastReceiver connectivityReceiver;

    /**
     * Obtenir l'instance singleton
     */
    public static synchronized SyncManager getInstance(Application application) {
        if (instance == null) {
            instance = new SyncManager(application);
        }
        return instance;
    }

    /**
     * Constructeur privé
     */
    private SyncManager(Application application) {
        this.application = application;
        this.preferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();

        // Initialiser le récepteur de broadcast
        this.connectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                    boolean isConnected = NetworkUtils.isNetworkAvailable(context);

                    if (isConnected && hasPendingProducts()) {
                        Log.d(TAG, "Connectivité rétablie. Démarrage de la synchronisation...");
                        scheduleSyncNow();
                    }
                }
            }
        };

        // Enregistrer le récepteur de broadcast
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        application.registerReceiver(connectivityReceiver, filter);
    }

    /**
     * Ajouter un produit à la liste des produits en attente de synchronisation
     */
    public void addProductForSync(int productId) {
        Set<Integer> pendingProducts = getPendingProducts();
        pendingProducts.add(productId);
        savePendingProducts(pendingProducts);

        // Si une connexion est disponible, démarrer la synchronisation immédiatement
        if (NetworkUtils.isNetworkAvailable(application)) {
            scheduleSyncNow();
        }
    }

    /**
     * Retirer un produit de la liste des produits en attente de synchronisation
     */
    public void removeProductFromSync(int productId) {
        Set<Integer> pendingProducts = getPendingProducts();
        pendingProducts.remove(productId);
        savePendingProducts(pendingProducts);
    }

    /**
     * Vérifier s'il y a des produits en attente de synchronisation
     */
    public boolean hasPendingProducts() {
        return !getPendingProducts().isEmpty();
    }

    /**
     * Obtenir le nombre de produits en attente de synchronisation
     */
    public int getPendingCount() {
        return getPendingProducts().size();
    }

    /**
     * Obtenir la liste des produits en attente de synchronisation
     */
    public Set<Integer> getPendingProducts() {
        String json = preferences.getString(KEY_PENDING_PRODUCTS, null);

        if (json == null) {
            return new HashSet<>();
        }

        try {
            Type type = new TypeToken<Set<Integer>>(){}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération des produits en attente", e);
            return new HashSet<>();
        }
    }

    /**
     * Sauvegarder la liste des produits en attente de synchronisation
     */
    private void savePendingProducts(Set<Integer> pendingProducts) {
        try {
            String json = gson.toJson(pendingProducts);
            preferences.edit().putString(KEY_PENDING_PRODUCTS, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la sauvegarde des produits en attente", e);
        }
    }

    /**
     * Planifier une synchronisation immédiate
     */
    public void scheduleSyncNow() {
        if (!hasPendingProducts()) {
            return;
        }

        Set<Integer> productIds = getPendingProducts();

        // Convertir l'ensemble en tableau
        int[] productIdsArray = new int[productIds.size()];
        int i = 0;
        for (Integer id : productIds) {
            productIdsArray[i++] = id;
        }

        // Créer les données d'entrée pour le Worker
        Data inputData = new Data.Builder()
                .putIntArray("product_ids", productIdsArray)
                .build();

        // Définir les contraintes du Worker
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Créer la requête de travail
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(ProductSyncWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build();

        // Planifier le travail unique, en remplaçant tout travail existant
        WorkManager.getInstance(application)
                .enqueueUniqueWork(
                        "product_sync_work",
                        ExistingWorkPolicy.REPLACE,
                        syncRequest
                );

        Log.d(TAG, "Synchronisation planifiée pour " + productIds.size() + " produits");
    }

    /**
     * Nettoyer lors de la destruction de l'application
     */
    public void cleanup() {
        try {
            application.unregisterReceiver(connectivityReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du nettoyage du SyncManager", e);
        }
    }
}