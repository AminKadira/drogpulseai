package com.drogpulseai.activities;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_details);

        // Récupérer l'ID du panier depuis l'intent
        cartId = getIntent().getIntExtra("cart_id", -1);

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

        apiService.getCart(cartId).enqueue(new Callback<NetworkResult<Cart>>() {
            @Override
            public void onResponse(Call<NetworkResult<Cart>> call, Response<NetworkResult<Cart>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    cart = response.body().getData();
                    displayCartDetails();
                } else {
                    // Gérer les erreurs
                    String errorMessage = "Erreur lors du chargement des détails du panier";

                    if (response.body() != null) {
                        errorMessage = response.body().getMessage();
                    }

                    Toast.makeText(CartDetailsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<NetworkResult<Cart>> call, Throwable t) {
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
                "%d produit(s), %d article(s), %.2f €",
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
                break;
            case "confirmed":
                tvStatus.setText("Confirmé");
                tvStatus.setBackgroundResource(R.drawable.bg_status_confirmed);
                break;
            case "cancelled":
                tvStatus.setText("Annulé");
                tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled);
                break;
            default:
                tvStatus.setText(cart.getStatus());
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                break;
        }
    }

    private void showConfirmationDialog(String action) {
        String message = action.equals("confirmer") ?
                "Êtes-vous sûr de vouloir confirmer ce panier ?" :
                "Êtes-vous sûr de vouloir annuler ce panier ?";

        new AlertDialog.Builder(this)
                .setTitle("Confirmation")
                .setMessage(message)
                .setPositiveButton("Oui", (dialog, which) -> updateCartStatus(action))
                .setNegativeButton("Non", null)
                .show();
    }

    private void updateCartStatus(String action) {
        String newStatus = action.equals("confirmer") ? "confirmed" : "cancelled";

        Map<String, Object> statusData = new HashMap<>();
        statusData.put("cart_id", cartId);
        statusData.put("status", newStatus);

        progressBar.setVisibility(View.VISIBLE);

        apiService.updateCartStatus(statusData).enqueue(new Callback<NetworkResult<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<NetworkResult<Map<String, Object>>> call, Response<NetworkResult<Map<String, Object>>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Mettre à jour le statut dans l'objet panier
                    cart.setStatus(newStatus);

                    // Mettre à jour l'affichage
                    updateStatusDisplay();
                    updateButtons();

                    // Message de confirmation
                    String message = action.equals("confirmer") ?
                            "Panier confirmé avec succès" :
                            "Panier annulé avec succès";

                    Toast.makeText(CartDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                } else {
                    // Gérer les erreurs
                    String errorMessage = "Erreur lors de la mise à jour du statut";

                    if (response.body() != null) {
                        errorMessage = response.body().getMessage();
                    }

                    Toast.makeText(CartDetailsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<NetworkResult<Map<String, Object>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Erreur réseau", t);
                Toast.makeText(CartDetailsActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();
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