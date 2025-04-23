package com.drogpulseai.activities.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drogpulseai.R;
import com.drogpulseai.adapters.ContactAdapter;
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

/**
 * Activité de recherche de contacts
 */
public class ContactSearchActivity extends AppCompatActivity implements ContactAdapter.OnContactClickListener {

    // UI Components
    private EditText etSearch;
    private ImageButton btnSearch;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvNoResults;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;

    // Données
    private List<Contact> contacts;
    private ContactAdapter adapter;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_search);

        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.search_contacts);
        }

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);

        // Récupérer l'utilisateur courant
        currentUser = sessionManager.getUser();

        // Initialisation des vues
        initializeViews();

        // Configuration du RecyclerView
        setupRecyclerView();

        // Configuration des listeners
        setupListeners();
    }

    /**
     * Initialisation des vues
     */
    private void initializeViews() {
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        tvNoResults = findViewById(R.id.tv_no_results);
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
        btnSearch.setOnClickListener(v -> performSearch());

        // Recherche lorsque l'utilisateur appuie sur Entrée
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });
    }

    /**
     * Effectuer la recherche
     */
    private void performSearch() {
        String query = etSearch.getText().toString().trim();

        if (query.isEmpty()) {
            etSearch.setError("Veuillez saisir un terme de recherche");
            return;
        }

        // Afficher la progression
        progressBar.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);

        // Appel à l'API pour la recherche
        apiService.searchContacts(currentUser.getId(), query).enqueue(new Callback<List<Contact>>() {
            @Override
            public void onResponse(Call<List<Contact>> call, Response<List<Contact>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    contacts.clear();
                    contacts.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    // Afficher un message si aucun résultat
                    if (contacts.isEmpty()) {
                        tvNoResults.setVisibility(View.VISIBLE);
                    } else {
                        tvNoResults.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(ContactSearchActivity.this, "Erreur lors de la recherche", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Contact>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ContactSearchActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
                System.out.println("Erreur réseau : " + t.getMessage());

            }
        });
    }

    @Override
    public void onContactClick(Contact contact) {
        Intent intent = new Intent(ContactSearchActivity.this, ContactFormActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("contact_id", contact.getId());
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