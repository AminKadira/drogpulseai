package com.drogpulseai.activities.carts;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.drogpulseai.adapters.CartAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.NetworkResult;
import com.drogpulseai.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartsListActivity extends AppCompatActivity implements CartAdapter.OnCartClickListener {

    private static final String TAG = "CartsListActivity";

    // UI Components
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvNoData;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;

    // Données
    private List<Map<String, Object>> carts;
    private User currentUser;
    private int currentPage = 1;
    private int totalPages = 1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carts_list);

        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mes paniers");
        }

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);

        // Récupérer l'utilisateur courant
        currentUser = sessionManager.getUser();

        if (currentUser == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialisation des vues
        initializeViews();

        // Configuration du RecyclerView
        setupRecyclerView();

        // Configuration des listeners
        setupListeners();

        // Chargement des paniers
        loadCarts(true);
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        tvNoData = findViewById(R.id.tv_no_data);
    }

    private void setupRecyclerView() {
        carts = new ArrayList<>();
        adapter = new CartAdapter(this, carts, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Ajouter la pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && currentPage < totalPages) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0) {
                            // Charger la page suivante
                            currentPage++;
                            loadCarts(false);
                        }
                    }
                }
            }
        });
    }

    private void setupListeners() {
        // Swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentPage = 1;
            loadCarts(true);
        });
    }

    private void loadCarts(boolean refresh) {
        if (refresh) {
            carts.clear();
            adapter.notifyDataSetChanged();
        }

        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        isLoading = true;

        apiService.getUserCarts(currentUser.getId(), currentPage, 10).enqueue(new Callback<NetworkResult<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<NetworkResult<Map<String, Object>>> call, Response<NetworkResult<Map<String, Object>>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                isLoading = false;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Map<String, Object> data = response.body().getData();

                    // Récupérer les paniers
                    List<Map<String, Object>> pageCarts = (List<Map<String, Object>>) data.get("carts");

                    if (pageCarts != null) {
                        carts.addAll(pageCarts);
                        adapter.notifyDataSetChanged();
                    }

                    // Récupérer les informations de pagination
                    Map<String, Object> pagination = (Map<String, Object>) data.get("pagination");
                    if (pagination != null) {
                        currentPage = ((Double) pagination.get("current_page")).intValue();
                        totalPages = ((Double) pagination.get("total_pages")).intValue();
                    }

                    // Afficher un message si aucun panier
                    if (carts.isEmpty()) {
                        tvNoData.setVisibility(View.VISIBLE);
                    } else {
                        tvNoData.setVisibility(View.GONE);
                    }
                } else {
                    // Gérer les erreurs
                    String errorMessage = "Erreur lors du chargement des paniers";

                    if (response.body() != null) {
                        errorMessage = response.body().getMessage();
                    }

                    Toast.makeText(CartsListActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<NetworkResult<Map<String, Object>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                isLoading = false;

                Log.e(TAG, "Erreur réseau", t);
                Toast.makeText(CartsListActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onCartClick(Map<String, Object> cart) {
        // Ouvrir les détails du panier
        Intent intent = new Intent(this, CartDetailsActivity.class);
        intent.putExtra("cart_id", ((Double) cart.get("id")).intValue());
        startActivity(intent);
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