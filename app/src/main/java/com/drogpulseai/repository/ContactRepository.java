package com.drogpulseai.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.drogpulseai.models.Contact;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository pour les données de contacts avec cache local
 * Implémentation simplifiée utilisant SharedPreferences avant l'implémentation complète de Room
 */
public class ContactRepository {

    private static final String TAG = "ContactRepository";
    private static final String PREF_NAME = "contact_cache";
    private static final String KEY_CONTACTS = "contacts";

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private final Executor executor;

    public ContactRepository(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Insérer ou mettre à jour un contact dans le cache local
     */
    public void insertOrUpdateContact(Contact contact) {
        if (contact == null) {
            return;
        }

        executor.execute(() -> {
            try {
                // Récupérer les contacts actuels
                List<Contact> contacts = getAllContacts();

                // Rechercher le contact existant
                boolean found = false;
                for (int i = 0; i < contacts.size(); i++) {
                    if (contacts.get(i).getId() == contact.getId()) {
                        contacts.set(i, contact);
                        found = true;
                        break;
                    }
                }

                // Ajouter si non trouvé
                if (!found) {
                    contacts.add(contact);
                }

                // Sauvegarder la liste mise à jour
                saveContacts(contacts);
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'insertion du contact", e);
            }
        });
    }

    /**
     * Supprimer un contact du cache local
     */
    public void deleteContact(int contactId) {
        executor.execute(() -> {
            try {
                // Récupérer les contacts actuels
                List<Contact> contacts = getAllContacts();

                // Supprimer le contact avec l'ID correspondant
                contacts.removeIf(c -> c.getId() == contactId);

                // Sauvegarder la liste mise à jour
                saveContacts(contacts);
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la suppression du contact", e);
            }
        });
    }

    /**
     * Obtenir un contact par ID depuis le cache local
     */
    public Contact getContactById(int contactId) {
        try {
            List<Contact> contacts = getAllContacts();

            for (Contact contact : contacts) {
                if (contact.getId() == contactId) {
                    return contact;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération du contact", e);
        }

        return null;
    }

    /**
     * Obtenir tous les contacts depuis le cache local
     */
    public List<Contact> getAllContacts() {
        String json = sharedPreferences.getString(KEY_CONTACTS, null);

        if (json == null) {
            return new ArrayList<>();
        }

        try {
            Type type = new TypeToken<List<Contact>>(){}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'analyse des contacts", e);
            return new ArrayList<>();
        }
    }

    /**
     * Obtenir les contacts pour un utilisateur spécifique
     */
    public List<Contact> getContactsForUser(int userId) {
        List<Contact> allContacts = getAllContacts();
        List<Contact> userContacts = new ArrayList<>();

        for (Contact contact : allContacts) {
            if (contact.getUserId() == userId) {
                userContacts.add(contact);
            }
        }

        return userContacts;
    }

    /**
     * Sauvegarder les contacts dans SharedPreferences
     */
    private void saveContacts(List<Contact> contacts) {
        try {
            String json = gson.toJson(contacts);
            sharedPreferences.edit().putString(KEY_CONTACTS, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la sauvegarde des contacts", e);
        }
    }

    /**
     * Effacer tous les contacts en cache
     */
    public void clearCache() {
        executor.execute(() -> {
            sharedPreferences.edit().clear().apply();
        });
    }

    /**
     * Obtenir l'ID local le plus bas (utilisé pour générer des IDs temporaires)
     * Note: Les IDs temporaires sont négatifs pour les distinguer des IDs serveur positifs
     */
    public int getLowestLocalId() {
        List<Contact> contacts = getAllContacts();

        int lowestId = -1; // Valeur par défaut si aucun ID temporaire n'existe encore

        for (Contact contact : contacts) {
            int contactId = contact.getId();

            // Nous ne considérons que les IDs négatifs (locaux)
            if (contactId < 0 && (contactId < lowestId || lowestId == -1)) {
                lowestId = contactId;
            }
        }

        // Si aucun ID local n'a été trouvé, retourner -1
        return lowestId == -1 ? -1 : lowestId;
    }
}