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
import com.drogpulseai.activities.carts.ContactCartsActivity;
import com.drogpulseai.activities.products.ProductFormActivity;
import com.drogpulseai.adapters.ContactAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Contact;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private FloatingActionButton fabAddContact;

    private Chip chipAll,chipFournisseur,chipVendeur,chipDistributeur,chipAutre;
    private ChipGroup chipGroup;
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

        // Définir "Tous" comme sélection par défaut
        chipAll.setChecked(true);


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
        // Initialiser et configurer le ChipGroup
        chipGroup = findViewById(R.id.chip_group_types);
        chipAll = findViewById(R.id.chip_all);
        chipFournisseur = findViewById(R.id.chip_fournisseur);
        chipVendeur = findViewById(R.id.chip_vendeur);
        chipDistributeur = findViewById(R.id.chip_distributeur);
        chipAutre = findViewById(R.id.chip_autre);
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

        btnSearch.setOnClickListener(v -> performSearch());

        chipGroup.setOnCheckedStateChangeListener((group, checkedId) -> {
            // Rafraîchir la recherche avec le nouveau filtre
            performSearch();
        });

        // Recherche lorsque l'utilisateur appuie sur Entrée
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });

        // Bouton d'ajout de contact
        fabAddContact.setOnClickListener(v -> {
            Intent intent = new Intent(this, ContactFormActivity.class);
            intent.putExtra("mode", "create");
            startActivity(intent);
        });
    }

    /**
     * Effectuer la recherche
     */
    private void performSearch() {
        String query = etSearch.getText().toString().trim();

        // Déterminer le type sélectionné
        String selectedType = null;

        if (chipFournisseur.isChecked()) {
            selectedType = "Fournisseur";
        } else if (chipVendeur.isChecked()) {
            selectedType = "Vendeur";
        } else if (chipDistributeur.isChecked()) {
            selectedType = "Distributeur";
        } else if (chipAutre.isChecked()) {
            selectedType = "Autre";
        }

        // Afficher la progression
        progressBar.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);

        // Désactiver les contrôles pendant la recherche
        setControlsEnabled(false);

        // Appel à l'API avec les paramètres (noter que query peut être vide)
        apiService.searchContacts(currentUser.getId(), query, selectedType).enqueue(new Callback<List<Contact>>() {
            @Override
            public void onResponse(Call<List<Contact>> call, Response<List<Contact>> response) {
                progressBar.setVisibility(View.GONE);
                setControlsEnabled(true);

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
                setControlsEnabled(true);
                Toast.makeText(ContactSearchActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
                System.out.println("Erreur réseau : " + t.getMessage());
            }
        });
    }

    // Méthode utilitaire pour activer/désactiver les contrôles
    private void setControlsEnabled(boolean enabled) {
        btnSearch.setEnabled(enabled);
        etSearch.setEnabled(enabled);

        // Activer/désactiver les chips
        ChipGroup chipGroup = findViewById(R.id.chip_group_types);
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            chipGroup.getChildAt(i).setEnabled(enabled);
        }
    }
    @Override
    public void onContactClick(Contact contact) {
        // Code existant pour gérer le clic sur un contact
        Intent intent = new Intent(ContactSearchActivity.this, ContactFormActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("contact_id", contact.getId());
        startActivity(intent);
    }
    @Override
    public void onAddProductsClick(Contact contact) {
        // Naviguer vers l'activité d'ajout de produits pour ce fournisseur
        Intent intent = new Intent(this, ProductFormActivity.class);
        intent.putExtra("mode", "create");
        intent.putExtra("supplier_id", contact.getId());
        intent.putExtra("supplier_name", contact.getFullName());
        startActivity(intent);
    }
    @Override
    public void onViewCartsClick(Contact contact) {
        // Implémentation pour gérer le clic sur le bouton Panier
        Intent intent = new Intent(ContactSearchActivity.this, ContactCartsActivity.class);
        intent.putExtra("contact_id", contact.getId());
        intent.putExtra("contact_name", contact.getFullName());
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