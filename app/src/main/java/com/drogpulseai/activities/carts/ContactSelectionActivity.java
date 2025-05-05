package com.drogpulseai.activities.carts;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drogpulseai.R;
import com.drogpulseai.adapters.ContactSelectionAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Contact;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ContactSelectionActivity extends AppCompatActivity implements ContactSelectionAdapter.OnContactClickListener {

    // UI Components
    private RecyclerView recyclerView;
    private ContactSelectionAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private Button btnCreateCart;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;

    // Données
    private List<Contact> contacts;
    private User currentUser;
    private Contact selectedContact = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_selection);

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);

        // Récupérer les données de l'utilisateur
        currentUser = sessionManager.getUser();

        // Initialisation des vues
        initializeViews();

        // Configuration du RecyclerView
        setupRecyclerView();

        // Configuration des listeners
        setupListeners();

        // Chargement des contacts
        loadContacts();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        btnCreateCart = findViewById(R.id.btn_create_cart);

        // Désactiver le bouton tant qu'aucun contact n'est sélectionné
        btnCreateCart.setEnabled(false);
    }

    private void setupRecyclerView() {
        contacts = new ArrayList<>();
        adapter = new ContactSelectionAdapter(this, contacts, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadContacts);

        // Bouton "Créer un panier"
        btnCreateCart.setOnClickListener(v -> {
            if (selectedContact != null) {
                Intent intent = new Intent(ContactSelectionActivity.this, CartActivity.class);
                intent.putExtra("contact_id", selectedContact.getId());
                intent.putExtra("contact_name", selectedContact.getFullName());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Veuillez sélectionner un contact", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadContacts() {
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        apiService.getContacts(currentUser.getId()).enqueue(new Callback<List<Contact>>() {
            @Override
            public void onResponse(Call<List<Contact>> call, Response<List<Contact>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    contacts.clear();
                    contacts.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    // Afficher un message si aucun contact
                    if (contacts.isEmpty()) {
                        Toast.makeText(ContactSelectionActivity.this, "Aucun contact trouvé", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ContactSelectionActivity.this, "Erreur lors du chargement des contacts", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Contact>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(ContactSelectionActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onContactClick(Contact contact, boolean isSelected) {
        if (isSelected) {
            selectedContact = contact;
            btnCreateCart.setEnabled(true);
        } else {
            // Si le contact déselectionné était le contact sélectionné
            if (selectedContact != null && selectedContact.getId() == contact.getId()) {
                selectedContact = null;
                btnCreateCart.setEnabled(false);
            }
        }
    }
}