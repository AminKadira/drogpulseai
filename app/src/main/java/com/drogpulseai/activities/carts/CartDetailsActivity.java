package com.drogpulseai.activities.carts;

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

import com.drogpulseai.R;
import com.drogpulseai.adapters.CartItemAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Cart;
import com.drogpulseai.models.CartItem;
import com.drogpulseai.utils.NetworkResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.Gson;

public class CartDetailsActivity extends AppCompatActivity {

    private static final String TAG = "CartDetailsActivity";

    // UI Components
    private TextView tvCartInfo, tvContactInfo, tvStatus, tvTotal;
    private RecyclerView recyclerView;
    private CartItemAdapter adapter;
    private ProgressBar progressBar;
    private Button btnConfirm, btnCancel;

    // Utilities
    private ApiService apiService;

    // Données
    private int cartId;
    private Cart cart;
    private String cancellationReason = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_details);

        // Récupérer l'ID du panier depuis l'intent
        cartId = getIntent().getIntExtra("cart_id", -1);
        Log.d(TAG, "CartDetailsActivity - cart_id reçu: " + cartId);

        if (cartId == -1) {
            Toast.makeText(this, "ID de panier invalide", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Détails du panier #" + cartId);
        }

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();

        // Initialisation des vues
        initializeViews();

        // Configuration du RecyclerView
        setupRecyclerView();

        // Configuration des listeners
        setupListeners();

        // Chargement des détails du panier
        loadCartDetails();
    }

    private void initializeViews() {
        tvCartInfo = findViewById(R.id.tv_cart_info);
        tvContactInfo = findViewById(R.id.tv_contact_info);
        tvStatus = findViewById(R.id.tv_status);
        tvTotal = findViewById(R.id.tv_total);
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        btnConfirm = findViewById(R.id.btn_confirm);
        btnCancel = findViewById(R.id.btn_cancel);
    }

    private void setupRecyclerView() {
        adapter = new CartItemAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Bouton Confirmer
        btnConfirm.setOnClickListener(v -> {
            if (cart != null && cart.isPending()) {
                showConfirmationDialog("confirmer");
            }
        });

        // Bouton Annuler
        btnCancel.setOnClickListener(v -> {
            if (cart != null && !cart.isCancelled()) {
                showConfirmationDialog("annuler");
            }
        });
    }

    private void loadCartDetails() {
        progressBar.setVisibility(View.VISIBLE);

        // Utiliser un type générique pour la réponse
        Call<Object> call = apiService.getCartRaw(cartId);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Convertir la réponse brute en chaîne JSON
                        Gson gson = new Gson();
                        String jsonStr = gson.toJson(response.body());
                        Log.d(TAG, "Réponse JSON brute: " + jsonStr);

                        // Analyser la réponse JSON manuellement
                        JSONObject jsonResponse = new JSONObject(jsonStr);

                        // Vérifier si la réponse contient un champ "success"
                        if (jsonResponse.has("success") && jsonResponse.getBoolean("success")) {
                            // Extraire les données du panier
                            JSONObject cartData;

                            // Vérifier si les données sont sous "data" puis "cart" ou directement sous "cart"
                            if (jsonResponse.has("data") && jsonResponse.getJSONObject("data").has("cart")) {
                                cartData = jsonResponse.getJSONObject("data").getJSONObject("cart");
                            } else if (jsonResponse.has("cart")) {
                                cartData = jsonResponse.getJSONObject("cart");
                            } else {
                                Toast.makeText(CartDetailsActivity.this, "Structure de données inattendue", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Créer un nouvel objet Cart à partir des données JSON
                            Cart cartObj = new Cart();

                            // Remplir les propriétés de base
                            if (cartData.has("id")) cartObj.setId(cartData.getInt("id"));
                            if (cartData.has("contact_id")) cartObj.setContactId(cartData.getInt("contact_id"));
                            if (cartData.has("user_id")) cartObj.setUserId(cartData.getInt("user_id"));
                            if (cartData.has("status")) cartObj.setStatus(cartData.getString("status"));
                            if (cartData.has("notes")) cartObj.setNotes(cartData.optString("notes", null));
                            if (cartData.has("created_at")) cartObj.setCreatedAt(cartData.getString("created_at"));
                            if (cartData.has("updated_at")) cartObj.setUpdatedAt(cartData.getString("updated_at"));

                            // Infos contact
                            if (cartData.has("contact_nom")) cartObj.setContactNom(cartData.getString("contact_nom"));
                            if (cartData.has("contact_prenom")) cartObj.setContactPrenom(cartData.getString("contact_prenom"));
                            if (cartData.has("contact_telephone")) cartObj.setContactTelephone(cartData.getString("contact_telephone"));
                            if (cartData.has("contact_email")) cartObj.setContactEmail(cartData.optString("contact_email", null));

                            // Totaux
                            if (cartData.has("total_quantity")) cartObj.setTotalQuantity(cartData.getInt("total_quantity"));
                            if (cartData.has("total_amount")) cartObj.setTotalAmount(cartData.getDouble("total_amount"));

                            // Articles
                            if (cartData.has("items") && !cartData.isNull("items")) {
                                JSONArray itemsArray = cartData.getJSONArray("items");
                                List<CartItem> items = new ArrayList<>();

                                for (int i = 0; i < itemsArray.length(); i++) {
                                    JSONObject itemJson = itemsArray.getJSONObject(i);
                                    CartItem item = new CartItem();

                                    if (itemJson.has("id")) item.setId(itemJson.getInt("id"));
                                    if (itemJson.has("cart_id")) item.setCartId(itemJson.getInt("cart_id"));
                                    if (itemJson.has("product_id")) item.setProductId(itemJson.getInt("product_id"));
                                    if (itemJson.has("quantity")) item.setQuantity(itemJson.getInt("quantity"));
                                    if (itemJson.has("price")) item.setPrice(itemJson.getDouble("price"));
                                    if (itemJson.has("product_reference")) item.setProductReference(itemJson.getString("product_reference"));
                                    if (itemJson.has("product_name")) item.setProductName(itemJson.getString("product_name"));
                                    if (itemJson.has("product_label")) item.setProductLabel(itemJson.getString("product_label"));

                                    items.add(item);
                                }

                                cartObj.setItems(items);
                            }

                            // Utiliser l'objet Cart créé
                            cart = cartObj;

                            // Afficher les détails
                            displayCartDetails();
                        } else {
                            // Erreur dans la réponse
                            String message = jsonResponse.optString("message", "Erreur lors du chargement des détails du panier");
                            Toast.makeText(CartDetailsActivity.this, message, Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors de l'analyse de la réponse JSON", e);
                        Toast.makeText(CartDetailsActivity.this, "Erreur lors de l'analyse des données: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else {
                    // Erreur HTTP
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Erreur inconnue";
                        Log.e(TAG, "Erreur HTTP: " + response.code() + " - " + errorBody);
                        Toast.makeText(CartDetailsActivity.this, "Erreur serveur: " + response.code(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors de la lecture de l'erreur", e);
                    }
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Erreur réseau", t);
                Toast.makeText(CartDetailsActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void displayCartDetails() {
        if (cart == null) return;

        // Format de la date
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        try {
            Date createdDate = inputFormat.parse(cart.getCreatedAt());
            String formattedDate = outputFormat.format(createdDate);
            tvCartInfo.setText(String.format("Panier #%d - %s", cart.getId(), formattedDate));
        } catch (Exception e) {
            tvCartInfo.setText(String.format("Panier #%d", cart.getId()));
        }

        // Infos contact
        tvContactInfo.setText(String.format("%s - %s",
                cart.getContactFullName(),
                cart.getContactTelephone()));

        // Statut
        updateStatusDisplay();

        // Total
        tvTotal.setText(String.format(Locale.getDefault(),
                "%d produit(s), %d article(s), %.2f MAD",
                cart.getItems().size(),
                cart.getTotalQuantity(),
                cart.getTotalAmount()));

        // Mise à jour de la liste des articles
        adapter.setItems(cart.getItems());

        // Mise à jour des boutons selon le statut
        updateButtons();
    }

    private void updateButtons() {
        if (cart == null) return;

        if (cart.isPending()) {
            btnConfirm.setVisibility(View.VISIBLE);
            btnCancel.setVisibility(View.VISIBLE);
        } else if (cart.isConfirmed()) {
            btnConfirm.setVisibility(View.GONE);
            btnCancel.setVisibility(View.VISIBLE);
        } else {
            btnConfirm.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
        }
    }

    private void updateStatusDisplay() {
        switch (cart.getStatus()) {
            case "pending":
                tvStatus.setText("En attente");
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvStatus.setBackgroundColor(R.drawable.bg_status_pending);

                break;
            case "confirmed":
                tvStatus.setText("Confirmé");
                tvStatus.setBackgroundResource(R.drawable.bg_status_confirmed);
                tvStatus.setBackgroundColor(R.drawable.bg_status_confirmed);
                break;
            case "cancelled":
                tvStatus.setText("Annulé");
                tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled);
                tvStatus.setBackgroundColor(R.drawable.bg_status_cancelled);
                break;
            default:
                tvStatus.setText(cart.getStatus());
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                break;
        }
    }

    /**
     * Affiche une boîte de dialogue de confirmation adaptée pour l'annulation ou confirmation de panier
     * Inclut une sélection de motif pour l'annulation
     */
    private void showConfirmationDialog(String action) {
        if (action.equals("confirmer")) {
            // Dialogue simple pour la confirmation
            new AlertDialog.Builder(this)
                    .setTitle("Confirmation")
                    .setMessage("Êtes-vous sûr de vouloir confirmer ce panier ?")
                    .setPositiveButton("Oui", (dialog, which) -> updateCartStatus(action))
                    .setNegativeButton("Non", null)
                    .show();
        } else {
            // Pour l'annulation, afficher les options de motif
            final String[] cancellationReasons = {
                    getString(R.string.annulation_cart_client),
                    getString(R.string.annulation_cart_erreur),
                    getString(R.string.annulation_cart_autre)
            };

            final int[] selectedReason = {0};  // Par défaut, premier élément

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Annulation de panier")
                    .setSingleChoiceItems(cancellationReasons, selectedReason[0], (dialog, which) -> {
                        selectedReason[0] = which;
                    })
                    .setPositiveButton("Confirmer", (dialog, which) -> {
                        cancellationReason = cancellationReasons[selectedReason[0]];
                        Log.d(TAG, "Motif d'annulation sélectionné : " + cancellationReason);
                        updateCartStatus(action);
                    })
                    .setNegativeButton("Annuler", null);

            // Important : ne pas appeler .setMessage() si tu utilises setSingleChoiceItems()
            // car ça masque la liste
            builder.create().show();
        }
    }

    /**
     * Update a cart's status with proper error handling for 500 errors
     */
    private void updateCartStatus(String action) {
        String newStatus = action.equals("confirmer") ? "confirmed" : "cancelled";

        Map<String, Object> statusData = new HashMap<>();
        statusData.put("cart_id", cartId);
        statusData.put("status", newStatus);

        // Only include notes if there's actually a value
        if (cancellationReason != null && !cancellationReason.isEmpty()) {
            statusData.put("notes", cancellationReason);
        }

        progressBar.setVisibility(View.VISIBLE);

        // Convert to JSON for better logging
        String requestJson = new Gson().toJson(statusData);
        Log.d(TAG, "Sending update request JSON: " + requestJson);

        apiService.updateCartStatus(statusData).enqueue(new Callback<NetworkResult<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<NetworkResult<Map<String, Object>>> call, Response<NetworkResult<Map<String, Object>>> response) {
                progressBar.setVisibility(View.GONE);

                // Log the entire response information for debugging
                Log.d(TAG, "Response: " + response.toString());
                Log.d(TAG, "Status code: " + response.code() + ", Message: " + response.message());

                try {
                    if (response.errorBody() != null) {
                        String errorBodyStr = response.errorBody().string();
                        Log.e(TAG, "Error body: " + errorBodyStr);

                        // Try to parse JSON error if possible
                        try {
                            JSONObject errorJson = new JSONObject(errorBodyStr);
                            String errorMessage = errorJson.optString("message", "Unknown error");
                            Log.e(TAG, "Parsed error message: " + errorMessage);

                            Toast.makeText(CartDetailsActivity.this,
                                    "Erreur: " + errorMessage, Toast.LENGTH_LONG).show();
                            return;
                        } catch (Exception e) {
                            // Not a valid JSON, just show the raw error
                            Log.e(TAG, "Error parsing JSON error response", e);
                        }
                    }

                    // Process successful responses
                    if (response.isSuccessful() && response.body() != null) {
                        NetworkResult<Map<String, Object>> result = response.body();
                        Log.d(TAG, "Response body: " + new Gson().toJson(result));

                        if (result.isSuccess()) {
                            // Update cart status locally
                            cart.setStatus(newStatus);
                            if (action.equals("annuler") && !cancellationReason.isEmpty()) {
                                cart.setNotes(cancellationReason);
                            }

                            // Update UI
                            updateStatusDisplay();
                            updateButtons();

                            String message = action.equals("confirmer") ?
                                    "Panier confirmé avec succès" :
                                    "Panier annulé avec succès";

                            Toast.makeText(CartDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                        } else {
                            // API logical error
                            String errorMessage = result.getMessage() != null ?
                                    result.getMessage() : "Erreur inconnue";

                            Toast.makeText(CartDetailsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // HTTP error with no parseable body
                        Toast.makeText(CartDetailsActivity.this,
                                "Erreur HTTP " + response.code() + ": Impossible de mettre à jour le statut",
                                Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception processing response", e);
                    Toast.makeText(CartDetailsActivity.this,
                            "Erreur de traitement: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                } finally {
                    cancellationReason = "";
                }
            }

            @Override
            public void onFailure(Call<NetworkResult<Map<String, Object>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);

                // Log the detailed failure
                Log.e(TAG, "Network failure", t);

                if (t instanceof java.net.SocketTimeoutException) {
                    Toast.makeText(CartDetailsActivity.this,
                            "Délai d'attente dépassé. Veuillez réessayer.",
                            Toast.LENGTH_LONG).show();
                } else if (t instanceof java.net.UnknownHostException) {
                    Toast.makeText(CartDetailsActivity.this,
                            "Serveur introuvable. Vérifiez votre connexion.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(CartDetailsActivity.this,
                            "Erreur réseau: " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                }

                cancellationReason = "";
            }
        });
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