package com.drogpulseai.viewmodels;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Product;
import com.drogpulseai.models.User;
import com.drogpulseai.repository.ProductRepository;
import com.drogpulseai.utils.FileUtils;
import com.drogpulseai.utils.NetworkResult;
import com.drogpulseai.utils.SessionManager;
import com.drogpulseai.sync.SyncManager;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel for ProductFormActivity
 * Handles data operations and business logic
 */
public class ProductFormViewModel extends AndroidViewModel {

    private static final String TAG = "ProductFormViewModel";

    // Mode constants
    private static final String MODE_CREATE = "create";
    private static final String MODE_EDIT = "edit";

    // Repository
    private final ProductRepository repository;

    // API client
    private final ApiService apiService;

    // Session manager
    private final SessionManager sessionManager;

    // LiveData
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Product> product = new MutableLiveData<>();
    private final MutableLiveData<String> photoUrl = new MutableLiveData<>("");
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();

    // State variables
    private String mode = MODE_CREATE;
    private int productId = -1;
    private User currentUser;

    public ProductFormViewModel(@NonNull Application application) {
        super(application);

        // Initialize repository
        repository = new ProductRepository(application);

        // Initialize API client
        apiService = ApiClient.getApiService();

        // Initialize session manager
        sessionManager = new SessionManager(application);

        // Get current user
        currentUser = sessionManager.getUser();
    }

