package com.drogpulseai.activities.products;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.drogpulseai.R;
import com.drogpulseai.models.Product;
import com.drogpulseai.utils.DialogUtils;
import com.drogpulseai.utils.ImageHelper;
import com.drogpulseai.utils.BarcodeHelper;
import com.drogpulseai.utils.FormValidator;
import com.drogpulseai.utils.ValidationMap;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.viewmodels.ProductFormViewModel;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

/**
 * Activity for creating and editing products
 * Optimized version with ViewModel architecture and improved separation of concerns
 */
public class ProductFormActivity extends AppCompatActivity implements
        ImageHelper.ImageSelectionCallback,
        BarcodeHelper.BarcodeCallback {

    private static final String TAG = "ProductFormActivity";

    // Views
    private ImageView ivProductPhoto;
    private Button btnAddPhoto, btnSave, btnDelete;
    private ImageButton btnScanBarcode;
    private EditText etReference, etLabel, etName, etDescription, etBarcode, etQuantity, etPrice;
    private ProgressBar progressBar;

    // ViewModel
    private ProductFormViewModel viewModel;

    // Helper classes
    private ImageHelper imageHelper;
    private BarcodeHelper barcodeHelper;
    private FormValidator validator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_form);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ProductFormViewModel.class);

        // Initialize helper classes
        imageHelper = new ImageHelper(this, this);
        barcodeHelper = new BarcodeHelper(this, this);
        validator = new FormValidator(this);

        // Initialize views
        initializeViews();

        // Configure toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get intent extras
        String mode = getIntent().getStringExtra("mode");
        int productId = getIntent().getIntExtra("product_id", -1);

        // Initialize mode in ViewModel
        viewModel.initializeMode(mode, productId);

        // Set up UI based on mode
        setupUI();

        // Set up observers
        setupObservers();
    }

    /**
     * Initialize views
     */
    private void initializeViews() {
        ivProductPhoto = findViewById(R.id.iv_product_photo);
        btnAddPhoto = findViewById(R.id.btn_add_photo);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        btnScanBarcode = findViewById(R.id.btn_scan_barcode);
        etReference = findViewById(R.id.et_reference);
        etLabel = findViewById(R.id.et_label);
        etName = findViewById(R.id.et_name);
        etDescription = findViewById(R.id.et_description);
        etBarcode = findViewById(R.id.et_barcode);
        etQuantity = findViewById(R.id.et_quantity);
        etPrice = findViewById(R.id.et_price);
        progressBar = findViewById(R.id.progress_bar);
    }

    /**
     * Set up the UI components and listeners
     */
    private void setupUI() {
        // Set activity title based on mode
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(viewModel.isCreateMode() ?
                    R.string.create_product : R.string.edit_product);
        }

        // Hide delete button in create mode
        if (viewModel.isCreateMode()) {
            btnDelete.setVisibility(View.GONE);
        }

        // Set up button click listeners
        // btnAddPhoto.setOnClickListener(v -> imageHelper.showImageSourceDialog());
        // Modifier le listener du bouton dans la méthode setupUI()
        btnAddPhoto.setOnClickListener(v -> {
            // Vérifier la connexion internet avant de montrer le dialogue de sélection d'image
            if (NetworkUtils.isNetworkAvailable(this)) {
                // Connexion disponible, afficher le dialogue de sélection d'image
                imageHelper.showImageSourceDialog();
            } else {
                // Pas de connexion, afficher un message d'erreur
                Toast.makeText(this,
                        "Connexion internet requise pour ajouter une photo",
                        Toast.LENGTH_LONG).show();

                // Vous pourriez également afficher un dialogue plus détaillé
                new AlertDialog.Builder(this)
                        .setTitle("Connexion requise")
                        .setMessage("Une connexion internet est nécessaire pour télécharger des photos. " +
                                "Veuillez vous connecter à Internet et réessayer.")
                        .setPositiveButton("OK", null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
        btnScanBarcode.setOnClickListener(v -> barcodeHelper.scanBarcode());
        btnSave.setOnClickListener(v -> validateAndSaveProduct());
        btnDelete.setOnClickListener(v -> confirmDeleteProduct());

        // Load product details if in edit mode
        if (!viewModel.isCreateMode()) {
            viewModel.loadProductDetails();
        }
    }

    /**
     * Set up LiveData observers
     */
    private void setupObservers() {
        // Observe loading state
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            setFormEnabled(!isLoading);
        });

        // Observe product data
        viewModel.getProduct().observe(this, product -> {
            if (product != null) {
                populateForm(product);
            }
        });

        // Observe photo URL
        viewModel.getPhotoUrl().observe(this, url -> {
            if (url != null && !url.isEmpty()) {
                imageHelper.displayImage(url, ivProductPhoto);
                btnAddPhoto.setText(R.string.change_photo);
            }
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });

        // Observe success events
        viewModel.getOperationSuccess().observe(this, success -> {
            if (success) {
                finish();
            }
        });
    }

    /**
     * Populate form fields with product data
     */
    @SuppressLint("DefaultLocale")
    private void populateForm(Product product) {
        etReference.setText(product.getReference());
        etLabel.setText(product.getLabel());
        etName.setText(product.getName());
        etDescription.setText(product.getDescription());
        etBarcode.setText(product.getBarcode());
        etQuantity.setText(String.valueOf(product.getQuantity()));
        etPrice.setText(String.format("%.2f", product.getPrice()));
    }

    /**
     * Enable/disable all form fields
     */
    private void setFormEnabled(boolean enabled) {
        etReference.setEnabled(enabled);
        etLabel.setEnabled(enabled);
        etName.setEnabled(enabled);
        etDescription.setEnabled(enabled);
        etBarcode.setEnabled(enabled);
        etQuantity.setEnabled(enabled);
        etPrice.setEnabled(enabled);
        btnSave.setEnabled(enabled);
        btnDelete.setEnabled(enabled);
        btnAddPhoto.setEnabled(enabled);
        btnScanBarcode.setEnabled(enabled);
    }

    /**
     * Validate form and save product
     */
    private void validateAndSaveProduct() {
        // Créer la map de validation
        ValidationMap validationMap = new ValidationMap();
        validationMap.add(etReference, validator.required("Référence requise"));
        validationMap.add(etLabel, validator.required("Libellé requis"));
        validationMap.add(etName, validator.required("Nom requis"));
        validationMap.add(etQuantity, validator.integer("Quantité invalide"));
        validationMap.add(etPrice, validator.decimal("Prix invalide"));

        // Valider le formulaire
        if (!validator.validate(validationMap)) {
            return;
        }

        // Créer l'objet produit à partir des données du formulaire
        Product product = getProductFromForm();

        // Vérifier la connectivité réseau
        if (NetworkUtils.isNetworkAvailable(this)) {
            // Connexion internet disponible - sauvegarder via l'API
            viewModel.saveProduct(product);
        } else {
            // Pas de connexion internet - sauvegarder localement
            viewModel.saveProductLocally(product);

            // Informer l'utilisateur
            Toast.makeText(this, "Produit sauvegardé localement. Synchronisation automatique dès que la connexion sera rétablie.",
                    Toast.LENGTH_LONG).show();

            // Terminer l'activité
            finish();
        }
    }

    /**
     * Create product object from form data
     */
    private Product getProductFromForm() {
        String reference = etReference.getText().toString().trim();
        String label = etLabel.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String barcode = etBarcode.getText().toString().trim();

        int quantity = 0;
        try {
            quantity = Integer.parseInt(etQuantity.getText().toString().trim());
        } catch (NumberFormatException ignored) {}

        double price = 0.0;
        try {
            price = Double.parseDouble(etPrice.getText().toString().trim());
        } catch (NumberFormatException ignored) {}

        // Create product object
        Product product = new Product();

        // If editing, set existing ID
        if (!viewModel.isCreateMode() && viewModel.getProduct().getValue() != null) {
            product.setId(viewModel.getProduct().getValue().getId());
        }

        product.setReference(reference);
        product.setLabel(label);
        product.setName(name);
        product.setDescription(description);
        product.setBarcode(barcode);
        product.setQuantity(quantity);
        product.setPrice(price);
        product.setPhotoUrl(viewModel.getPhotoUrl().getValue());
        product.setUserId(viewModel.getCurrentUserId());

        return product;
    }

    /**
     * Confirm product deletion
     */
    private void confirmDeleteProduct() {
        DialogUtils.showConfirmationDialog(
                this,
                getString(R.string.delete_product),
                "Êtes-vous sûr de vouloir supprimer ce produit ?",
                () -> viewModel.deleteProduct()
        );
    }

    // ImageHelper callback
    @Override
    public void onImageSelected(Uri imageUri) {
        viewModel.uploadProductPhoto(imageUri, this);
    }

    // BarcodeHelper callback
    @Override
    public void onBarcodeScanned(String barcode) {
        etBarcode.setText(barcode);
        etReference.setText(barcode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check if result is from barcode scanner
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {
            barcodeHelper.handleScanResult(scanResult);
            return;
        }

        // Otherwise handle as image result
        if (resultCode == RESULT_OK) {
            imageHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        imageHelper.cleanup();
    }
}