package com.drogpulseai.activities.products;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.drogpulseai.R;
import com.drogpulseai.adapters.SupplierAdapter;
import com.drogpulseai.activities.suppliers.SupplierProductsActivity;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Product;
import com.drogpulseai.models.Supplier;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductSuppliersActivity extends AppCompatActivity implements SupplierAdapter.OnSupplierClickListener {

    // Constantes
    private static final String TAG = "ProductSuppliersActivity";

    // Produit
    private int productId;
    private Product product;

    // UI Components
    private ImageView ivProductImage;
    private TextView tvProductReference;
    private TextView tvProductName;
    private TextView tvProductQuantity;
    private RecyclerView recyclerView;
    private TextView tvEmptySuppliers;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddSupplier;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;
    private User currentUser;

    // Données
    private List<Supplier> suppliers = new ArrayList<>();
    private SupplierAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_suppliers);

        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.product_suppliers);
        }

        // Récupérer l'ID du produit depuis l'intent
        productId = getIntent().getIntExtra("product_id", -1);

        // Option: récupérer tout l'objet produit si passé
        if (getIntent().hasExtra("product")) {
            product = (Product) getIntent().getSerializableExtra("product");
        }

        if (productId == -1 && product == null) {
            Toast.makeText(this, "Erreur: Produit non spécifié", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (product != null) {
            productId = product.getId();
        }

        // Initialiser les utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);
        currentUser = sessionManager.getUser();

        // Initialiser les vues
        initializeViews();

        // Configurer le RecyclerView
        setupRecyclerView();

        // Configurer le SwipeRefreshLayout
        setupSwipeRefresh();

        // Configurer les listeners
        setupListeners();

        // Afficher les informations du produit si disponibles
        if (product != null) {
            displayProductInfo(product);
        }

        // Charger les fournisseurs
        loadSuppliers();
    }

    private void initializeViews() {
        ivProductImage = findViewById(R.id.iv_product_image);
        tvProductReference = findViewById(R.id.tv_product_reference);
        tvProductName = findViewById(R.id.tv_product_name);
        tvProductQuantity = findViewById(R.id.tv_product_quantity);
      //  tvProductBarcode = findViewById(R.id.tv_product_barcode);
        recyclerView = findViewById(R.id.recycler_view_suppliers);
        tvEmptySuppliers = findViewById(R.id.tv_empty_suppliers);
        progressBar = findViewById(R.id.progress_bar);
        fabAddSupplier = findViewById(R.id.fab_add_supplier);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
    }

    private void setupRecyclerView() {
        adapter = new SupplierAdapter(this, suppliers, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.accent,
                R.color.primaryDark
        );

        swipeRefreshLayout.setOnRefreshListener(this::loadSuppliers);
    }

    private void setupListeners() {
        fabAddSupplier.setOnClickListener(v -> {
            // Lancer une activité pour ajouter un fournisseur au produit
            Toast.makeText(this, "Fonctionnalité à implémenter: Ajouter un fournisseur", Toast.LENGTH_SHORT).show();

            // Exemple d'implémentation:
            // Intent intent = new Intent(ProductSuppliersActivity.this, SelectSupplierActivity.class);
            // intent.putExtra("product_id", productId);
            // startActivity(intent);
        });
    }

    private void displayProductInfo(Product product) {
        // Afficher les informations du produit
        tvProductReference.setText(product.getReference());
        tvProductName.setText(product.getName());
        tvProductQuantity.setText(getString(R.string.stock_format, product.getQuantity()));

//        if (product.getBarcode() != null && !product.getBarcode().isEmpty()) {
//            tvProductBarcode.setVisibility(View.VISIBLE);
//            tvProductBarcode.setText(product.getBarcode());
//        } else {
//            tvProductBarcode.setVisibility(View.GONE);
//        }

        // Charger l'image du produit si disponible
        if (product.getPhotoUrl() != null && !product.getPhotoUrl().isEmpty()) {
            String photoUrl = product.getPhotoUrl();
            String fullUrl;

            if (photoUrl.startsWith("http") || photoUrl.startsWith("https")) {
                fullUrl = photoUrl;
            } else {
                String baseUrl = ApiClient.getBaseUrl();
                if (!baseUrl.endsWith("/") && !photoUrl.startsWith("/")) {
                    baseUrl += "/";
                }
                fullUrl = baseUrl + photoUrl;
            }

            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(ivProductImage);
        } else {
            Glide.with(this)
                    .load(R.drawable.ic_image_placeholder)
                    .into(ivProductImage);
        }
    }

    private void loadSuppliers() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Aucune connexion Internet", Toast.LENGTH_LONG).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Si nous n'avons pas les infos du produit, chargeons-les d'abord
        if (product == null) {
            loadProductDetails();
        }

        // Appeler l'API pour récupérer les fournisseurs
        apiService.getProductSuppliers(productId, false).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    processSupplierResponse(response.body());
                } else {
                    Toast.makeText(ProductSuppliersActivity.this,
                            "Erreur lors du chargement des fournisseurs", Toast.LENGTH_LONG).show();
                    tvEmptySuppliers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(ProductSuppliersActivity.this,
                        "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();
                tvEmptySuppliers.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadProductDetails() {
        apiService.getProductDetails(productId).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful() && response.body() != null) {
                    product = response.body();
                    displayProductInfo(product);
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                Toast.makeText(ProductSuppliersActivity.this,
                        "Erreur lors du chargement des détails du produit", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processSupplierResponse(Map<String, Object> responseData) {
        suppliers.clear();

        if (responseData.containsKey("suppliers")) {
            Object suppliersObj = responseData.get("suppliers");

            // Utiliser Gson pour convertir l'objet en liste de fournisseurs
            Gson gson = new Gson();
            String jsonString = gson.toJson(suppliersObj);
            Type listType = new TypeToken<List<Supplier>>(){}.getType();
            List<Supplier> supplierList = gson.fromJson(jsonString, listType);

            if (supplierList != null && !supplierList.isEmpty()) {
                suppliers.addAll(supplierList);
                adapter.notifyDataSetChanged();
                tvEmptySuppliers.setVisibility(View.GONE);
            } else {
                tvEmptySuppliers.setVisibility(View.VISIBLE);
            }
        } else {
            tvEmptySuppliers.setVisibility(View.VISIBLE);
        }

        // Si le produit n'était pas déjà chargé et que la réponse contient des infos sur le produit
        if (product == null && responseData.containsKey("product_info")) {
            Object productInfoObj = responseData.get("product_info");
            if (productInfoObj != null) {
                Gson gson = new Gson();
                String jsonString = gson.toJson(productInfoObj);
                Product productFromResponse = gson.fromJson(jsonString, Product.class);

                if (productFromResponse != null) {
                    productFromResponse.setId(productId);
                    product = productFromResponse;
                    displayProductInfo(product);
                }
            }
        }
    }

    @Override
    public void onSupplierClick(Supplier supplier) {
        // Ouvrir les détails du fournisseur
        // Implémenter selon vos besoins
    }

    @Override
    public void onCallSupplier(Supplier supplier) {
        // Appeler le fournisseur
        String phone = supplier.getTelephone();
        if (phone != null && !phone.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Numéro de téléphone non disponible", Toast.LENGTH_SHORT).show();
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
    protected void onResume() {
        super.onResume();

        // Recharger les données si nécessaire
        if (!swipeRefreshLayout.isRefreshing() && suppliers.isEmpty()) {
            loadSuppliers();
        }
    }
}