    /**
     * Initialize ViewModel with mode and product ID
     */
    public void initializeMode(String mode, int productId) {
        this.mode = mode;
        this.productId = productId;
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
     * Load product details from repository
     */
    public void loadProductDetails() {
        if (productId <= 0) {
            return;
        }

        isLoading.setValue(true);

        // First try to get from local cache
        Product cachedProduct = repository.getProductById(productId);
        if (cachedProduct != null) {
            product.setValue(cachedProduct);
            photoUrl.setValue(cachedProduct.getPhotoUrl());
            isLoading.setValue(false);
        }

        // Then get fresh data from API
        apiService.getProductDetails(productId).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    Product remoteProduct = response.body();

                    // Update LiveData
                    product.setValue(remoteProduct);
                    photoUrl.setValue(remoteProduct.getPhotoUrl());

                    // Cache product locally
                    repository.insertOrUpdateProduct(remoteProduct);
                } else {
                    handleApiError(response);
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Erreur réseau: " + t.getMessage());
                Log.e(TAG, "Network error: ", t);
            }
        });
    }

    /**
     * Save product (create or update)
     */
    public void saveProduct(Product product) {
        isLoading.setValue(true);

        if (isCreateMode()) {
            createProduct(product);
        } else {
            updateProduct(product);
        }
    }

    /**
     * Create a new product
     */
    private void createProduct(Product product) {
        apiService.createProduct(product).enqueue(new Callback<NetworkResult<Product>>() {
            @Override
            public void onResponse(Call<NetworkResult<Product>> call, Response<NetworkResult<Product>> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Cache product locally
                    Product createdProduct = response.body().getData();
                    repository.insertOrUpdateProduct(createdProduct);

                    // Set success
                    operationSuccess.setValue(true);
                } else {
                    handleApiError(response);
                }
            }

            @Override
            public void onFailure(Call<NetworkResult<Product>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Erreur réseau: " + t.getMessage());
                Log.e(TAG, "Network error: ", t);
            }
        });
    }

    /**
     * Update an existing product
     */
    private void updateProduct(Product product) {
        apiService.updateProduct(product).enqueue(new Callback<NetworkResult<Product>>() {
            @Override
            public void onResponse(Call<NetworkResult<Product>> call, Response<NetworkResult<Product>> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Cache product locally
                    Product updatedProduct = response.body().getData();
                    repository.insertOrUpdateProduct(updatedProduct);

                    // Set success
                    operationSuccess.setValue(true);
                } else {
                    handleApiError(response);
                }
            }

            @Override
            public void onFailure(Call<NetworkResult<Product>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Erreur réseau: " + t.getMessage());
                Log.e(TAG, "Network error: ", t);
            }
        });
    }

    /**
     * Delete a product
     */
    public void deleteProduct() {
        if (product.getValue() == null) {
            return;
        }

        isLoading.setValue(true);

        apiService.deleteProduct(product.getValue().getId()).enqueue(new Callback<NetworkResult<Void>>() {
            @Override
            public void onResponse(Call<NetworkResult<Void>> call, Response<NetworkResult<Void>> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Remove from local cache
                    repository.deleteProduct(product.getValue().getId());

                    // Set success
                    operationSuccess.setValue(true);
                } else {
                    handleApiError(response);
                }
            }

            @Override
            public void onFailure(Call<NetworkResult<Void>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Erreur réseau: " + t.getMessage());
                Log.e(TAG, "Network error: ", t);
            }
        });
    }

    /**
     * Upload product photo
     */
    public void uploadProductPhoto(Uri imageUri, Context context) {
        if (imageUri == null) {
            return;
        }

        isLoading.setValue(true);

        // Process the image (compress and resize)
        File processedImage = FileUtils.compressAndResizeImage(context, imageUri, 1024, 1024);

        if (processedImage == null) {
            isLoading.setValue(false);
            errorMessage.setValue("Erreur lors du traitement de l'image");
            return;
        }

        // Create multipart request
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), processedImage);
        MultipartBody.Part photoPart = MultipartBody.Part.createFormData("photo", processedImage.getName(), requestFile);
        RequestBody userId = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(currentUser.getId()));

        apiService.uploadProductPhoto(photoPart, userId).enqueue(new Callback<NetworkResult<String>>() {
            @Override
            public void onResponse(Call<NetworkResult<String>> call, Response<NetworkResult<String>> response) {
                isLoading.setValue(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String url = response.body().getData();
                    photoUrl.setValue(url);
                } else {
                    handleApiError(response);
                }

                // Clean up temporary file
                if (processedImage.exists()) {
                    processedImage.delete();
                }
            }

            @Override
            public void onFailure(Call<NetworkResult<String>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Erreur réseau: " + t.getMessage());
                Log.e(TAG, "Network error: ", t);

                // Clean up temporary file
                if (processedImage.exists()) {
                    processedImage.delete();
                }
            }
        });
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

    public LiveData<Product> getProduct() {
        return product;
    }

    public LiveData<String> getPhotoUrl() {
        return photoUrl;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getOperationSuccess() {
        return operationSuccess;
    }


    /**
     * Sauvegarde un produit localement
     * @param product Le produit à sauvegarder
     */
    public void saveProductLocally(Product product) {
        // Marquer le produit comme "dirty" pour la synchronisation future
        product.setDirty(true);

        // Timestamp pour suivi de dernière modification
        product.setLastUpdated(System.currentTimeMillis());

        // Sauvegarder en local via le repository
        repository.insertOrUpdateProduct(product);

        // Si c'est une création, on génère un ID temporaire négatif
        // (les IDs du serveur sont toujours positifs)
        if (isCreateMode()) {
            // Trouver l'ID local le plus bas pour éviter les conflits
            int tempId = repository.getLowestLocalId() - 1;
            product.setId(tempId);
            repository.insertOrUpdateProduct(product);
        }

        // Si c'est une édition, on met à jour le produit dans le cache
        if (!isCreateMode()) {
            repository.insertOrUpdateProduct(product);
        }

        // Enregistrer ce produit pour synchronisation future
        SyncManager.getInstance(getApplication()).addProductForSync(product.getId());

        // Marquer l'opération comme réussie
        operationSuccess.setValue(true);
    }

    /**
     * Synchroniser tous les produits en attente
     */
    public void syncPendingProducts() {
        SyncManager.getInstance(getApplication()).scheduleSyncNow();
    }

    /**
     * Vérifier s'il y a des produits en attente de synchronisation
     */
    public boolean hasPendingProducts() {
        return SyncManager.getInstance(getApplication()).hasPendingProducts();
    }

    /**
     * Obtenir le nombre de produits en attente de synchronisation
     */
    public int getPendingProductCount() {
        return SyncManager.getInstance(getApplication()).getPendingCount();
    }
}