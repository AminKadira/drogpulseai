package com.drogpulseai.activities.products;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.drogpulseai.R;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Product;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.FileUtils;
import com.drogpulseai.utils.SessionManager;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;

public class ProductFormActivity extends AppCompatActivity {

    // Mode de l'activité
    private static final String MODE_CREATE = "create";
    private static final String MODE_EDIT = "edit";
    private static final int PICK_IMAGE_REQUEST = 1;

    // UI Components
    private EditText etReference, etLabel, etName, etDescription, etBarcode, etQuantity;
    private Button btnSave, btnDelete, btnAddPhoto;
    private ImageView ivProductPhoto;
    private ProgressBar progressBar;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;

    // Données
    private String mode;
    private int productId;
    private User currentUser;
    private Product currentProduct;
    private String photoUrl = "";
    private Uri selectedImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_form);

        // Récupérer le mode et les données
        mode = getIntent().getStringExtra("mode");
        productId = getIntent().getIntExtra("product_id", -1);

        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(MODE_CREATE.equals(mode) ?
                    R.string.create_product : R.string.edit_product);
        }

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);

        // Récupérer l'utilisateur courant
        currentUser = sessionManager.getUser();

        // Initialisation des vues
        initializeViews();

        // Configuration des listeners
        setupListeners();

        // Configuration initiale selon le mode
        if (MODE_CREATE.equals(mode)) {
            setupForCreateMode();
        } else if (MODE_EDIT.equals(mode) && productId != -1) {
            loadProductDetails(productId);
        } else {
            Toast.makeText(this, "Mode invalide", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Initialisation des vues
     */
    private void initializeViews() {
        etReference = findViewById(R.id.et_reference);
        etLabel = findViewById(R.id.et_label);
        etName = findViewById(R.id.et_name);
        etDescription = findViewById(R.id.et_description);
        etBarcode = findViewById(R.id.et_barcode);
        etQuantity = findViewById(R.id.et_quantity);
        ivProductPhoto = findViewById(R.id.iv_product_photo);
        btnAddPhoto = findViewById(R.id.btn_add_photo);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        progressBar = findViewById(R.id.progress_bar);
    }

    /**
     * Configuration des écouteurs d'événements
     */
    private void setupListeners() {
        // Bouton d'ajout de photo
        btnAddPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // Bouton de sauvegarde
        btnSave.setOnClickListener(v -> saveProduct());

        // Bouton de suppression
        btnDelete.setOnClickListener(v -> confirmDeleteProduct());
    }

    /**
     * Configuration pour le mode création
     */
    private void setupForCreateMode() {
        btnDelete.setVisibility(View.GONE);
        etQuantity.setText("0"); // Valeur par défaut
    }

    /**
     * Gestion du résultat de la sélection d'image
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            // Afficher l'image sélectionnée
            Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(ivProductPhoto);

            // Masquer le bouton une fois la photo chargée
            btnAddPhoto.setText(R.string.change_photo);

            // Uploader directement la photo
            uploadProductPhoto();
        }
    }

    /**
     * Upload de la photo du produit
     */
    private void uploadProductPhoto() {
        if (selectedImageUri == null) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Obtenir le fichier réel
        File file = FileUtils.getFileFromUri(this, selectedImageUri);

        if (file == null) {
            Toast.makeText(this, "Impossible de traiter l'image", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Créer les RequestBody
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part photoPart = MultipartBody.Part.createFormData("photo", file.getName(), requestFile);
        RequestBody userId = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(currentUser.getId()));

        // Appel API
        apiService.uploadProductPhoto(photoPart, userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    boolean success = (boolean) result.get("success");

                    if (success && result.containsKey("photo_url")) {
                        photoUrl = (String) result.get("photo_url");
                        Toast.makeText(ProductFormActivity.this, "Photo téléchargée", Toast.LENGTH_SHORT).show();
                    } else {
                        String message = (String) result.get("message");
                        Toast.makeText(ProductFormActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(ProductFormActivity.this, "Erreur lors du téléchargement", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ProductFormActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Chargement des détails d'un produit existant
     */
    private void loadProductDetails(int productId) {
        progressBar.setVisibility(View.VISIBLE);

        apiService.getProductDetails(productId).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    currentProduct = response.body();

                    // Remplir les champs avec les données du produit
                    etReference.setText(currentProduct.getReference());
                    etLabel.setText(currentProduct.getLabel());
                    etName.setText(currentProduct.getName());
                    etDescription.setText(currentProduct.getDescription());
                    etBarcode.setText(currentProduct.getBarcode());
                    etQuantity.setText(String.valueOf(currentProduct.getQuantity()));

                    // Charger la photo si disponible
                    if (currentProduct.getPhotoUrl() != null && !currentProduct.getPhotoUrl().isEmpty()) {
                        photoUrl = currentProduct.getPhotoUrl();
                        Glide.with(ProductFormActivity.this)
                                .load(ApiClient.getBaseUrl() + photoUrl)
                                .centerCrop()
                                .into(ivProductPhoto);
                        btnAddPhoto.setText(R.string.change_photo);
                    }
                } else {
                    Toast.makeText(ProductFormActivity.this,
                            "Erreur lors du chargement des détails du produit",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ProductFormActivity.this,
                        "Erreur réseau : " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    /**
     * Sauvegarde du produit (création ou mise à jour)
     */
    private void saveProduct() {
        // Récupération des données saisies
        String reference = etReference.getText().toString().trim();
        String label = etLabel.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String barcode = etBarcode.getText().toString().trim();
        String quantityStr = etQuantity.getText().toString().trim();

        // Validation des champs requis
        if (reference.isEmpty()) {
            etReference.setError("Veuillez saisir la référence");
            etReference.requestFocus();
            return;
        }

        if (label.isEmpty()) {
            etLabel.setError("Veuillez saisir le libellé");
            etLabel.requestFocus();
            return;
        }

        if (name.isEmpty()) {
            etName.setError("Veuillez saisir le nom");
            etName.requestFocus();
            return;
        }

        // Convertir la quantité
        int quantity = 0;
        if (!quantityStr.isEmpty()) {
            try {
                quantity = Integer.parseInt(quantityStr);
            } catch (NumberFormatException e) {
                etQuantity.setError("Quantité invalide");
                etQuantity.requestFocus();
                return;
            }
        }

        // Afficher la progression
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        if (MODE_CREATE.equals(mode)) {

            // Création d'un nouveau produit
            Product newProduct = new Product(reference, label, name, description, photoUrl, barcode, quantity, currentUser.getId());

            apiService.createProduct(newProduct).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    // Masquer l'indicateur de progression
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> result = response.body();

                        boolean success = (boolean) result.get("success");

                        if (success) {
                            // Récupérer le produit créé dans la réponse
                            Map<String, Object> productData = (Map<String, Object>) result.get("product");

                            // Afficher un message de succès
                            Toast.makeText(ProductFormActivity.this, "Produit créé avec succès", Toast.LENGTH_SHORT).show();

                            // Vous pouvez ici retourner à l'activité précédente ou effectuer d'autres actions
                            finish();
                        } else {
                            // En cas d'erreur côté serveur
                            String message = (String) result.get("message");
                            Toast.makeText(ProductFormActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // En cas d'erreur HTTP
                        try {
                            // Essayer de parser l'erreur du serveur
                            if (response.errorBody() != null) {
                                String errorJson = response.errorBody().string();
                                JSONObject errorObject = new JSONObject(errorJson);
                                String errorMessage = errorObject.getString("message");
                                Toast.makeText(ProductFormActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ProductFormActivity.this,
                                        "Erreur " + response.code() + ": " + response.message(),
                                        Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(ProductFormActivity.this,
                                    "Erreur lors de la création du produit (" + response.code() + ")",
                                    Toast.LENGTH_LONG).show();

                            // Log l'erreur pour débogage
                            Log.e("API", "Erreur: " + e.getMessage());
                        }
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    // Masquer l'indicateur de progression
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);

                    // Log détaillé de l'erreur
                    Log.e("API", "URL: " + call.request().url());
                    Log.e("API", "Méthode: " + call.request().method());
                    Log.e("API", "Erreur: " + t.getMessage());
                    Log.e("API", "Classe d'erreur: " + t.getClass().getName());

                    // Afficher des informations utiles en fonction du type d'erreur
                    if (t instanceof IOException) {
                        // Problème de réseau
                        Toast.makeText(ProductFormActivity.this,
                                "Erreur de connexion réseau. Vérifiez votre connexion Internet.",
                                Toast.LENGTH_LONG).show();
                    } else if (t instanceof HttpException) {
                        // Erreur HTTP
                        HttpException httpError = (HttpException) t;
                        Response<?> response = httpError.response();

                        try {
                            String errorBody = response.errorBody().string();
                            Log.e("API", "Corps de l'erreur: " + errorBody);
                            Toast.makeText(ProductFormActivity.this,
                                    "Erreur serveur: " + response.code() + " - " + errorBody,
                                    Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            Toast.makeText(ProductFormActivity.this,
                                    "Erreur serveur: " + response.code(),
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // Autre type d'erreur
                        Toast.makeText(ProductFormActivity.this,
                                "Erreur: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            // Mise à jour du produit existant
            currentProduct.setReference(reference);
            currentProduct.setLabel(label);
            currentProduct.setName(name);
            currentProduct.setDescription(description);
            currentProduct.setPhotoUrl(photoUrl);
            currentProduct.setBarcode(barcode);
            currentProduct.setQuantity(quantity);

            apiService.updateProduct(currentProduct).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> result = response.body();
                        boolean success = (boolean) result.get("success");

                        if (success) {
                            Toast.makeText(ProductFormActivity.this, "Produit mis à jour avec succès", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String message = (String) result.get("message");
                            Toast.makeText(ProductFormActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(ProductFormActivity.this, "Erreur lors de la mise à jour du produit", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                        Log.e("API", "Erreur complète: " + t.toString());
                        Log.e("API", "Cause: " + (t.getCause() != null ? t.getCause().toString() : "Inconnue"));
                        Log.e("API", "Message: " + t.getMessage());
                        Log.e("API", "URL: " + call.request().url());

                        // Enregistrer la requête pour débogage
                        Request request = call.request();
                        try {
                            Log.e("API", "Headers: " + request.headers().toString());
                            if (request.body() != null) {
                                Log.e("API", "Body class: " + request.body().getClass().getName());
                            }
                        } catch (Exception e) {
                            Log.e("API", "Erreur lors de l'analyse de la requête: " + e.getMessage());
                        }

                        Toast.makeText(ProductFormActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();

                }
            });
        }
    }

    /**
     * Confirmation avant la suppression d'un produit
     */
    private void confirmDeleteProduct() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Supprimer le produit");
        builder.setMessage("Êtes-vous sûr de vouloir supprimer ce produit ?");
        builder.setPositiveButton("Oui", (dialog, which) -> deleteProduct());
        builder.setNegativeButton("Non", null);
        builder.show();
    }

    /**
     * Suppression du produit
     */
    private void deleteProduct() {
        progressBar.setVisibility(View.VISIBLE);
        btnDelete.setEnabled(false);

        apiService.deleteProduct(currentProduct.getId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                btnDelete.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    boolean success = (boolean) result.get("success");

                    if (success) {
                        Toast.makeText(ProductFormActivity.this, "Produit supprimé avec succès", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String message = (String) result.get("message");
                        Toast.makeText(ProductFormActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(ProductFormActivity.this, "Erreur lors de la suppression du produit", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnDelete.setEnabled(true);
                Toast.makeText(ProductFormActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}