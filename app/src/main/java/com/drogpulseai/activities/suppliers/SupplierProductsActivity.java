package com.drogpulseai.activities.suppliers;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drogpulseai.R;
import com.drogpulseai.activities.products.ProductFormActivity;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private FloatingActionButton fabAddProduct;

    private SwipeRefreshLayout swipeRefreshLayout;

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

        // Configurer le SwipeRefreshLayout
        setupSwipeRefresh();

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
        // Initialiser le FAB
        fabAddProduct = findViewById(R.id.fab_add_product);
        // Initialiser le SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
    }

    private void displaySupplierInfo() {
        tvSupplierName.setText(supplierName);
        tvSupplierPhone.setText(supplierPhone);
        tvSupplierNotes.setText(supplierNotes != null && !supplierNotes.isEmpty() ?
                supplierNotes : "Aucune note");
    }

    private void setupRecyclerView() {
        adapter = new SelectableProductAdapter(this, filteredProducts, this);
        // Initialiser l'adaptateur avec la sélection actuelle
        adapter.setSelectedProductIds(selectedProductIds);
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
        // Configurer le clic sur le FAB
        fabAddProduct.setOnClickListener(v -> {
            // Lancer l'activité ProductFormActivity en mode création
            Intent intent = new Intent(SupplierProductsActivity.this, ProductFormActivity.class);
            intent.putExtra("mode", "create");
            intent.putExtra("supplier_id", supplierId);
            intent.putExtra("supplier_name", supplierName);
            intent.putExtra("supplier_phone", supplierPhone);
            intent.putExtra("supplier_note", supplierNotes);
            startActivity(intent);
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
            // Ensemble pour stocker les ID des produits associés au fournisseur


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


    /**
     * Charge les produits depuis l'API, en affichant d'abord ceux associés au fournisseur
     */
    private void loadProducts() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Aucune connexion Internet", Toast.LENGTH_LONG).show();
            return;
        }

        loadingOverlay.setVisibility(View.VISIBLE);

        // Ensemble pour stocker les ID des produits associés au fournisseur
        final Set<Integer> associatedProductIds = new HashSet<>();
        final Map<Integer, ProductSupplierInfo> supplierProductInfo = new HashMap<>();

        // 1. D'abord récupérer les produits associés au fournisseur
        apiService.getSupplierProducts(supplierId, currentUser.getId()).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        List<Map<String, Object>> supplierProducts = response.body();

                        for (Map<String, Object> product : supplierProducts) {
                            // Extraire l'ID du produit
                            int productId = ((Double) product.get("product_id")).intValue();
                            associatedProductIds.add(productId);

                            // Créer un objet pour stocker les infos du fournisseur pour ce produit
                            ProductSupplierInfo info = new ProductSupplierInfo();

                            // Extraire le prix
                            if (product.containsKey("price")) {
                                Object priceObj = product.get("price");
                                if (priceObj instanceof Double) {
                                    info.setPrice((Double) priceObj);
                                } else if (priceObj instanceof String) {
                                    try {
                                        info.setPrice(Double.parseDouble((String) priceObj));
                                    } catch (NumberFormatException e) {
                                        Log.e("SupplierProductsActivity", "Error parsing price: " + e.getMessage());
                                    }
                                }
                            }

                            // Extraire si c'est un fournisseur principal
                            if (product.containsKey("is_primary")) {
                                info.setPrimarySupplier((Boolean) product.get("is_primary"));
                            }

                            // Extraire le délai de livraison
                            if (product.containsKey("delivery_time")) {
                                Object deliveryTimeObj = product.get("delivery_time");
                                if (deliveryTimeObj instanceof Double) {
                                    info.setDeliveryTime(((Double) deliveryTimeObj).intValue());
                                } else if (deliveryTimeObj instanceof Integer) {
                                    info.setDeliveryTime((Integer) deliveryTimeObj);
                                }
                            }

                            // Stocker les infos pour ce produit
                            supplierProductInfo.put(productId, info);
                        }
                    } catch (Exception e) {
                        Log.e("SupplierProductsActivity", "Error processing response: " + e.getMessage());
                    }
                }

                // Passer à l'étape suivante : charger tous les produits
                loadAllProducts(associatedProductIds, supplierProductInfo);
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Log.e("SupplierProductsActivity", "API Error: " + t.getMessage());
                // En cas d'erreur réseau, charger quand même tous les produits
                loadAllProducts(new HashSet<>(), new HashMap<>());
            }
        });
    }

    /**
     * Classe pour stocker les informations du fournisseur pour un produit spécifique
     */
    private static class ProductSupplierInfo {
        private Double price;
        private Boolean isPrimarySupplier;
        private Integer deliveryTime;

        // Getters et setters
        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public Boolean isPrimarySupplier() {
            return isPrimarySupplier != null && isPrimarySupplier;
        }

        public void setPrimarySupplier(Boolean primarySupplier) {
            isPrimarySupplier = primarySupplier;
        }

        public Integer getDeliveryTime() {
            return deliveryTime;
        }

        public void setDeliveryTime(Integer deliveryTime) {
            this.deliveryTime = deliveryTime;
        }
    }

    /**
     * Charge tous les produits et marque ceux qui sont déjà associés au fournisseur
     */
    private void loadAllProducts(final Set<Integer> associatedProductIds, final Map<Integer, ProductSupplierInfo> supplierProductInfo) {
        apiService.getProducts(currentUser.getId()).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                loadingOverlay.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    allProducts = response.body();

                    // Marquer les produits associés et trier la liste
                    List<Product> associatedProducts = new ArrayList<>();
                    List<Product> nonAssociatedProducts = new ArrayList<>();

                    for (Product product : allProducts) {
                        // Vérifier si ce produit est associé au fournisseur
                        boolean isAssociated = associatedProductIds.contains(product.getId());
                        product.setAssociatedWithSupplier(isAssociated);

                        // Si associé, ajouter toutes les informations du fournisseur
                        if (isAssociated && supplierProductInfo.containsKey(product.getId())) {
                            ProductSupplierInfo info = supplierProductInfo.get(product.getId());
                            product.setSupplierPrice(info.getPrice());
                            product.setPrimarySupplier(info.isPrimarySupplier());
                            product.setDeliveryTime(info.getDeliveryTime());
                        }

                        // Séparer les produits en deux listes: associés et non-associés
                        if (isAssociated) {
                            associatedProducts.add(product);
                        } else {
                            nonAssociatedProducts.add(product);
                        }
                    }

                    // Trier les produits associés (fournisseurs principaux en premier)
                    Collections.sort(associatedProducts, (p1, p2) -> {
                        // Fournisseurs principaux en premier
                        if (p1.isPrimarySupplier() && !p2.isPrimarySupplier()) return -1;
                        if (!p1.isPrimarySupplier() && p2.isPrimarySupplier()) return 1;

                        // Ensuite, trier par référence
                        return p1.getReference().compareTo(p2.getReference());
                    });

                    // Réorganiser la liste complète: associés en premier, puis les autres
                    allProducts.clear();
                    allProducts.addAll(associatedProducts);
                    allProducts.addAll(nonAssociatedProducts);

                    // Mettre à jour la liste affichée
                    filteredProducts.clear();
                    filteredProducts.addAll(allProducts);
                    adapter.notifyDataSetChanged();
                    adapter.setSelectedProductIds(selectedProductIds);
                    updateEmptyView();

                    // Afficher un message informatif
                    if (!associatedProducts.isEmpty()) {
                        Snackbar.make(recyclerView,
                                associatedProducts.size() + " produits déjà associés à ce fournisseur",
                                Snackbar.LENGTH_LONG).show();
                    }
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

    /**
     * Filtre les produits en fonction de la recherche
     */
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
        // Maintenir l'état de sélection après le filtrage
        adapter.setSelectedProductIds(selectedProductIds);
        updateEmptyView();

        // Arrêter l'animation de rafraîchissement si elle était active
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
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

    /**
     * Configure le SwipeRefreshLayout
     */
    private void setupSwipeRefresh() {
        // Définir les couleurs de l'indicateur de rafraîchissement
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.accent,
                R.color.primaryDark
        );

        // Définir l'action à effectuer lors d'un rafraîchissement
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Vider les listes et réinitialiser l'état
            allProducts.clear();
            filteredProducts.clear();
            adapter.notifyDataSetChanged();

            // Recharger les produits
            refreshProducts();
        });
    }
    /**
     * Rafraîchit la liste des produits
     * Cette méthode est similaire à loadProducts() mais gère l'indicateur de rafraîchissement
     */
    private void refreshProducts() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Aucune connexion Internet", Toast.LENGTH_LONG).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Ne pas afficher le loading overlay pendant le rafraîchissement
        // car le SwipeRefreshLayout montre déjà un indicateur

        // Ensemble pour stocker les ID des produits associés au fournisseur
        final Set<Integer> associatedProductIds = new HashSet<>();
        final Map<Integer, ProductSupplierInfo> supplierProductInfo = new HashMap<>();

        // 1. D'abord récupérer les produits associés au fournisseur
        apiService.getSupplierProducts(supplierId, currentUser.getId()).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                // Même logique que dans loadProducts()
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        List<Map<String, Object>> supplierProducts = response.body();

                        for (Map<String, Object> product : supplierProducts) {
                            // Extraire l'ID du produit
                            int productId = ((Double) product.get("product_id")).intValue();
                            associatedProductIds.add(productId);

                            // Créer un objet pour stocker les infos du fournisseur pour ce produit
                            ProductSupplierInfo info = new ProductSupplierInfo();

                            // Extraire le prix
                            if (product.containsKey("price")) {
                                Object priceObj = product.get("price");
                                if (priceObj instanceof Double) {
                                    info.setPrice((Double) priceObj);
                                } else if (priceObj instanceof String) {
                                    try {
                                        info.setPrice(Double.parseDouble((String) priceObj));
                                    } catch (NumberFormatException e) {
                                        Log.e("SupplierProductsActivity", "Error parsing price: " + e.getMessage());
                                    }
                                }
                            }

                            // Extraire si c'est un fournisseur principal
                            if (product.containsKey("is_primary")) {
                                info.setPrimarySupplier((Boolean) product.get("is_primary"));
                            }

                            // Extraire le délai de livraison
                            if (product.containsKey("delivery_time")) {
                                Object deliveryTimeObj = product.get("delivery_time");
                                if (deliveryTimeObj instanceof Double) {
                                    info.setDeliveryTime(((Double) deliveryTimeObj).intValue());
                                } else if (deliveryTimeObj instanceof Integer) {
                                    info.setDeliveryTime((Integer) deliveryTimeObj);
                                }
                            }

                            // Stocker les infos pour ce produit
                            supplierProductInfo.put(productId, info);
                        }
                    } catch (Exception e) {
                        Log.e("SupplierProductsActivity", "Error processing response: " + e.getMessage());
                    }
                }

                // Passer à l'étape suivante : charger tous les produits
                refreshAllProducts(associatedProductIds, supplierProductInfo);
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Log.e("SupplierProductsActivity", "API Error: " + t.getMessage());
                // En cas d'erreur réseau, charger quand même tous les produits
                refreshAllProducts(new HashSet<>(), new HashMap<>());
            }
        });
    }

    /**
     * Version de loadAllProducts() adaptée pour le rafraîchissement
     */
    private void refreshAllProducts(final Set<Integer> associatedProductIds, final Map<Integer, ProductSupplierInfo> supplierProductInfo) {
        apiService.getProducts(currentUser.getId()).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                // Arrêter l'animation de rafraîchissement
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    allProducts = response.body();

                    // Marquer les produits associés et trier la liste
                    List<Product> associatedProducts = new ArrayList<>();
                    List<Product> nonAssociatedProducts = new ArrayList<>();

                    for (Product product : allProducts) {
                        // Vérifier si ce produit est associé au fournisseur
                        boolean isAssociated = associatedProductIds.contains(product.getId());
                        product.setAssociatedWithSupplier(isAssociated);

                        // Si associé, ajouter toutes les informations du fournisseur
                        if (isAssociated && supplierProductInfo.containsKey(product.getId())) {
                            ProductSupplierInfo info = supplierProductInfo.get(product.getId());
                            product.setSupplierPrice(info.getPrice());
                            product.setPrimarySupplier(info.isPrimarySupplier());
                            product.setDeliveryTime(info.getDeliveryTime());
                        }

                        // Séparer les produits en deux listes: associés et non-associés
                        if (isAssociated) {
                            associatedProducts.add(product);
                        } else {
                            nonAssociatedProducts.add(product);
                        }
                    }

                    // Trier les produits associés (fournisseurs principaux en premier)
                    Collections.sort(associatedProducts, (p1, p2) -> {
                        // Fournisseurs principaux en premier
                        if (p1.isPrimarySupplier() && !p2.isPrimarySupplier()) return -1;
                        if (!p1.isPrimarySupplier() && p2.isPrimarySupplier()) return 1;

                        // Ensuite, trier par référence
                        return p1.getReference().compareTo(p2.getReference());
                    });

                    // Réorganiser la liste complète: associés en premier, puis les autres
                    allProducts.clear();
                    allProducts.addAll(associatedProducts);
                    allProducts.addAll(nonAssociatedProducts);

                    // Mettre à jour la liste affichée
                    filteredProducts.clear();
                    filteredProducts.addAll(allProducts);
                    adapter.notifyDataSetChanged();
                    adapter.setSelectedProductIds(selectedProductIds);
                    updateEmptyView();

                    // Afficher un message informatif
                    if (!associatedProducts.isEmpty()) {
                        Snackbar.make(recyclerView,
                                associatedProducts.size() + " produits déjà associés à ce fournisseur",
                                Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(SupplierProductsActivity.this,
                            "Erreur lors du chargement des produits", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                // Arrêter l'animation de rafraîchissement
                swipeRefreshLayout.setRefreshing(false);

                Toast.makeText(SupplierProductsActivity.this,
                        "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onProductSelected(int productId, boolean isSelected) {
        if (isSelected) {
            selectedProductIds.add(productId);
        } else {
            selectedProductIds.remove(productId);
        }

        updateSelectedCount();
        // Mettre à jour l'adaptateur avec les IDs sélectionnés
        adapter.setSelectedProductIds(selectedProductIds);
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

    @Override
    protected void onResume() {
        super.onResume();

        // Si nous revenons de ProductFormActivity, recharger les produits
        if (!swipeRefreshLayout.isRefreshing() && !loadingOverlay.isShown()) {
            // Utiliser swipeRefreshLayout pour montrer visuellement le chargement
            swipeRefreshLayout.setRefreshing(true);
            refreshProducts();
        }
    }
}