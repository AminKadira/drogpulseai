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

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel pour ContactFormActivity
 * Gère les opérations de données et la logique métier
 */
public class ContactFormViewModel extends AndroidViewModel {

    private static final String TAG = "ContactFormViewModel";

    // Constants
    private static final String MODE_CREATE = "create";
    private static final String MODE_EDIT = "edit";

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

    // State variables
    private String mode = MODE_CREATE;
    private int contactId = -1;
    private User currentUser;

    public ContactFormViewModel(@NonNull Application application) {
        super(application);

        // Initialize repository
        repository = new ContactRepository(application);

        // Initialize API client
        apiService = ApiClient.getApiService();

        // Initialize session manager
        sessionManager = new SessionManager(application);

        // Get current user
        currentUser = sessionManager.getUser();
    }

    /**
     * Initialize ViewModel with mode and contact ID
     */
    public void initializeMode(String mode, int contactId) {
        this.mode = mode;
        this.contactId = contactId;
    }

    /**
     * Check if in create mode
     */
    public boolean isCreateMode() {
        return MODE_CREATE.equals(mode);
    }

    /**
     * Get current user ID
     */
    public int getCurrentUserId() {
        return currentUser.getId();
    }

    /**
     * Load contact details
     */
    public void loadContactDetails() {
        if (contactId <= 0) {
            return;
        }

        isLoading.setValue(true);

        // First try to get from local cache
        Contact cachedContact = repository.getContactById(contactId);
        if (cachedContact != null) {
            contact.setValue(cachedContact);
            isLoading.setValue(false);
            return;
        }

        // Then get fresh data from API
        apiService.getContactDetails(contactId).enqueue(new Callback<Contact>() {
            @Override
            public void onResponse(Call<Contact> call, Response<Contact> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    Contact remoteContact = response.body();

                    // Update LiveData
                    contact.setValue(remoteContact);

                    // Cache contact locally
                    repository.insertOrUpdateContact(remoteContact);
                } else {
                    handleApiError(response);
                }
            }

            @Override
            public void onFailure(Call<Contact> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Erreur réseau: " + t.getMessage());
                Log.e(TAG, "Network error: ", t);
            }
        });
    }

    /**
     * Save contact (create or update)
     */
    public void saveContact(Contact contact) {
        // Check network connectivity
        if (NetworkUtils.isNetworkAvailable(getApplication())) {
            // Online - save to server
            isLoading.setValue(true);

            if (isCreateMode()) {
                createContact(contact);
            } else {
                updateContact(contact);
            }
        } else {
            // Offline - save locally
            saveContactLocally(contact);
        }
    }

    /**
     * Create a new contact on the server
     */
    private void createContact(Contact contact) {
        apiService.createContact(contact).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null && (boolean) response.body().get("success")) {
                    // Get created contact data
                    Map<String, Object> contactData = (Map<String, Object>) response.body().get("contact");
                    int newId = ((Double) contactData.get("id")).intValue();

                    // Update contact with server ID
                    contact.setId(newId);

                    // Cache contact locally
                    repository.insertOrUpdateContact(contact);

                    // Set success
                    operationSuccess.setValue(true);
                } else {
                    handleApiError(response);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Erreur réseau: " + t.getMessage());
                Log.e(TAG, "Network error: ", t);
            }
        });
    }

    /**
     * Update an existing contact on the server
     */
    private void updateContact(Contact contact) {
        apiService.updateContact(contact).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null && (boolean) response.body().get("success")) {
                    // Cache contact locally
                    repository.insertOrUpdateContact(contact);

                    // Set success
                    operationSuccess.setValue(true);
                } else {
                    handleApiError(response);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Erreur réseau: " + t.getMessage());
                Log.e(TAG, "Network error: ", t);
            }
        });
    }

    /**
     * Delete a contact
     */
    public void deleteContact() {
        if (contact.getValue() == null) {
            return;
        }

        isLoading.setValue(true);

        apiService.deleteContact(contact.getValue().getId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null && (boolean) response.body().get("success")) {
                    // Remove from local cache
                    repository.deleteContact(contact.getValue().getId());

                    // Set success
                    operationSuccess.setValue(true);
                } else {
                    handleApiError(response);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Erreur réseau: " + t.getMessage());
                Log.e(TAG, "Network error: ", t);
            }
        });
    }

    /**
     * Save contact locally for later synchronization
     */
    public void saveContactLocally(Contact contact) {
        // Mark contact as dirty for future synchronization
        contact.setDirty(true);

        // Timestamp for tracking last modification
        contact.setLastUpdated(System.currentTimeMillis());

        // For new contacts, generate a temporary negative ID
        if (isCreateMode()) {
            // Find the lowest local ID to avoid conflicts
            int tempId = repository.getLowestLocalId() - 1;
            contact.setId(tempId);
        }

        // Save to local repository
        repository.insertOrUpdateContact(contact);

        // Register this contact for future synchronization
        SyncManager.getInstance(getApplication()).addContactForSync(contact.getId());

        // Mark operation as successful
        operationSuccess.setValue(true);
    }

    /**
     * Handle API error responses
     */
    private <T> void handleApiError(Response<T> response) {
        String error = "Erreur serveur: ";

        try {
            if (response.errorBody() != null) {
                error += response.errorBody().string();
            } else {
                error += response.code();
            }
        } catch (Exception e) {
            error += response.code();
            Log.e(TAG, "Error parsing error body", e);
        }

        errorMessage.setValue(error);
        Log.e(TAG, error);
    }

    // Getters for LiveData
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Contact> getContact() {
        return contact;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getOperationSuccess() {
        return operationSuccess;
    }
}