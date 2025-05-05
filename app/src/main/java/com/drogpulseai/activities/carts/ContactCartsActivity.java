package com.drogpulseai.activities.carts;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drogpulseai.R;
import com.drogpulseai.adapters.CartAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.utils.NetworkResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.Gson;

public class ContactCartsActivity extends AppCompatActivity implements CartAdapter.OnCartClickListener {

    private static final String TAG = "ContactCartsActivity";

    // UI Components
    private TextView tvContactInfo;
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvNoData;

    // Utilities
    private ApiService apiService;

    // Données
    private List<Map<String, Object>> carts;
    private int contactId;
    private String contactName;
    private int currentPage = 1;
    private int totalPages = 1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_carts);

        // Récupérer les données de l'intent
        contactId = getIntent().getIntExtra("contact_id", -1);
        contactName = getIntent().getStringExtra("contact_name");

        if (contactId == -1 || contactName == null) {
            Toast.makeText(this, "Données de contact invalides", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Paniers de " + contactName);
        }

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();

        // Initialisation des vues
        initializeViews();

        // Configuration du RecyclerView
        setupRecyclerView();

        // Configuration des listeners
        setupListeners();

        // Chargement des paniers
        loadContactCarts(true);
    }

    private void initializeViews() {
        tvContactInfo = findViewById(R.id.tv_contact_info);
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        tvNoData = findViewById(R.id.tv_no_data);

        // Afficher les infos du contact
        tvContactInfo.setText(contactName);
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
                            loadContactCarts(false);
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
            loadContactCarts(true);
        });
    }
    private void loadContactCarts(boolean refresh) {
        try {
            if (refresh) {
                carts.clear();
                adapter.notifyDataSetChanged();
            }

            if (!swipeRefreshLayout.isRefreshing()) {
                progressBar.setVisibility(View.VISIBLE);
            }

            isLoading = true;

            // Log pour déboguer
            Log.d(TAG, "Début de loadContactCarts - contactId: " + contactId + ", page: " + currentPage);

            // Utiliser Call<Object> pour obtenir la réponse brute
            Call<Object> call = apiService.getContactCartsRaw(contactId, currentPage, 10);
            call.enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    try {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        isLoading = false;

                        Log.d(TAG, "Réponse reçue - code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            // Convertir la réponse en JSON pour l'analyser manuellement
                            Gson gson = new Gson();
                            String jsonStr = gson.toJson(response.body());
                            Log.d(TAG, "Réponse JSON brute: " + jsonStr.substring(0, Math.min(500, jsonStr.length())) + "...");

                            JSONObject jsonResponse = new JSONObject(jsonStr);

                            boolean success = jsonResponse.optBoolean("success", false);
                            Log.d(TAG, "Success: " + success);

                            if (success) {
                                // Traiter les informations du contact si disponibles
                                if (jsonResponse.has("contact")) {
                                    JSONObject contactJson = jsonResponse.getJSONObject("contact");
                                    String fullName = contactJson.optString("full_name", contactName);
                                    Log.d(TAG, "Contact full_name: " + fullName);
                                    if (!fullName.isEmpty()) {
                                        contactName = fullName;
                                        tvContactInfo.setText(contactName);
                                    }
                                }

                                // Vérifier si les paniers sont disponibles
                                if (jsonResponse.has("carts")) {
                                    JSONArray cartsArray = jsonResponse.getJSONArray("carts");
                                    Log.d(TAG, "Nombre de paniers: " + cartsArray.length());

                                    for (int i = 0; i < cartsArray.length(); i++) {
                                        JSONObject cartJson = cartsArray.getJSONObject(i);
                                        Log.d(TAG, "Panier #" + i + ": " + cartJson.toString().substring(0, Math.min(200, cartJson.toString().length())) + "...");

                                        // Convertir en Map de façon sécurisée
                                        try {
                                            Map<String, Object> cartMap = new HashMap<>();

                                            // Extraire les propriétés manuellement
                                            if (cartJson.has("id")) cartMap.put("id", cartJson.getDouble("id"));
                                            if (cartJson.has("user_id")) cartMap.put("user_id", cartJson.getDouble("user_id"));
                                            if (cartJson.has("created_at")) cartMap.put("created_at", cartJson.getString("created_at"));
                                            if (cartJson.has("status")) cartMap.put("status", cartJson.getString("status"));
                                            if (cartJson.has("items_count")) cartMap.put("items_count", cartJson.getDouble("items_count"));
                                            if (cartJson.has("total_quantity")) cartMap.put("total_quantity", cartJson.getString("total_quantity"));
                                            if (cartJson.has("total_amount")) cartMap.put("total_amount", cartJson.getString("total_amount"));

                                            // Convertir les chaînes en nombres si nécessaire
                                            try {
                                                if (cartMap.containsKey("total_quantity") && cartMap.get("total_quantity") instanceof String) {
                                                    cartMap.put("total_quantity", Double.parseDouble((String) cartMap.get("total_quantity")));
                                                }
                                                if (cartMap.containsKey("total_amount") && cartMap.get("total_amount") instanceof String) {
                                                    cartMap.put("total_amount", Double.parseDouble((String) cartMap.get("total_amount")));
                                                }
                                            } catch (NumberFormatException e) {
                                                Log.e(TAG, "Erreur lors de la conversion des nombres", e);
                                            }

                                            // Ajouter à notre liste
                                            carts.add(cartMap);

                                        } catch (Exception e) {
                                            Log.e(TAG, "Erreur lors de la conversion du panier " + i, e);
                                        }
                                    }

                                    // Notifier l'adaptateur en toute sécurité
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            adapter.notifyDataSetChanged();

                                            // Vérifier si la liste est vide
                                            if (currentPage == 1 && carts.isEmpty()) {
                                                handleEmptyCartsList();
                                            } else {
                                                if (tvNoData != null) {
                                                    tvNoData.setVisibility(View.GONE);
                                                }
                                            }
                                        }
                                    });
                                } else {
                                    Log.d(TAG, "Pas de clé 'carts' dans la réponse");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            handleEmptyCartsList();
                                        }
                                    });
                                }

                                // Traiter la pagination
                                if (jsonResponse.has("pagination")) {
                                    JSONObject pagination = jsonResponse.getJSONObject("pagination");
                                    currentPage = pagination.optInt("current_page", currentPage);
                                    totalPages = pagination.optInt("total_pages", totalPages);
                                    Log.d(TAG, "Pagination: page " + currentPage + " sur " + totalPages);
                                }

                            } else {
                                // La réponse indique un échec
                                final String message = jsonResponse.optString("message", "Erreur lors du chargement des paniers");
                                Log.e(TAG, "Échec de la requête: " + message);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        handleApiError(message);
                                    }
                                });
                            }
                        } else {
                            // Erreur HTTP
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "Erreur inconnue";
                            Log.e(TAG, "Erreur HTTP: " + response.code() + " - " + errorBody);
                            final String errorMessage = "Erreur serveur: " + response.code();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    handleApiError(errorMessage);
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Exception dans onResponse", e);
                        final String errorMessage = "Erreur lors du traitement de la réponse: " + e.getMessage();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handleApiError(errorMessage);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    try {
                        progressBar.setVisibility(View.GONE);
                        swipeRefreshLayout.setRefreshing(false);
                        isLoading = false;

                        Log.e(TAG, "Erreur réseau", t);
                        final String errorMessage = "Erreur réseau: " + t.getMessage();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                handleApiError(errorMessage);
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Exception dans onFailure", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception dans loadContactCarts", e);
            Toast.makeText(this, "Erreur inattendue: " + e.getMessage(), Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            isLoading = false;
        }
    }
    // Déplacer cette méthode pour éviter la référence à "this" dans un callback
    private void handleEmptyCartsList() {
        // Vérifier si les vues existent
        if (tvNoData != null) {
            tvNoData.setVisibility(View.VISIBLE);
            tvNoData.setText("Aucun panier trouvé pour " + contactName);
        }

        View emptyView = findViewById(R.id.empty_view);
        if (emptyView != null) {
            emptyView.setVisibility(View.VISIBLE);

            Button btnCreateCart = findViewById(R.id.btn_create_cart);
            if (btnCreateCart != null) {
                btnCreateCart.setOnClickListener(v -> {
                    Intent intent = new Intent(ContactCartsActivity.this, CartActivity.class);
                    intent.putExtra("contact_id", contactId);
                    intent.putExtra("contact_name", contactName);
                    startActivity(intent);
                    finish();
                });
            }

            Button btnBackToContacts = findViewById(R.id.btn_back_to_contacts);
            if (btnBackToContacts != null) {
                btnBackToContacts.setOnClickListener(v -> finish());
            }
        }
    }
    /**
     * Gérer les erreurs d'API
     */
    private void handleApiError(String errorMessage) {
        // Afficher un dialogue d'erreur avec option de retour
        new AlertDialog.Builder(this)
                .setTitle("Erreur")
                .setMessage(errorMessage + "\n\nVoulez-vous retourner à la liste des contacts ?")
                .setPositiveButton("Oui", (dialog, which) -> finish())
                .setNegativeButton("Réessayer", (dialog, which) -> {
                    currentPage = 1;
                    loadContactCarts(true);
                })
                .setCancelable(false)
                .show();
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