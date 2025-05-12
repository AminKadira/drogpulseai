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
import android.widget.FrameLayout;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for creating and editing products
 * Optimized version with ViewModel architecture and improved separation of concerns
 * Updated to support multiple photos (up to 3)
 */
public class ProductFormActivity extends AppCompatActivity implements
        ImageHelper.ImageSelectionCallback,
        BarcodeHelper.BarcodeCallback {

    private static final String TAG = "ProductFormActivity";
    private static final int MAX_PHOTOS = 3;

    // Views
    private Button btnSave, btnDelete;
    private ImageButton btnScanBarcode;
    private EditText etReference, etLabel, etName, etDescription, etBarcode, etQuantity, etPrice;
    private ProgressBar progressBar;

    // Photo views
    private ImageView[] photoImageViews = new ImageView[MAX_PHOTOS];
    private Button[] addPhotoButtons = new Button[MAX_PHOTOS];
    private ImageButton[] removePhotoButtons = new ImageButton[MAX_PHOTOS];
    private FrameLayout[] photoFrames = new FrameLayout[MAX_PHOTOS];

    // Photo management
    private int currentPhotoIndex = 0;
    private List<String> photoUrls = new ArrayList<>();
    private FrameLayout photoFullscreenOverlay;
    private ImageView fullscreenImageView;
    private ImageButton btnCloseFullscreen;
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

        // Initialisation des vues pour l'affichage plein écran
        photoFullscreenOverlay = findViewById(R.id.photo_fullscreen_overlay);
        fullscreenImageView = findViewById(R.id.fullscreen_image_view);
        btnCloseFullscreen = findViewById(R.id.btn_close_fullscreen);

        // Initialize views
        initializeViews();
        initializePhotoViews();

        // Configure toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Configurer le bouton de fermeture
        btnCloseFullscreen.setOnClickListener(v -> hideFullscreenImage());
        photoFullscreenOverlay.setOnClickListener(v -> hideFullscreenImage());

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
     * Initialize photo views
     */
    private void initializePhotoViews() {
        // Initialize arrays for photo views
        for (int i = 0; i < MAX_PHOTOS; i++) {
            final int index = i;

            // Get view references
            int imageViewId = getResources().getIdentifier("iv_product_photo_" + (i + 1), "id", getPackageName());
            int addButtonId = getResources().getIdentifier("btn_add_photo_" + (i + 1), "id", getPackageName());
            int removeButtonId = getResources().getIdentifier("btn_remove_photo_" + (i + 1), "id", getPackageName());
            int frameId = getResources().getIdentifier("frame_photo_" + (i + 1), "id", getPackageName());

            photoImageViews[i] = findViewById(imageViewId);
            addPhotoButtons[i] = findViewById(addButtonId);
            removePhotoButtons[i] = findViewById(removeButtonId);
            photoFrames[i] = findViewById(frameId);

            // Ajouter un clickListener sur l'ImageView
            photoImageViews[i].setOnClickListener(v -> showFullscreenImage(index));

            // Configure listeners for buttons
            addPhotoButtons[i].setOnClickListener(v -> {
                currentPhotoIndex = index;
                checkNetworkAndSelectImage();
            });

            removePhotoButtons[i].setOnClickListener(v -> {
                removePhoto(index);
            });
        }

        // First frame is always visible
        photoFrames[0].setVisibility(View.VISIBLE);
    }

    /**
     * Afficher une image en plein écran
     */
    private void showFullscreenImage(int index) {
        if (index < photoUrls.size()) {
            // Charger l'image en plein écran
            imageHelper.displayImage(photoUrls.get(index), fullscreenImageView);

            // Afficher l'overlay
            photoFullscreenOverlay.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Cacher l'image en plein écran
     */
    private void hideFullscreenImage() {
        photoFullscreenOverlay.setVisibility(View.GONE);
    }

    // Ajoutez également cette méthode pour gérer le comportement du bouton "Retour"
    @Override
    public void onBackPressed() {
        // Si l'overlay est visible, le fermer
        if (photoFullscreenOverlay.getVisibility() == View.VISIBLE) {
            hideFullscreenImage();
        } else {
            // Sinon, comportement normal
            super.onBackPressed();
        }
    }
    /**
     * Check network availability before showing image dialog
     */
    private void checkNetworkAndSelectImage() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            imageHelper.showImageSourceDialog();
        } else {
            Toast.makeText(this, "Connexion internet requise pour ajouter une photo", Toast.LENGTH_LONG).show();
            new AlertDialog.Builder(this)
                    .setTitle("Connexion requise")
                    .setMessage("Une connexion internet est nécessaire pour télécharger des photos. Veuillez vous connecter à Internet et réessayer.")
                    .setPositiveButton("OK", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    /**
     * Remove a photo at the specified index
     */
    private void removePhoto(int index) {
        // Clear image and hide remove button
        photoImageViews[index].setImageDrawable(null);
        removePhotoButtons[index].setVisibility(View.GONE);
        addPhotoButtons[index].setVisibility(View.VISIBLE);

        // Remove URL from list
        if (index < photoUrls.size()) {
            photoUrls.remove(index);
        }

        // Reorganize photos
        reorganizePhotos();

        // Update ViewModel
        viewModel.setPhotoUrls(photoUrls);
    }

    /**
     * Reorganize photos after deletion
     */
    private void reorganizePhotos() {
        // Hide unused frames
        for (int i = 0; i < MAX_PHOTOS; i++) {
            if (i == 0 || i < photoUrls.size() + 1) {
                photoFrames[i].setVisibility(View.VISIBLE);
            } else {
                photoFrames[i].setVisibility(View.GONE);
            }
        }
    }

    /**
     * Add or update a photo at current index
     */
    private void updatePhotoAtCurrentIndex(String url) {
        if (currentPhotoIndex < MAX_PHOTOS) {
            // Update display
            imageHelper.displayImage(url, photoImageViews[currentPhotoIndex]);
            addPhotoButtons[currentPhotoIndex].setVisibility(View.GONE);
            removePhotoButtons[currentPhotoIndex].setVisibility(View.VISIBLE);

            // Update URL list
            if (currentPhotoIndex < photoUrls.size()) {
                photoUrls.set(currentPhotoIndex, url);
            } else {
                photoUrls.add(url);

                // Make next photo slot visible if we haven't reached max
                if (photoUrls.size() < MAX_PHOTOS) {
                    photoFrames[photoUrls.size()].setVisibility(View.VISIBLE);
                }
            }

            // Update ViewModel
            viewModel.setPhotoUrls(photoUrls);
        }
    }

    /**
     * Display existing photos from a product
     */
    private void displayExistingPhotos(List<String> urls) {
        photoUrls.clear();
        photoUrls.addAll(urls);

        for (int i = 0; i < MAX_PHOTOS; i++) {
            if (i < urls.size()) {
                // Afficher la photo existante
                imageHelper.displayImage(urls.get(i), photoImageViews[i]);
                addPhotoButtons[i].setVisibility(View.GONE);
                removePhotoButtons[i].setVisibility(View.VISIBLE);
                photoFrames[i].setVisibility(View.VISIBLE);

                // S'assurer que l'ImageView est cliquable
                photoImageViews[i].setClickable(true);
            } else if (i == urls.size()) {
                // Rendre visible le prochain emplacement vide
                photoFrames[i].setVisibility(View.VISIBLE);
                addPhotoButtons[i].setVisibility(View.VISIBLE);
                removePhotoButtons[i].setVisibility(View.GONE);

                // Réinitialiser l'image et la rendre non cliquable
                photoImageViews[i].setImageDrawable(null);
                photoImageViews[i].setClickable(false);
            } else {
                // Cacher les emplacements inutilisés
                photoFrames[i].setVisibility(View.GONE);

                // Réinitialiser l'image et la rendre non cliquable
                photoImageViews[i].setImageDrawable(null);
                photoImageViews[i].setClickable(false);
            }
        }
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

        // Observe photo URLs list
        viewModel.getPhotoUrls().observe(this, urls -> {
            if (urls != null && !urls.isEmpty()) {
                displayExistingPhotos(urls);
            }
        });

        // Observe newly uploaded photo URL
        viewModel.getNewPhotoUrl().observe(this, result -> {
            if (result != null && result.getIndex() >= 0 && result.getUrl() != null && !result.getUrl().isEmpty()) {
                updatePhotoAtCurrentIndex(result.getUrl());
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
        etBarcode.setText(product.getReference());
        etQuantity.setText(String.valueOf(product.getQuantity()));
        //etPrice.setText(String.format("%.2f", product.getPrice()));
        etPrice.setText(String.format(Locale.US, "%.2f", product.getPrice()));

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

        // Enable/disable photo buttons
        for (int i = 0; i < MAX_PHOTOS; i++) {
            if (photoFrames[i].getVisibility() == View.VISIBLE) {
                addPhotoButtons[i].setEnabled(enabled);
                removePhotoButtons[i].setEnabled(enabled);
            }
        }
    }

    /**
     * Validate form and save product
     */
    private void validateAndSaveProduct() {
        // Create validation map
        ValidationMap validationMap = new ValidationMap();
        validationMap.add(etReference, validator.required("Référence requise"));
        validationMap.add(etName, validator.required("Nom requis"));
        validationMap.add(etQuantity, validator.integer("Quantité invalide"));
        validationMap.add(etPrice, validator.decimal("Prix invalide"));
        validationMap.add(etPrice, validator.required("Prix requis"));

        // Validate form
        if (!validator.validate(validationMap)) {
            return;
        }

        // Create product object from form data
        Product product = getProductFromForm();

        // Check network connectivity
        if (NetworkUtils.isNetworkAvailable(this)) {
            // Internet connection available - save via API
            viewModel.saveProduct(product);
        } else {
            // No internet connection - save locally
            viewModel.saveProductLocally(product);

            // Inform user
            Toast.makeText(this, "Produit sauvegardé localement. Synchronisation automatique dès que la connexion sera rétablie.",
                    Toast.LENGTH_LONG).show();

            // Finish activity
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
        product.setUserId(viewModel.getCurrentUserId());

        // Set photo URLs from the list
        if (!photoUrls.isEmpty()) {
            if (photoUrls.size() > 0) product.setPhotoUrl(photoUrls.get(0));
            if (photoUrls.size() > 1) product.setPhotoUrl2(photoUrls.get(1));
            if (photoUrls.size() > 2) product.setPhotoUrl3(photoUrls.get(2));
        }

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
        viewModel.uploadProductPhoto(imageUri, this, currentPhotoIndex);
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