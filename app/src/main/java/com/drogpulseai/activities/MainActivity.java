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
import com.drogpulseai.activities.carts.ContactCartsActivity;
import com.drogpulseai.activities.carts.ContactSelectionActivity;
import com.drogpulseai.activities.contacts.ContactFormActivity;
import com.drogpulseai.activities.contacts.ContactSearchActivity;
import com.drogpulseai.activities.products.ProductListActivity;


import com.drogpulseai.adapters.ContactAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Contact;
import com.drogpulseai.models.User;
import com.drogpulseai.sync.SyncManager;
import com.drogpulseai.utils.CameraPermissionHelper;
import com.drogpulseai.utils.Config;
import com.drogpulseai.utils.SessionManager;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.repository.ContactRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité principale avec la liste des contacts
 */
public class MainActivity extends AppCompatActivity implements ContactAdapter.OnContactClickListener, CameraPermissionHelper.PermissionCallback {

    // UI Components
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddContact;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;
    private CameraPermissionHelper cameraPermissionHelper;

    // Données
    private List<Contact> contacts;
    private User currentUser;

    // Constantes pour les actions nécessitant la caméra
    private static final int ACTION_NONE = 0;
    private static final int ACTION_SCAN_BARCODE = 1;
    private static final int ACTION_TAKE_PHOTO = 2;

    // Action en attente de permission
    private int pendingCameraAction = ACTION_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //run configuration
        Config.init(this);

        // Initialiser le SyncManager
        SyncManager.getInstance((Application) getApplicationContext());

        // Initialiser la configuration
        Config.init(this);

        // Initialiser le helper de permission caméra
        cameraPermissionHelper = new CameraPermissionHelper(this, this);

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

