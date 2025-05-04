package com.drogpulseai.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Contact;
import com.drogpulseai.repository.ContactRepository;
import com.drogpulseai.sync.SyncManager;
import com.drogpulseai.utils.NetworkResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Worker pour synchroniser les contacts en arrière-plan
 */
public class ContactSyncWorker extends Worker {
    private static final String TAG = "ContactSyncWorker";

    private final ContactRepository repository;
    private final ApiService apiService;
    private final SyncManager syncManager;

    public ContactSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        repository = new ContactRepository(context);
        apiService = ApiClient.getApiService();
        syncManager = SyncManager.getInstance((android.app.Application) context.getApplicationContext());
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Démarrage de la synchronisation des contacts");

        // Récupérer les IDs des contacts à synchroniser
        int[] contactIds = getInputData().getIntArray("contact_ids");

        if (contactIds == null || contactIds.length == 0) {
            Log.d(TAG, "Aucun contact à synchroniser");
            return Result.success();
        }

        List<Integer> failedSyncs = new ArrayList<>();

        // Synchroniser chaque contact
        for (int contactId : contactIds) {
            try {
                boolean success = syncContact(contactId);

                if (success) {
                    // Retirer le contact de la liste des contacts en attente
                    syncManager.removeContactFromSync(contactId);
                } else {
                    // Garder le contact pour une synchronisation ultérieure
                    failedSyncs.add(contactId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la synchronisation du contact " + contactId, e);
                failedSyncs.add(contactId);
            }
        }

        // Si certains contacts n'ont pas pu être synchronisés
        if (!failedSyncs.isEmpty()) {
            Log.d(TAG, failedSyncs.size() + " contacts n'ont pas pu être synchronisés");
            return Result.retry();
        }

        Log.d(TAG, "Synchronisation terminée avec succès");
        return Result.success();
    }

    /**
     * Synchroniser un contact avec le serveur
     * @param contactId L'ID du contact à synchroniser
     * @return true si la synchronisation a réussi, false sinon
     */
    private boolean syncContact(int contactId) throws Exception {
        // Récupérer le contact depuis le repository local
        Contact contact = repository.getContactById(contactId);

        if (contact == null) {
            Log.e(TAG, "Contact introuvable dans le cache local: " + contactId);
            return true; // Considéré comme un succès pour retirer de la liste
        }

        // Vérifier si c'est un ID temporaire (négatif)
        boolean isLocalOnly = contactId < 0;

        if (isLocalOnly) {
            // C'est un nouveau contact créé localement
            return createContactOnServer(contact);
        } else {
            // C'est une mise à jour d'un contact existant
            return updateContactOnServer(contact);
        }
    }

    /**
     * Créer un contact sur le serveur
     * @param contact Le contact à créer
     * @return true si la création a réussi, false sinon
     */
    private boolean createContactOnServer(Contact contact) throws Exception {
        // Nettoyer les données temporaires
        int localId = contact.getId();
        contact.setId(0); // L'API générera un nouvel ID
        contact.setDirty(false);

        // Appel synchrone à l'API (nous sommes déjà dans un thread secondaire)
        Call<Map<String, Object>> call = apiService.createContact(contact);
        Response<Map<String, Object>> response = call.execute();

        if (response.isSuccessful() && response.body() != null && (boolean) response.body().get("success")) {
            // Récupérer le contact créé avec son ID serveur
            Map<String, Object> responseData = (Map<String, Object>) response.body().get("contact");
            int serverId = ((Double) responseData.get("id")).intValue();

            // Créer une nouvelle instance avec l'ID serveur
            Contact createdContact = new Contact(
                    contact.getNom(),
                    contact.getPrenom(),
                    contact.getTelephone(),
                    contact.getEmail(),
                    contact.getNotes(),
                    contact.getLatitude(),
                    contact.getLongitude(),
                    contact.getUserId()
            );
            createdContact.setId(serverId);

            // Mettre à jour le contact local
            repository.deleteContact(localId); // Supprimer l'ancienne entrée avec l'ID temporaire
            repository.insertOrUpdateContact(createdContact); // Ajouter la nouvelle entrée avec l'ID serveur

            Log.d(TAG, "Contact créé sur le serveur avec succès: " + createdContact.getId());
            return true;
        } else {
            Log.e(TAG, "Erreur lors de la création du contact sur le serveur: " +
                    (response.errorBody() != null ? response.errorBody().string() : "Erreur inconnue"));
            return false;
        }
    }

    /**
     * Mettre à jour un contact sur le serveur
     * @param contact Le contact à mettre à jour
     * @return true si la mise à jour a réussi, false sinon
     */
    private boolean updateContactOnServer(Contact contact) throws Exception {
        // Nettoyer les données temporaires
        contact.setDirty(false);

        // Appel synchrone à l'API (nous sommes déjà dans un thread secondaire)
        Call<Map<String, Object>> call = apiService.updateContact(contact);
        Response<Map<String, Object>> response = call.execute();

        if (response.isSuccessful() && response.body() != null && (boolean) response.body().get("success")) {
            // Mettre à jour le contact local
            repository.insertOrUpdateContact(contact);

            Log.d(TAG, "Contact mis à jour sur le serveur avec succès: " + contact.getId());
            return true;
        } else {
            Log.e(TAG, "Erreur lors de la mise à jour du contact sur le serveur: " +
                    (response.errorBody() != null ? response.errorBody().string() : "Erreur inconnue"));
            return false;
        }
    }
}