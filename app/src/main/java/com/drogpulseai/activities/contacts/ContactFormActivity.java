package com.drogpulseai.activities.contacts;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.drogpulseai.R;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Contact;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.LocationUtils;
import com.drogpulseai.utils.SessionManager;

import java.util.Map;

import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité pour la création et la modification de contacts
 */
public class ContactFormActivity extends AppCompatActivity implements LocationUtils.LocationCallback {

    // Mode de l'activité
    private static final String MODE_CREATE = "create";
    private static final String MODE_EDIT = "edit";

    // UI Components
    private EditText etNom, etPrenom, etTelephone, etEmail, etNotes;
    private Button btnSave, btnDelete;
    private ProgressBar progressBar;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;
    private LocationUtils locationUtils;

    // Données
    private String mode;
    private int contactId;
    private User currentUser;
    private Contact currentContact;
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_form);

        // Récupérer le mode et les données
        mode = getIntent().getStringExtra("mode");
        contactId = getIntent().getIntExtra("contact_id", -1);

        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(MODE_CREATE.equals(mode) ?
                    R.string.create_contact : R.string.edit_contact);
        }

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);
        locationUtils = new LocationUtils(this, this);

        // Récupérer l'utilisateur courant
        currentUser = sessionManager.getUser();

        // Initialisation des vues
        initializeViews();

        // Configuration des listeners
        setupListeners();

        // Configuration initiale selon le mode
        if (MODE_CREATE.equals(mode)) {
            setupForCreateMode();
        } else if (MODE_EDIT.equals(mode) && contactId != -1) {
            loadContactDetails(contactId);
        } else {
            Toast.makeText(this, "Mode invalide", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Démarrer la détection de localisation
        locationUtils.getCurrentLocation();
    }

    /**
     * Initialisation des vues
     */
    private void initializeViews() {
        etNom = findViewById(R.id.et_nom);
        etPrenom = findViewById(R.id.et_prenom);
        etTelephone = findViewById(R.id.et_telephone);
        etEmail = findViewById(R.id.et_email);
        etNotes = findViewById(R.id.et_notes);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        progressBar = findViewById(R.id.progress_bar);
    }

    /**
     * Configuration des écouteurs d'événements
     */
    private void setupListeners() {
        // Bouton de sauvegarde
        btnSave.setOnClickListener(v -> saveContact());

        // Bouton de suppression
        btnDelete.setOnClickListener(v -> confirmDeleteContact());
    }

    /**
     * Configuration pour le mode création
     */
    private void setupForCreateMode() {
        btnDelete.setVisibility(View.GONE);
    }

    /**
     * Chargement des détails d'un contact existant
     */
    private void loadContactDetails(int contactId) {
        progressBar.setVisibility(View.VISIBLE);

        apiService.getContactDetails(contactId).enqueue(new Callback<Contact>() {
            @Override
            public void onResponse(Call<Contact> call, Response<Contact> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    currentContact = response.body();

                    // Remplir les champs avec les données du contact
                    etNom.setText(currentContact.getNom());
                    etPrenom.setText(currentContact.getPrenom());
                    etTelephone.setText(currentContact.getTelephone());
                    etEmail.setText(currentContact.getEmail());
                    etNotes.setText(currentContact.getNotes());

                    // Stocker les coordonnées
                    latitude = currentContact.getLatitude();
                    longitude = currentContact.getLongitude();
                } else {
                    Toast.makeText(ContactFormActivity.this,
                            "Erreur lors du chargement des détails du contact",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Contact> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ContactFormActivity.this,
                        "Erreur réseau : " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    /**
     * Sauvegarde du contact (création ou mise à jour)
     */
    private void saveContact() {
        // Récupération des données saisies
        String nom = etNom.getText().toString().trim();
        String prenom = etPrenom.getText().toString().trim();
        String telephone = etTelephone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // Validation des champs requis
        if (nom.isEmpty()) {
            etNom.setError("Veuillez saisir le nom");
            etNom.requestFocus();
            return;
        }

        if (prenom.isEmpty()) {
            etPrenom.setError("Veuillez saisir le prénom");
            etPrenom.requestFocus();
            return;
        }

        if (telephone.isEmpty()) {
            etTelephone.setError("Veuillez saisir le téléphone");
            etTelephone.requestFocus();
            return;
        }

        // Vérifier la localisation
        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "Récupération de la localisation en cours...", Toast.LENGTH_SHORT).show();
            locationUtils.getCurrentLocation();
            return;
        }

        // Afficher la progression
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        if (MODE_CREATE.equals(mode)) {
            // Création d'un nouveau contact
            Contact newContact = new Contact(nom, prenom, telephone, email, notes, latitude, longitude, currentUser.getId());

            apiService.createContact(newContact).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> result = response.body();

                        boolean success = (boolean) result.get("success");

                        if (success) {
                            Toast.makeText(ContactFormActivity.this, "Contact créé avec succès", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String message = (String) result.get("message");
                            Toast.makeText(ContactFormActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(ContactFormActivity.this, "Erreur lors de la création du contact", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Log.e("API", "Erreur complète: " + t.toString());
                    Log.e("API", "Cause: " + (t.getCause() != null ? t.getCause().toString() : "Inconnue"));
                    Log.e("API", "Message: " + t.getMessage());
                    Log.e("API", "URL: " + call.request().url());

                    // Enregistrer la requête pour débogage
                    Request request = call.request();
                    try {
                        Log.e("API", "Headers: " + request.headers().toString());
                        if (request.body() != null) {
                            Log.e("API", "Body class: " + request.body().getClass().getName());
                        }
                    } catch (Exception e) {
                        Log.e("API", "Erreur lors de l'analyse de la requête: " + e.getMessage());
                    }
                    Toast.makeText(ContactFormActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();

                }
            });
        } else {
            // Mise à jour du contact existant
            currentContact.setNom(nom);
            currentContact.setPrenom(prenom);
            currentContact.setTelephone(telephone);
            currentContact.setEmail(email);
            currentContact.setNotes(notes);
            currentContact.setLatitude(latitude);
            currentContact.setLongitude(longitude);

            apiService.updateContact(currentContact).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> result = response.body();

                        boolean success = (boolean) result.get("success");

                        if (success) {
                            Toast.makeText(ContactFormActivity.this, "Contact mis à jour avec succès", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            String message = (String) result.get("message");
                            Toast.makeText(ContactFormActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(ContactFormActivity.this, "Erreur lors de la mise à jour du contact", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(ContactFormActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
                    System.out.println("Erreur réseau : " + t.getMessage());

                }
            });
        }
    }

    /**
     * Confirmation avant la suppression d'un contact
     */
    private void confirmDeleteContact() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Supprimer le contact");
        builder.setMessage("Êtes-vous sûr de vouloir supprimer ce contact ?");
        builder.setPositiveButton("Oui", (dialog, which) -> deleteContact());
        builder.setNegativeButton("Non", null);
        builder.show();
    }

    /**
     * Suppression du contact
     */
    private void deleteContact() {
        progressBar.setVisibility(View.VISIBLE);
        btnDelete.setEnabled(false);

        apiService.deleteContact(currentContact.getId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                btnDelete.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();

                    boolean success = (boolean) result.get("success");

                    if (success) {
                        Toast.makeText(ContactFormActivity.this, "Contact supprimé avec succès", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String message = (String) result.get("message");
                        Toast.makeText(ContactFormActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(ContactFormActivity.this, "Erreur lors de la suppression du contact", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnDelete.setEnabled(true);
                Toast.makeText(ContactFormActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
                System.out.println("Erreur réseau : " + t.getMessage());

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationReceived(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public void onLocationError(String message) {
        // Si c'est en mode édition, on garde les coordonnées existantes
        if (MODE_EDIT.equals(mode) && currentContact != null) {
            return;
        }

        Toast.makeText(this, "Erreur de localisation : " + message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationUtils.handlePermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationUtils.stopLocationUpdates();
    }
}