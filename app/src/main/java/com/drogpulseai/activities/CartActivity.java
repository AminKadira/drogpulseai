package com.drogpulseai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.drogpulseai.adapters.CartProductAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Cart;
import com.drogpulseai.models.Product;
import com.drogpulseai.models.ProductCartItem;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.NetworkResult;
import com.drogpulseai.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.app.ProgressDialog;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class CartActivity extends AppCompatActivity implements CartProductAdapter.OnProductSelectionChangeListener {

    private static final String TAG = "CartActivity";

    // UI Components
    private TextView tvContactInfo;
    private TextView tvSelectionCount;
    private RecyclerView recyclerView;
    private CartProductAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvNoProducts;
    private Button btnSelectAll;
    private Button btnClearSelection;
    private Button btnAddToCart;
    private EditText etSearchProducts;
    private ImageButton btnClearSearch;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;

    // Données
    private List<Product> products;
    private User currentUser;
    private int contactId;
    private String contactName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        Log.d(TAG, "Création de CartActivity");

        // Configurer la barre d'action pour le retour
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Panier");
        }

        // Récupérer les données de l'intent
        contactId = getIntent().getIntExtra("contact_id", -1);
        contactName = getIntent().getStringExtra("contact_name");

        Log.d(TAG, "Contact ID: " + contactId + ", Nom: " + contactName);

        if (contactId == -1 || contactName == null) {
            Toast.makeText(this, "Données de contact invalides", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);

        // Récupérer l'utilisateur courant
        currentUser = sessionManager.getUser();

        if (currentUser == null) {
            Log.e(TAG, "Utilisateur non connecté");
            Toast.makeText(this, "Erreur: Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Utilisateur ID: " + currentUser.getId());

        // Initialisation des vues
        initializeViews();

        // Configuration du RecyclerView
        setupRecyclerView();

        // Configuration des listeners
        setupListeners();

        // Chargement des produits
        loadProducts();
    }

    private void initializeViews() {
        tvContactInfo = findViewById(R.id.tv_contact_info);
        tvSelectionCount = findViewById(R.id.tv_selection_count);
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        tvNoProducts = findViewById(R.id.tv_no_products);
        btnSelectAll = findViewById(R.id.btn_select_all);
        btnClearSelection = findViewById(R.id.btn_clear_selection);
        btnAddToCart = findViewById(R.id.btn_add_to_cart);
        etSearchProducts = findViewById(R.id.et_search_products);
        btnClearSearch = findViewById(R.id.btn_clear_search);

        // Afficher les informations du contact
        tvContactInfo.setText("Contact: " + contactName + " (ID: " + contactId + ")");
        tvSelectionCount.setText("Sélectionné(s): 0 (0 articles)");

        // S'assurer que le champ de recherche est vide
        etSearchProducts.setText("");
        btnClearSearch.setVisibility(View.GONE);

        Log.d(TAG, "Vues initialisées");
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Configuration du RecyclerView");
        products = new ArrayList<>();
        adapter = new CartProductAdapter(this, products, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        Log.d(TAG, "Configuration des listeners");

        // Swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Rafraîchissement demandé");
            loadProducts();
        });

        // Bouton Tout sélectionner
        btnSelectAll.setOnClickListener(v -> {
            Log.d(TAG, "Tout sélectionner");
            adapter.selectAll();
        });

        // Bouton Effacer la sélection
        btnClearSelection.setOnClickListener(v -> {
            Log.d(TAG, "Effacer sélection");
            adapter.clearSelection();
        });

        // Bouton Ajouter au panier
        btnAddToCart.setOnClickListener(v -> {
            List<ProductCartItem> selectedItems = adapter.getSelectedProductItems();
            Log.d(TAG, "Ajouter au panier - " + selectedItems.size() + " produits sélectionnés");

            if (!selectedItems.isEmpty()) {
                createCart(selectedItems);
            } else {
                Toast.makeText(this, "Veuillez sélectionner au moins un produit", Toast.LENGTH_SHORT).show();
            }
        });

        // Recherche de produits
        etSearchProducts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Non utilisé
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "Recherche: " + s);
                adapter.filter(s.toString());

                // Afficher ou masquer le bouton de suppression
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Non utilisé
            }
        });

        // Recherche lorsque l'utilisateur appuie sur "Rechercher" sur le clavier
        etSearchProducts.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Log.d(TAG, "Action recherche depuis le clavier");
                // Masquer le clavier
                v.clearFocus();
                return true;
            }
            return false;
        });

        // Bouton pour effacer la recherche
        btnClearSearch.setOnClickListener(v -> {
            Log.d(TAG, "Effacer la recherche");
            etSearchProducts.setText("");
            adapter.filter("");
            btnClearSearch.setVisibility(View.GONE);
        });
    }

    private void loadProducts() {
        Log.d(TAG, "Chargement des produits pour l'utilisateur ID: " + currentUser.getId());

        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        tvNoProducts.setVisibility(View.GONE);

        apiService.getProducts(currentUser.getId()).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Product> loadedProducts = response.body();
                    Log.d(TAG, "Produits chargés: " + loadedProducts.size());

                    // Créer quelques produits factices pour tester si le problème vient de l'API
                    if (loadedProducts.isEmpty()) {
                        Log.d(TAG, "Création de produits factices pour tester");
                        loadedProducts = createDummyProducts();
                    }

                    products.clear();
                    products.addAll(loadedProducts);

                    // Mettre à jour l'adaptateur avec les nouveaux produits
                    adapter.updateProducts(products);

                    // S'assurer que tous les produits sont affichés par défaut
                    adapter.showAllProducts();

                    // Afficher l'état actuel de l'adaptateur
                    Log.d(TAG, "Nombre d'éléments dans l'adaptateur: " + adapter.getItemCount());

                    // Afficher un message si aucun produit
                    if (products.isEmpty()) {
                        tvNoProducts.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Aucun produit trouvé");
                    } else {
                        tvNoProducts.setVisibility(View.GONE);
                    }

                    // Forcer le rafraîchissement de la vue
                    recyclerView.post(() -> adapter.notifyDataSetChanged());
                } else {
                    Log.e(TAG, "Erreur lors du chargement des produits: " +
                            (response.errorBody() != null ? response.errorBody().toString() : "Inconnu") +
                            " - Code: " + response.code());
                    Toast.makeText(CartActivity.this, "Erreur lors du chargement des produits", Toast.LENGTH_LONG).show();

                    // Charger des produits factices pour tester l'affichage
                    List<Product> dummyProducts = createDummyProducts();
                    adapter.updateProducts(dummyProducts);
                    adapter.showAllProducts();
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "Échec du chargement des produits: " + t.getMessage(), t);
                Toast.makeText(CartActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();

                // Charger des produits factices pour tester l'affichage
                List<Product> dummyProducts = createDummyProducts();
                adapter.updateProducts(dummyProducts);
                adapter.showAllProducts();
            }
        });
    }

    /**
     * Créer des produits factices pour tester l'affichage
     */
    private List<Product> createDummyProducts() {
        List<Product> dummyProducts = new ArrayList<>();

        // Créer 5 produits factices
        for (int i = 1; i <= 5; i++) {
            Product product = new Product();
            product.setId(i);
            product.setReference("REF-" + i);
            product.setName("Produit test " + i);
            product.setDescription("Description du produit " + i);
            product.setQuantity(10 * i);
            product.setPrice(9.99 * i);
            product.setUserId(currentUser.getId());
            dummyProducts.add(product);
        }

        Log.d(TAG, "Produits factices créés: " + dummyProducts.size());
        return dummyProducts;
    }

    /**
     * Méthode pour créer un panier avec les produits sélectionnés
     */
    private void createCart(List<ProductCartItem> selectedItems) {
        Log.d(TAG, "Création du panier avec " + selectedItems.size() + " produits");

        // Afficher un dialogue de progression
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Création du panier en cours...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Préparer les données pour l'API
        Map<String, Object> cartData = new HashMap<>();
        cartData.put("contact_id", contactId);
        cartData.put("user_id", currentUser.getId());
        cartData.put("notes", "Panier créé depuis l'application mobile");

        // Préparer les articles du panier
        List<Map<String, Object>> items = new ArrayList<>();

        for (ProductCartItem item : selectedItems) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("product_id", item.getProduct().getId());
            itemData.put("quantity", item.getQuantity());
            itemData.put("price", item.getProduct().getPrice());
            items.add(itemData);
        }

        cartData.put("items", items);

        // Appel à l'API pour créer le panier
        apiService.createCart(cartData).enqueue(new Callback<NetworkResult<Cart>>() {
            @Override
            public void onResponse(Call<NetworkResult<Cart>> call, Response<NetworkResult<Cart>> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Récupérer le panier créé
                    Cart cart = response.body().getData();

                    // Afficher un message de succès
                    String message = String.format(
                            "Panier #%d créé avec succès!\n" +
                                    "Contact: %s\n" +
                                    "Produits: %d (total de %d articles)\n" +
                                    "Montant total: %.2f €",
                            cart.getId(),
                            cart.getContactFullName(),
                            cart.getItems().size(),
                            cart.getTotalQuantity(),
                            cart.getTotalAmount()
                    );

                    showSuccessDialog("Panier créé", message);

                    // Vider la sélection après création réussie
                    adapter.clearSelection();
                } else {
                    // Gérer les erreurs
                    String errorMessage = "Erreur lors de la création du panier";

                    if (response.body() != null) {
                        errorMessage = response.body().getMessage();
                    } else if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            JSONObject jsonError = new JSONObject(errorBody);
                            errorMessage = jsonError.optString("message", errorMessage);
                        } catch (Exception e) {
                            Log.e(TAG, "Erreur lors du parsing de l'erreur", e);
                        }
                    }

                    Toast.makeText(CartActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<NetworkResult<Cart>> call, Throwable t) {
                progressDialog.dismiss();
                Log.e(TAG, "Erreur réseau lors de la création du panier", t);
                Toast.makeText(CartActivity.this, "Erreur réseau: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Affiche un dialogue de succès avec option de voir les détails du panier
     */
    private void showSuccessDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .setNeutralButton("Voir mes paniers", (dialog, which) -> {
                    // Lancer l'activité de liste des paniers
                    Intent intent = new Intent(CartActivity.this, CartsListActivity.class);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Log.d(TAG, "Bouton retour pressé");
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSelectionChanged(int count, int totalItems) {
        Log.d(TAG, "Sélection changée: " + count + " produits, " + totalItems + " articles");
        tvSelectionCount.setText("Sélectionné(s): " + count + " (" + totalItems + " articles)");
        btnAddToCart.setEnabled(count > 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        // Vérifier l'état de l'adaptateur au moment de la reprise
        if (adapter != null) {
            Log.d(TAG, "Nombre d'éléments dans l'adaptateur: " + adapter.getItemCount());
        }
    }
}