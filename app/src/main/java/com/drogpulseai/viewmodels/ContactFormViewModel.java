package com.drogpulseai.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Contact;
import com.drogpulseai.models.User;
import com.drogpulseai.repository.ContactRepository;
import com.drogpulseai.sync.SyncManager;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel pour ContactFormActivity
 * Gère les opérations de données et la logique métier
 *
 * Version corrigée avec gestion d'erreurs améliorée et validation des paramètres
 */
public class ContactFormViewModel extends AndroidViewModel {

    private static final String TAG = "ContactFormViewModel";

    // ✅ Corrigé: Constants avec valeurs distinctes
    private static final String MODE_CREATE = "create";
    private static final String MODE_EDIT = "edit";
    private static final String MODE_ASSIGN_CONTACT = "assign_contact";
    private static final String MODE_ASSIGN_CART = "assign_cart_to_contact"; // ✅ Valeur corrigée

    // Repository
    private final ContactRepository repository;

    // API client
    private final ApiService apiService;

    // Session manager
    private final SessionManager sessionManager;

    // LiveData
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Contact> contact = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> assignmentSuccess = new MutableLiveData<>(false);

    // State variables
    private String mode = MODE_CREATE;
    private int contactId = -1;
    private int cartId = -1;
    private User currentUser;

    public ContactFormViewModel(@NonNull Application application) {
        super(application);

        Log.d(TAG, "Initializing ContactFormViewModel");

        // Initialize repository
        repository = new ContactRepository(application);

        // Initialize API client
        apiService = ApiClient.getApiService();

        // Initialize session manager
        sessionManager = new SessionManager(application);

        // Get current user
        currentUser = sessionManager.getUser();

        if (currentUser == null) {
            Log.e(TAG, "Current user is null!");
            errorMessage.setValue("Utilisateur non connecté");
        } else {
            Log.d(TAG, "Current user ID: " + currentUser.getId());
        }
    }

    /**
     * ✅ Amélioration: Initialize ViewModel with validation
     */
    public void initializeMode(String mode, int contactId) {
        Log.d(TAG, "initializeMode: mode=" + mode + ", contactId=" + contactId);

        try {
            validateModeParameters(mode, contactId, -1);
            this.mode = mode;
            this.contactId = contactId;
            this.cartId = -1;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid mode parameters", e);
            errorMessage.setValue(e.getMessage());
        }
    }

    /**
     * ✅ Amélioration: Initialize ViewModel with validation for cart operations
     */
    public void initializeMode(String mode, int contactId, int cartId) {
        Log.d(TAG, "initializeMode: mode=" + mode + ", contactId=" + contactId + ", cartId=" + cartId);

        try {
            validateModeParameters(mode, contactId, cartId);
            this.mode = mode;
            this.contactId = contactId;
            this.cartId = cartId;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid mode parameters", e);
            errorMessage.setValue(e.getMessage());
        }
    }

    /**
     * ✅ Nouvelle méthode: Validation des paramètres
     */
    private void validateModeParameters(String mode, int contactId, int cartId) {
        if (mode == null || mode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mode cannot be null or empty");
        }

        switch (mode) {
            case MODE_EDIT:
                if (contactId <= 0) {
                    throw new IllegalArgumentException("Contact ID must be valid for edit mode");
                }
                break;

            case MODE_ASSIGN_CONTACT:
                if (cartId <= 0) {
                    throw new IllegalArgumentException("Cart ID must be valid for assign contact mode");
                }
                break;

            case MODE_ASSIGN_CART:
                if (contactId <= 0 || cartId <= 0) {
                    throw new IllegalArgumentException("Contact ID and Cart ID must be valid for assign cart mode");
                }
                break;

            case MODE_CREATE:
                // No specific validation needed for create mode
                break;

            default:
                Log.w(TAG, "Unknown mode: " + mode + ", treating as CREATE");
                break;
        }
    }

    /**
     * Check if in create mode
     */
    public boolean isCreateMode() {
        return MODE_CREATE.equals(mode);
    }

    /**
     * ✅ Corrigé: Check if in assign cart mode
     */
    public boolean isAssignCartMode() {
        return MODE_ASSIGN_CART.equals(mode);
    }

