package com.drogpulseai.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Product;
import com.drogpulseai.repository.ProductRepository;
import com.drogpulseai.sync.SyncManager;
import com.drogpulseai.utils.NetworkResult;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Worker pour synchroniser les produits en arrière-plan
 */
public class ProductSyncWorker extends Worker {
    private static final String TAG = "ProductSyncWorker";

    private final ProductRepository repository;
    private final ApiService apiService;
    private final SyncManager syncManager;

    public ProductSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        repository = new ProductRepository(context);
        apiService = ApiClient.getApiService();
        syncManager = SyncManager.getInstance((android.app.Application) context.getApplicationContext());
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Démarrage de la synchronisation des produits");

        // Récupérer les IDs des produits à synchroniser
        int[] productIds = getInputData().getIntArray("product_ids");

        if (productIds == null || productIds.length == 0) {
            Log.d(TAG, "Aucun produit à synchroniser");
            return Result.success();
        }

        List<Integer> failedSyncs = new ArrayList<>();

        // Synchroniser chaque produit
        for (int productId : productIds) {
            try {
                boolean success = syncProduct(productId);

                if (success) {
                    // Retirer le produit de la liste des produits en attente
                    syncManager.removeProductFromSync(productId);
                } else {
                    // Garder le produit pour une synchronisation ultérieure
                    failedSyncs.add(productId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la synchronisation du produit " + productId, e);
                failedSyncs.add(productId);
            }
        }

        // Si certains produits n'ont pas pu être synchronisés
        if (!failedSyncs.isEmpty()) {
            Log.d(TAG, failedSyncs.size() + " produits n'ont pas pu être synchronisés");
            return Result.retry();
        }

        Log.d(TAG, "Synchronisation terminée avec succès");
        return Result.success();
    }

    /**
     * Synchroniser un produit avec le serveur
     * @param productId L'ID du produit à synchroniser
     * @return true si la synchronisation a réussi, false sinon
     */
    private boolean syncProduct(int productId) throws Exception {
        // Récupérer le produit depuis le repository local
        Product product = repository.getProductById(productId);

        if (product == null) {
            Log.e(TAG, "Produit introuvable dans le cache local: " + productId);
            return true; // Considéré comme un succès pour retirer de la liste
        }

        // Vérifier si c'est un ID temporaire (négatif)
        boolean isLocalOnly = productId < 0;

        if (isLocalOnly) {
            // C'est un nouveau produit créé localement
            return createProductOnServer(product);
        } else {
            // C'est une mise à jour d'un produit existant
            return updateProductOnServer(product);
        }
    }

    /**
     * Créer un produit sur le serveur
     * @param product Le produit à créer
     * @return true si la création a réussi, false sinon
     */
    private boolean createProductOnServer(Product product) throws Exception {
        // Nettoyer les données temporaires
        int localId = product.getId();
        product.setId(0); // L'API génèrera un nouvel ID
        product.setDirty(false);

        // Appel synchrone à l'API (nous sommes déjà dans un thread secondaire)
        Call<NetworkResult<Product>> call = apiService.createProduct(product);
        Response<NetworkResult<Product>> response = call.execute();

        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            // Récupérer le produit créé avec son ID serveur
            Product createdProduct = response.body().getData();

            // Mettre à jour le produit local
            repository.deleteProduct(localId); // Supprimer l'ancienne entrée avec l'ID temporaire
            repository.insertOrUpdateProduct(createdProduct); // Ajouter la nouvelle entrée avec l'ID serveur

            Log.d(TAG, "Produit créé sur le serveur avec succès: " + createdProduct.getId());
            return true;
        } else {
            Log.e(TAG, "Erreur lors de la création du produit sur le serveur: " +
                    (response.errorBody() != null ? response.errorBody().string() : "Erreur inconnue"));
            return false;
        }
    }

    /**
     * Mettre à jour un produit sur le serveur
     * @param product Le produit à mettre à jour
     * @return true si la mise à jour a réussi, false sinon
     */
    private boolean updateProductOnServer(Product product) throws Exception {
        // Nettoyer les données temporaires
        product.setDirty(false);

        // Appel synchrone à l'API (nous sommes déjà dans un thread secondaire)
        Call<NetworkResult<Product>> call = apiService.updateProduct(product);
        Response<NetworkResult<Product>> response = call.execute();

        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
            // Récupérer le produit mis à jour
            Product updatedProduct = response.body().getData();

            // Mettre à jour le produit local
            repository.insertOrUpdateProduct(updatedProduct);

            Log.d(TAG, "Produit mis à jour sur le serveur avec succès: " + updatedProduct.getId());
            return true;
        } else {
            Log.e(TAG, "Erreur lors de la mise à jour du produit sur le serveur: " +
                    (response.errorBody() != null ? response.errorBody().string() : "Erreur inconnue"));
            return false;
        }
    }
}