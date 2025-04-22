package com.drogpulseai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drogpulseai.R;
import com.drogpulseai.adapters.ProductAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Product;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité principale pour la gestion des produits
 */
public class ProductListActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    // UI Components
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddProduct;
    private TextView tvEmptyList;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;

    // Données
    private List<Product> products;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.product_management);
        }

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);

        // Vérifier si l'utilisateur est connecté
        if (!sessionManager.isLoggedIn()) {
            // Rediriger vers la page de connexion
            startActivity(new Intent(ProductListActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Récupérer les données de l'utilisateur
        currentUser = sessionManager.getUser();

        // Initialisation des vues
        initializeViews();

        // Configuration du RecyclerView
        setupRecyclerView();

        // Configuration des listeners
        setupListeners();

        // Chargement des produits
        loadProducts();
    }

    /**
     * Initialisation des vues
     */
    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        fabAddProduct = findViewById(R.id.fab_add_product);
        tvEmptyList = findViewById(R.id.tv_empty_list);
    }

    /**
     * Configuration du RecyclerView
     */
    private void setupRecyclerView() {
        products = new ArrayList<>();
        adapter = new ProductAdapter(this, products, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Configuration des écouteurs d'événements
     */
    private void setupListeners() {
        // Swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadProducts);

        // Bouton d'ajout de produit
        // Bouton d'ajout de produit
        fabAddProduct.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(ProductListActivity.this, ProductFormActivity.class);
                intent.putExtra("mode", "create");
                startActivity(intent);
            } catch (Exception e) {
                // Afficher l'erreur pour diagnostic
                Toast.makeText(ProductListActivity.this,
                        "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace(); // Pour le log
            }
        });
    }

    /**
     * Chargement des produits
     */
    private void loadProducts() {
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        tvEmptyList.setVisibility(View.GONE);

        apiService.getProducts(currentUser.getId()).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    products.clear();
                    products.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    // Afficher un message si aucun produit
                    if (products.isEmpty()) {
                        tvEmptyList.setText(R.string.no_products_found);
                        tvEmptyList.setVisibility(View.VISIBLE);
                    } else {
                        tvEmptyList.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(ProductListActivity.this, "Erreur lors du chargement des produits", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(ProductListActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(ProductListActivity.this, ProductFormActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("product_id", product.getId());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.product_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_search_product) {
            Intent intent = new Intent(ProductListActivity.this, ProductSearchActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rafraîchir les produits à chaque retour à l'activité
        loadProducts();
    }
}