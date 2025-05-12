package com.drogpulseai.activities.suppliers;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drogpulseai.R;
import com.drogpulseai.adapters.SelectableProductAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Product;
import com.drogpulseai.models.SupplierProductRequest;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.NetworkResult;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupplierProductsActivity extends AppCompatActivity implements SelectableProductAdapter.OnProductSelectionListener {

    // Fournisseur
    private int supplierId;
    private String supplierName;
    private String supplierPhone;
    private String supplierNotes;

    // UI Components
    private TextView tvSupplierName;
    private TextView tvSupplierPhone;
    private TextView tvSupplierNotes;
    private RecyclerView recyclerView;
    private TextView tvEmptyView;
    private TextView tvSelectedCount;
    private MaterialButton btnAddToSupplier;
    private LinearLayout supplierSettingsPanel;
    private EditText etSearch;
    private MaterialButton btnSearch;
    private FrameLayout loadingOverlay;
    private EditText etPrice;
    private EditText etDeliveryTime;
    private EditText etDeliveryConditions;
    private EditText etNotes;
    private CheckBox cbPrimarySupplier;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;
    private User currentUser;

    // Données
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();
    private Set<Integer> selectedProductIds = new HashSet<>();
    private SelectableProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supplier_products);

        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Produits pour fournisseur");
        }

        // Récupérer les informations du fournisseur depuis l'intent
        supplierId = getIntent().getIntExtra("supplier_id", -1);
        supplierName = getIntent().getStringExtra("supplier_name");
        supplierPhone = getIntent().getStringExtra("supplier_phone");
        supplierNotes = getIntent().getStringExtra("supplier_note");

        if (supplierId == -1) {
            Toast.makeText(this, "Erreur: Fournisseur non spécifié", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialiser les utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);
        currentUser = sessionManager.getUser();

        // Initialiser les vues
        initializeViews();

        // Afficher les informations du fournisseur
        displaySupplierInfo();

        // Configurer le RecyclerView
        setupRecyclerView();

        // Configurer les listeners
        setupListeners();

        // Charger les produits
        loadProducts();
    }

    private void initializeViews() {
        tvSupplierName = findViewById(R.id.tv_supplier_name);
        tvSupplierPhone = findViewById(R.id.tv_supplier_phone);
        tvSupplierNotes = findViewById(R.id.tv_supplier_notes);
        recyclerView = findViewById(R.id.recyclerview_products);
        tvEmptyView = findViewById(R.id.tv_empty_view);
        tvSelectedCount = findViewById(R.id.tv_selected_count);
        btnAddToSupplier = findViewById(R.id.btn_add_to_supplier);
        supplierSettingsPanel = findViewById(R.id.supplier_settings_panel);
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);
        loadingOverlay = findViewById(R.id.loading_overlay);
        etPrice = findViewById(R.id.et_price);
        etDeliveryTime = findViewById(R.id.et_delivery_time);
        etDeliveryConditions = findViewById(R.id.et_delivery_conditions);
        etNotes = findViewById(R.id.et_notes);
        cbPrimarySupplier = findViewById(R.id.cb_primary_supplier);
    }

    private void displaySupplierInfo() {
        tvSupplierName.setText(supplierName);
        tvSupplierPhone.setText(supplierPhone);
        tvSupplierNotes.setText(supplierNotes != null && !supplierNotes.isEmpty() ?
                supplierNotes : "Aucune note");
    }

    private void setupRecyclerView() {
        adapter = new SelectableProductAdapter(this, filteredProducts, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> filterProducts());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterProducts();
            }
        });

        btnAddToSupplier.setOnClickListener(v -> {
            // Vérifier si des produits sont sélectionnés
            if (selectedProductIds.isEmpty()) {
                Toast.makeText(SupplierProductsActivity.this,
                        "Veuillez sélectionner au moins un produit", Toast.LENGTH_SHORT).show();
                return;
            }

            // Vérifier la connexion réseau
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "Aucune connexion Internet", Toast.LENGTH_LONG).show();
                return;
            }

            // Récupérer les valeurs des champs
            double price = 0;
            try {
                price = Double.parseDouble(etPrice.getText().toString());
            } catch (NumberFormatException e) {
                // Prix optionnel, donc on continue si vide
            }

            Integer deliveryTime = null;
            try {
                if (!etDeliveryTime.getText().toString().isEmpty()) {
                    deliveryTime = Integer.parseInt(etDeliveryTime.getText().toString());
                }
            } catch (NumberFormatException e) {
                // Délai optionnel, donc on continue si vide
            }

            String deliveryConditions = etDeliveryConditions.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();
            boolean isPrimary = cbPrimarySupplier.isChecked();

            // Afficher le chargement
            loadingOverlay.setVisibility(View.VISIBLE);

            // Compteur pour suivre les requêtes en cours
            AtomicInteger pendingRequests = new AtomicInteger(selectedProductIds.size());
            AtomicInteger successCount = new AtomicInteger(0);

            // Itérer sur les produits sélectionnés
            for (Integer productId : selectedProductIds) {
                // Préparer la requête
                SupplierProductRequest request = new SupplierProductRequest();
                request.setProductId(productId);
                request.setContactId(supplierId);
                request.setPrimary(isPrimary);
                request.setPrice(price);
                request.setDeliveryTime(deliveryTime);
                request.setDeliveryConditions(deliveryConditions.isEmpty() ? null : deliveryConditions);
                request.setNotes(notes.isEmpty() ? null : notes);

                // Appeler l'API
                apiService.addProductSupplier(request).enqueue(new Callback<NetworkResult<Void>>() {
                    @Override
                    public void onResponse(Call<NetworkResult<Void>> call, Response<NetworkResult<Void>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            successCount.incrementAndGet();
                        }

                        // Vérifier si c'est la dernière requête
                        checkRequestCompletion(pendingRequests, successCount);
                    }

                    @Override
                    public void onFailure(Call<NetworkResult<Void>> call, Throwable t) {
                        // Vérifier si c'est la dernière requête
                        checkRequestCompletion(pendingRequests, successCount);
                    }
                });
            }
        });
    }

    private void checkRequestCompletion(AtomicInteger pendingRequests, AtomicInteger successCount) {
        int remaining = pendingRequests.decrementAndGet();
        if (remaining <= 0) {
            // Toutes les requêtes sont terminées
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.GONE);
                int totalSelected = selectedProductIds.size();
                int successes = successCount.get();

                if (successes == totalSelected) {
                    // Toutes les requêtes ont réussi
                    Snackbar.make(recyclerView,
                            "Tous les produits ont été associés au fournisseur",
                            Snackbar.LENGTH_LONG).show();

                    // Réinitialiser la sélection
                    selectedProductIds.clear();
                    adapter.notifyDataSetChanged();
                    updateSelectedCount();

                    // Masquer le panneau de paramètres
                    supplierSettingsPanel.setVisibility(View.GONE);

                    // Vider les champs
                    etPrice.setText("");
                    etDeliveryTime.setText("");
                    etDeliveryConditions.setText("");
                    etNotes.setText("");
                    cbPrimarySupplier.setChecked(false);
                } else {
                    // Certaines requêtes ont échoué
                    Snackbar.make(recyclerView,
                            successes + " sur " + totalSelected + " produits ont été associés",
                            Snackbar.LENGTH_LONG).show();
                }
            });
        }
    }

    private void loadProducts() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Aucune connexion Internet", Toast.LENGTH_LONG).show();
            return;
        }

        loadingOverlay.setVisibility(View.VISIBLE);

        apiService.getProducts(currentUser.getId()).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                loadingOverlay.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    allProducts = response.body();
                    filteredProducts.clear();
                    filteredProducts.addAll(allProducts);
                    adapter.notifyDataSetChanged();

                    updateEmptyView();
                } else {
                    Toast.makeText(SupplierProductsActivity.this,
                            "Erreur lors du chargement des produits", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(SupplierProductsActivity.this,
                        "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filterProducts() {
        String query = etSearch.getText().toString().toLowerCase().trim();

        filteredProducts.clear();

        if (query.isEmpty()) {
            filteredProducts.addAll(allProducts);
        } else {
            for (Product product : allProducts) {
                if (matchesQuery(product, query)) {
                    filteredProducts.add(product);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private boolean matchesQuery(Product product, String query) {
        return (product.getName() != null && product.getName().toLowerCase().contains(query))
                || (product.getReference() != null && product.getReference().toLowerCase().contains(query))
                || (product.getBarcode() != null && product.getBarcode().toLowerCase().contains(query));
    }

    private void updateEmptyView() {
        if (filteredProducts.isEmpty()) {
            tvEmptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onProductSelected(int productId, boolean isSelected) {
        if (isSelected) {
            selectedProductIds.add(productId);
        } else {
            selectedProductIds.remove(productId);
        }

        updateSelectedCount();
    }

    private void updateSelectedCount() {
        int count = selectedProductIds.size();
        tvSelectedCount.setText(count + " produit(s) sélectionné(s)");
        btnAddToSupplier.setEnabled(count > 0);

        // Afficher ou masquer le panneau de paramètres
        supplierSettingsPanel.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}