    /**
     * ✅ Nouvelle méthode: Check if in assign contact mode
     */
    public boolean isAssignContactMode() {
        return MODE_ASSIGN_CONTACT.equals(mode);
    }

    /**
     * Get current user ID
     */
    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }

    /**
     * Get cart ID
     */
    public int getCartId() {
        return cartId;
    }

    /**
     * Get contact ID
     */
    public int getContactId() {
        return contactId;
    }

    /**
     * Get current mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * Load contact details with improved error handling
     */
    public void loadContactDetails() {
        if (contactId <= 0) {
            Log.w(TAG, "Invalid contactId for loading details: " + contactId);
            return;
        }

        Log.d(TAG, "Loading contact details for ID: " + contactId);
        isLoading.setValue(true);

        // First try to get from local cache
        try {
            Contact cachedContact = repository.getContactById(contactId);
            if (cachedContact != null) {
                Log.d(TAG, "Contact found in cache: " + cachedContact.toString());
                contact.setValue(cachedContact);
                isLoading.setValue(false);
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error accessing local cache", e);
            // Continue with API call
        }

        // Then get fresh data from API
        apiService.getContactDetails(contactId).enqueue(new Callback<Contact>() {
            @Override
            public void onResponse(Call<Contact> call, Response<Contact> response) {
                isLoading.setValue(false);
                Log.d(TAG, "API response received for contact details - Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Contact remoteContact = response.body();
                    Log.d(TAG, "Contact loaded from API: " + remoteContact.toString());

                    // Update LiveData
                    contact.setValue(remoteContact);

                    // Cache contact locally
                    try {
                        repository.insertOrUpdateContact(remoteContact);
                    } catch (Exception e) {
                        Log.e(TAG, "Error caching contact locally", e);
                        // Don't show error to user, it's not critical
                    }
                } else {
                    handleApiError("loading contact details", response);
                }
            }

            @Override
            public void onFailure(Call<Contact> call, Throwable t) {
                isLoading.setValue(false);
                handleNetworkError("loading contact details", t);
            }
        });
    }

    /**
     * ✅ Amélioration: Save contact with improved logic
     */
    public void saveContact(Contact contact) {
        if (contact == null) {
            Log.e(TAG, "Cannot save null contact");
            errorMessage.setValue("Contact invalide");
            return;
        }

        Log.d(TAG, "Saving contact: " + contact.toString());

        // If we're in assign cart mode, we don't want to save contact changes
        // since it's just for assignment purposes
        if (isAssignCartMode()) {
            Log.d(TAG, "In assign cart mode, skipping save and marking as success");
            operationSuccess.setValue(true);
            return;
        }

        // Check network connectivity
        if (NetworkUtils.isNetworkAvailable(getApplication())) {
            Log.d(TAG, "Network available, saving online");
            // Online - save to server
            isLoading.setValue(true);

            if (isCreateMode() || isAssignContactMode()) {
                createContact(contact);
            } else {
                updateContact(contact);
            }
        } else {
            Log.d(TAG, "Network unavailable, saving locally");
            // Offline - save locally
            saveContactLocally(contact);
        }
    }

    /**
     * ✅ Amélioration: Assign cart to contact with better validation
     */
    public void assignCartToContact() {
        Log.d(TAG, "assignCartToContact - contactId: " + contactId + ", cartId: " + cartId);

        if (contactId <= 0 || cartId <= 0) {
            String error = "ID de contact ou de panier invalide (contactId: " + contactId + ", cartId: " + cartId + ")";
            Log.e(TAG, error);
            errorMessage.setValue(error);
            return;
        }

        if (currentUser == null) {
            Log.e(TAG, "Current user is null");
            errorMessage.setValue("Utilisateur non connecté");
            return;
        }

        isLoading.setValue(true);

        // Préparer les données pour l'appel API
        Map<String, Object> assignData = new HashMap<>();
        assignData.put("cart_id", cartId);
        assignData.put("contact_id", contactId);
        assignData.put("user_id", currentUser.getId());

        Log.d(TAG, "Assignment data: " + assignData.toString());

        // Appel à l'API
        apiService.assignContactToCart(assignData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoading.setValue(false);
                Log.d(TAG, "Assignment API response - Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        boolean success = (boolean) response.body().get("success");
                        Log.d(TAG, "Assignment success: " + success);

                        if (success) {
                            assignmentSuccess.setValue(true);
                        } else {
                            String message = response.body().containsKey("message") ?
                                    (String) response.body().get("message") :
                                    "Erreur lors de l'assignation";
                            Log.e(TAG, "Assignment failed: " + message);
                            errorMessage.setValue(message);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing assignment response", e);
                        errorMessage.setValue("Erreur lors du traitement de la réponse");
                    }
                } else {
                    handleApiError("assignment", response);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoading.setValue(false);
                handleNetworkError("assignment", t);
            }
        });
    }

    /**
     * ✅ Amélioration: Create a new contact on the server with better error handling
     */
    private void createContact(Contact contact) {
        Log.d(TAG, "Creating contact on server");

        apiService.createContact(contact).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoading.setValue(false);
                Log.d(TAG, "Create contact API response - Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        boolean success = (boolean) response.body().get("success");
                        Log.d(TAG, "Create contact success: " + success);

                        if (success) {
                            // Get created contact data
                            @SuppressWarnings("unchecked")
                            Map<String, Object> contactData = (Map<String, Object>) response.body().get("contact");
                            if (contactData != null) {
                                int newId = ((Number) contactData.get("id")).intValue();
                                Log.d(TAG, "New contact ID: " + newId);

                                // Update contact with server ID
                                contact.setId(newId);

                                // Update the LiveData
                                ContactFormViewModel.this.contact.setValue(contact);

                                // Cache contact locally
                                try {
                                    repository.insertOrUpdateContact(contact);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error caching created contact", e);
                                }
                            }

                            // Set success
                            operationSuccess.setValue(true);
                        } else {
                            String message = response.body().containsKey("message") ?
                                    (String) response.body().get("message") : "Erreur lors de la création";
                            Log.e(TAG, "Create contact failed: " + message);
                            errorMessage.setValue(message);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing create response", e);
                        errorMessage.setValue("Erreur lors du traitement de la réponse");
                    }
                } else {
                    handleApiError("contact creation", response);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoading.setValue(false);
                handleNetworkError("contact creation", t);
            }
        });
    }

    /**
     * ✅ Amélioration: Update an existing contact on the server
     */
    private void updateContact(Contact contact) {
        Log.d(TAG, "Updating contact on server");

        apiService.updateContact(contact).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoading.setValue(false);
                Log.d(TAG, "Update contact API response - Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        boolean success = (boolean) response.body().get("success");
                        Log.d(TAG, "Update contact success: " + success);

                        if (success) {
                            // Cache contact locally
                            try {
                                repository.insertOrUpdateContact(contact);
                            } catch (Exception e) {
                                Log.e(TAG, "Error caching updated contact", e);
                            }

                            // Set success
                            operationSuccess.setValue(true);
                        } else {
                            String message = response.body().containsKey("message") ?
                                    (String) response.body().get("message") : "Erreur lors de la mise à jour";
                            Log.e(TAG, "Update contact failed: " + message);
                            errorMessage.setValue(message);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing update response", e);
                        errorMessage.setValue("Erreur lors du traitement de la réponse");
                    }
                } else {
                    handleApiError("contact update", response);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoading.setValue(false);
                handleNetworkError("contact update", t);
            }
        });
    }

    /**
     * ✅ Amélioration: Delete a contact with better error handling
     */
    public void deleteContact() {
        if (contact.getValue() == null) {
            Log.e(TAG, "Cannot delete null contact");
            errorMessage.setValue("Aucun contact à supprimer");
            return;
        }

        Log.d(TAG, "Deleting contact: " + contact.getValue().getId());
        isLoading.setValue(true);

        apiService.deleteContact(contact.getValue().getId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoading.setValue(false);
                Log.d(TAG, "Delete contact API response - Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        boolean success = (boolean) response.body().get("success");
                        Log.d(TAG, "Delete contact success: " + success);

                        if (success) {
                            // Remove from local cache
                            try {
                                repository.deleteContact(contact.getValue().getId());
                            } catch (Exception e) {
                                Log.e(TAG, "Error removing contact from cache", e);
                            }

                            // Set success
                            operationSuccess.setValue(true);
                        } else {
                            String message = response.body().containsKey("message") ?
                                    (String) response.body().get("message") : "Erreur lors de la suppression";
                            Log.e(TAG, "Delete contact failed: " + message);
                            errorMessage.setValue(message);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing delete response", e);
                        errorMessage.setValue("Erreur lors du traitement de la réponse");
                    }
                } else {
                    handleApiError("contact deletion", response);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoading.setValue(false);
                handleNetworkError("contact deletion", t);
            }
        });
    }

    /**
     * ✅ Amélioration: Save contact locally for later synchronization
     */
    public void saveContactLocally(Contact contact) {
        Log.d(TAG, "Saving contact locally: " + contact.toString());

        try {
            // Mark contact as dirty for future synchronization
            contact.setDirty(true);

            // Timestamp for tracking last modification
            contact.setLastUpdated(System.currentTimeMillis());

            // For new contacts, generate a temporary negative ID
            if (isCreateMode() || isAssignContactMode()) {
                // Find the lowest local ID to avoid conflicts
                int tempId = repository.getLowestLocalId() - 1;
                contact.setId(tempId);
                Log.d(TAG, "Assigned temporary ID: " + tempId);
            }

            // Save to local repository
            repository.insertOrUpdateContact(contact);

            // Update LiveData
            this.contact.setValue(contact);

            // Register this contact for future synchronization
            SyncManager.getInstance(getApplication()).addContactForSync(contact.getId());

            // Mark operation as successful
            operationSuccess.setValue(true);

            Log.d(TAG, "Contact saved locally successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error saving contact locally", e);
            errorMessage.setValue("Erreur lors de la sauvegarde locale: " + e.getMessage());
        }
    }

    /**
     * ✅ Amélioration: Handle API error responses with detailed logging
     */
    private <T> void handleApiError(String operation, Response<T> response) {
        String error = "Erreur " + operation + ": ";

        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                error += errorBody;
                Log.e(TAG, "API Error for " + operation + " - Code: " + response.code() + ", Body: " + errorBody);
            } else {
                error += "Code " + response.code();
                Log.e(TAG, "API Error for " + operation + " - Code: " + response.code() + ", No error body");
            }
        } catch (Exception e) {
            error += "Code " + response.code();
            Log.e(TAG, "Error parsing error body for " + operation, e);
        }

        errorMessage.setValue(error);
    }

    /**
     * ✅ Nouvelle méthode: Handle network errors
     */
    private void handleNetworkError(String operation, Throwable error) {
        String message;

        if (error instanceof java.net.SocketTimeoutException) {
            message = "Délai d'attente dépassé pour " + operation;
        } else if (error instanceof java.net.UnknownHostException) {
            message = "Serveur introuvable pour " + operation;
        } else if (error instanceof java.io.IOException) {
            message = "Erreur de connexion pour " + operation;
        } else {
            message = "Erreur réseau pour " + operation + ": " + error.getMessage();
        }

        Log.e(TAG, "Network error for " + operation, error);
        errorMessage.setValue(message);
    }

    // ✅ Getters for LiveData (unchanged but with better documentation)

    /**
     * Get loading state LiveData
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Get contact data LiveData
     */
    public LiveData<Contact> getContact() {
        return contact;
    }

    /**
     * Get error message LiveData
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Get operation success LiveData
     */
    public LiveData<Boolean> getOperationSuccess() {
        return operationSuccess;
    }

    /**
     * Get assignment success LiveData
     */
    public LiveData<Boolean> getAssignmentSuccess() {
        return assignmentSuccess;
    }

    /**
     * ✅ Nouvelle méthode: Reset error messages
     */
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    /**
     * ✅ Nouvelle méthode: Reset success states
     */
    public void resetSuccessStates() {
        operationSuccess.setValue(false);
        assignmentSuccess.setValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "ViewModel cleared");
    }
}