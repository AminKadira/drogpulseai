package com.drogpulseai.activities;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drogpulseai.R;
import com.drogpulseai.activities.appuser.LoginActivity;
import com.drogpulseai.activities.contacts.ContactFormActivity;
import com.drogpulseai.activities.contacts.ContactSearchActivity;
import com.drogpulseai.activities.products.ProductListActivity;
import com.drogpulseai.adapters.ContactAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Contact;
import com.drogpulseai.models.User;
import com.drogpulseai.sync.SyncManager;
import com.drogpulseai.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité principale avec la liste des contacts
 */
public class MainActivity extends AppCompatActivity implements ContactAdapter.OnContactClickListener {

    // UI Components
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddContact;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;

    // Données
    private List<Contact> contacts;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialiser le SyncManager
        SyncManager.getInstance((Application) getApplicationContext());

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);

        // Vérifier si l'utilisateur est connecté
        if (!sessionManager.isLoggedIn()) {
            // Rediriger vers la page de connexion
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
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

        // Chargement des contacts
        loadContacts();
    }

    /**
     * Initialisation des vues
     */
    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        fabAddContact = findViewById(R.id.fab_add_contact);
    }

    /**
     * Configuration du RecyclerView
     */
    private void setupRecyclerView() {
        contacts = new ArrayList<>();
        adapter = new ContactAdapter(this, contacts, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Configuration des écouteurs d'événements
     */
    private void setupListeners() {
        // Swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadContacts);

        // Bouton d'ajout de contact
        fabAddContact.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ContactFormActivity.class);
            intent.putExtra("mode", "create");
            startActivity(intent);
        });

    }

    /**
     * Chargement des contacts
     */
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
                        Toast.makeText(MainActivity.this, "Aucun contact trouvé", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Erreur lors du chargement des contacts", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Contact>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(MainActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
                System.out.println("Erreur réseau : " + t.getMessage());

            }
        });
    }

    @Override
    public void onContactClick(Contact contact) {
        Intent intent = new Intent(MainActivity.this, ContactFormActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("contact_id", contact.getId());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            // Naviguer vers l'écran de recherche
            startActivity(new Intent(MainActivity.this, ContactSearchActivity.class));
            return true;
        } else if (id == R.id.action_products) {
            // Naviguer vers l'écran de gestion des produits
            startActivity(new Intent(MainActivity.this, ProductListActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            // Déconnexion
            sessionManager.logout();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rafraîchir les contacts à chaque retour à l'activité
        loadContacts();
    }

}