        // Vérifier s'il y a une connexion Internet
        if (NetworkUtils.isNetworkAvailable(this)) {
            // Mode en ligne - charger depuis l'API
            apiService.getContacts(currentUser.getId()).enqueue(new Callback<List<Contact>>() {
                @Override
                public void onResponse(Call<List<Contact>> call, Response<List<Contact>> response) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);

                    if (response.isSuccessful() && response.body() != null) {
                        // Mettre à jour la liste des contacts
                        contacts.clear();
                        contacts.addAll(response.body());
                        adapter.notifyDataSetChanged();

                        // Mettre à jour le cache local
                        ContactRepository repository = new ContactRepository(MainActivity.this);
                        for (Contact contact : contacts) {
                            repository.insertOrUpdateContact(contact);
                        }

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

                    // En cas d'erreur réseau, charger depuis le cache local
                    loadContactsFromLocalCache();
                }
            });
        } else {
            // Mode hors ligne - charger depuis le cache local
            loadContactsFromLocalCache();
        }
    }

    /**
     * Charger les contacts depuis le cache local
     */
    private void loadContactsFromLocalCache() {
        ContactRepository repository = new ContactRepository(this);
        List<Contact> cachedContacts = repository.getContactsForUser(currentUser.getId());

        contacts.clear();
        contacts.addAll(cachedContacts);
        adapter.notifyDataSetChanged();

        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);

        // Afficher un message si aucun contact en cache
        if (contacts.isEmpty()) {
            Toast.makeText(this, "Aucun contact dans le cache", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Contacts chargés depuis le cache local", Toast.LENGTH_SHORT).show();
        }
    }

    // Ajouter aussi dans onCreate ou onResume
    private void checkPendingSynchronizations() {
        SyncManager syncManager = SyncManager.getInstance((Application) getApplicationContext());

        // Si des contacts sont en attente de synchronisation et une connexion est disponible
        if (syncManager.hasPendingContacts() && NetworkUtils.isNetworkAvailable(this)) {
            syncManager.scheduleContactSyncNow();
            Toast.makeText(this, "Synchronisation des contacts en cours...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onContactClick(Contact contact) {
        Intent intent = new Intent(MainActivity.this, ContactFormActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("contact_id", contact.getId());
        startActivity(intent);
    }



    @Override
    public void onViewCartsClick(Contact contact) {
        // Lancer l'activité de liste des paniers du contact
        Intent intent = new Intent(MainActivity.this, ContactCartsActivity.class);
        intent.putExtra("contact_id", contact.getId());
        intent.putExtra("contact_name", contact.getFullName());
        startActivity(intent);
    }
    /**
     * Lancer le scanner de code-barres (après vérification de la permission)
     */
    private void scanBarcode() {
        pendingCameraAction = ACTION_SCAN_BARCODE;

        if (cameraPermissionHelper.checkAndRequestPermission()) {
            // La permission est déjà accordée, lancer le scanner immédiatement
            startBarcodeScanner();
        }
        // Sinon, onPermissionGranted sera appelé si l'utilisateur accorde la permission
    }

    /**
     * Lancer l'appareil photo pour prendre une photo (après vérification de la permission)
     */
    private void takePhoto() {
        pendingCameraAction = ACTION_TAKE_PHOTO;

        if (cameraPermissionHelper.checkAndRequestPermission()) {
            // La permission est déjà accordée, lancer l'appareil photo immédiatement
            startCamera();
        }
        // Sinon, onPermissionGranted sera appelé si l'utilisateur accorde la permission
    }

    /**
     * Lancer l'activité de scan de code-barres
     */
    private void startBarcodeScanner() {
        Toast.makeText(this, "Lancement du scanner de code-barres", Toast.LENGTH_SHORT).show();

        // Ici vous pouvez lancer votre activité de scan de code-barres
        // Exemple : ProductScanActivity

        // Intent intent = new Intent(this, ProductScanActivity.class);
        // startActivity(intent);
    }

    /**
     * Lancer l'activité de prise de photo
     */
    private void startCamera() {
        Toast.makeText(this, "Lancement de l'appareil photo", Toast.LENGTH_SHORT).show();

        // Ici vous pouvez lancer votre activité de prise de photo
        // ou utiliser l'intent de la caméra système

        // Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
        //     startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        // }
    }

    /**
     * Callback appelé lorsque la permission caméra est accordée
     */
    @Override
    public void onPermissionGranted() {
        // Exécuter l'action en attente
        switch (pendingCameraAction) {
            case ACTION_SCAN_BARCODE:
                startBarcodeScanner();
                break;
            case ACTION_TAKE_PHOTO:
                startCamera();
                break;
        }

        // Réinitialiser l'action en attente
        pendingCameraAction = ACTION_NONE;
    }

    /**
     * Callback appelé lorsque la permission caméra est refusée
     */
    @Override
    public void onPermissionDenied() {
        Toast.makeText(this,
                "Cette fonctionnalité nécessite l'accès à la caméra",
                Toast.LENGTH_LONG).show();

        // Réinitialiser l'action en attente
        pendingCameraAction = ACTION_NONE;
    }

    /**
     * Gérer le résultat de la demande de permission
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Déléguer le traitement au helper
        cameraPermissionHelper.handlePermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menu.add(Menu.NONE, R.id.action_create_cart, Menu.NONE, R.string.create_cart);
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
        } else if (id == R.id.action_scan) {
            // Lancer le scanner de code-barres (avec vérification de permission)
            scanBarcode();
            return true;
        } else if (id == R.id.action_camera) {
            // Lancer l'appareil photo (avec vérification de permission)
            takePhoto();
            return true;
        } else if (id == R.id.action_logout) {
            // Déconnexion
            sessionManager.logout();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_create_cart) {
        // Naviguer vers l'écran de sélection de contact pour créer un panier
        startActivity(new Intent(MainActivity.this, ContactSelectionActivity.class));
        return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Rafraîchir les contacts à chaque retour à l'activité
        loadContacts();
        // Vérifier les synchronisations en attente
        checkPendingSynchronizations();
    }
}