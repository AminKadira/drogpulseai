package com.drogpulseai.activities.contacts;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.drogpulseai.R;
import com.drogpulseai.models.Contact;
import com.drogpulseai.utils.DialogUtils;
import com.drogpulseai.utils.FormValidator;
import com.drogpulseai.utils.LocationUtils;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.utils.ValidationMap;
import com.drogpulseai.viewmodels.ContactFormViewModel;

/**
 * Activité pour la création et la modification de contacts, avec support hors-ligne
 */
public class ContactFormActivity extends AppCompatActivity implements LocationUtils.LocationCallback {

    // Mode de l'activité
    private static final String MODE_CREATE = "create";
    private static final String MODE_EDIT = "edit";

    // UI Components
    private EditText etNom, etPrenom, etTelephone, etEmail, etNotes;
    private Button btnSave, btnDelete;
    private ProgressBar progressBar;
    private TextView tvLocationInfo;

    // Utilities
    private LocationUtils locationUtils;
    private FormValidator validator;

    // ViewModel
    private ContactFormViewModel viewModel;

    // Données
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_form);

        // Récupérer le mode et les données
        String mode = getIntent().getStringExtra("mode");
        int contactId = getIntent().getIntExtra("contact_id", -1);

        // Initialiser le ViewModel
        viewModel = new ViewModelProvider(this).get(ContactFormViewModel.class);
        viewModel.initializeMode(mode, contactId);

        // Initialiser les utilitaires
        locationUtils = new LocationUtils(this, this);
        validator = new FormValidator(this);

        // Initialisation des vues
        initializeViews();

        // Configuration des listeners
        setupListeners();

        // Configuration de la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(viewModel.isCreateMode() ?
                    R.string.create_contact : R.string.edit_contact);
        }

        // Configuration selon le mode
        if (viewModel.isCreateMode()) {
            btnDelete.setVisibility(View.GONE);
        } else {
            viewModel.loadContactDetails();
        }

        // Démarrer la détection de localisation
        locationUtils.getCurrentLocation();

        // Observer les changements de données
        setupObservers();
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
        tvLocationInfo = findViewById(R.id.tv_location_info);
    }

    /**
     * Configuration des écouteurs d'événements
     */
    private void setupListeners() {
        // Bouton de sauvegarde
        btnSave.setOnClickListener(v -> validateAndSaveContact());

        // Bouton de suppression
        btnDelete.setOnClickListener(v -> confirmDeleteContact());
    }

    /**
     * Configuration des observateurs LiveData
     */
    private void setupObservers() {
        // Observer le chargement
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            setFormEnabled(!isLoading);
        });

        // Observer les données du contact
        viewModel.getContact().observe(this, contact -> {
            if (contact != null) {
                populateForm(contact);
            }
        });

        // Observer les messages d'erreur
        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });

        // Observer le succès des opérations
        viewModel.getOperationSuccess().observe(this, success -> {
            if (success) {
                finish();
            }
        });
    }

    /**
     * Remplir le formulaire avec les données du contact
     */
    private void populateForm(Contact contact) {
        etNom.setText(contact.getNom());
        etPrenom.setText(contact.getPrenom());
        etTelephone.setText(contact.getTelephone());
        etEmail.setText(contact.getEmail());
        etNotes.setText(contact.getNotes());

        // Stocker les coordonnées
        latitude = contact.getLatitude();
        longitude = contact.getLongitude();

        // Mettre à jour l'info de localisation
        tvLocationInfo.setText(R.string.location_retrieved);
    }

    /**
     * Activer/désactiver tous les champs du formulaire
     */
    private void setFormEnabled(boolean enabled) {
        etNom.setEnabled(enabled);
        etPrenom.setEnabled(enabled);
        etTelephone.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etNotes.setEnabled(enabled);
        btnSave.setEnabled(enabled);
        btnDelete.setEnabled(enabled);
    }

    /**
     * Valider le formulaire et sauvegarder le contact
     */
    private void validateAndSaveContact() {
        // Créer la map de validation
        ValidationMap validationMap = new ValidationMap();
        validationMap.add(etNom, validator.required("Nom requis"));
        validationMap.add(etPrenom, validator.required("Prénom requis"));
        validationMap.add(etTelephone, validator.required("Téléphone requis"));

        // Valider l'email si présent
        if (!etEmail.getText().toString().trim().isEmpty()) {
            validationMap.add(etEmail, validator.email("Email invalide"));
        }

        // Valider le formulaire
        if (!validator.validate(validationMap)) {
            return;
        }

        // Vérifier la localisation
        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "Récupération de la localisation en cours...", Toast.LENGTH_SHORT).show();
            locationUtils.getCurrentLocation();
            return;
        }

        // Créer l'objet contact à partir des données du formulaire
        Contact contact = getContactFromForm();

        // Sauvegarder le contact
        viewModel.saveContact(contact);

        // Informer l'utilisateur si mode hors ligne
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this,
                    "Contact sauvegardé localement. Synchronisation automatique dès que la connexion sera rétablie.",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Créer un objet Contact à partir des données du formulaire
     */
    private Contact getContactFromForm() {
        String nom = etNom.getText().toString().trim();
        String prenom = etPrenom.getText().toString().trim();
        String telephone = etTelephone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // Créer l'objet contact
        Contact contact = new Contact(nom, prenom, telephone, email, notes, latitude, longitude, viewModel.getCurrentUserId());

        // Si en mode édition, définir l'ID existant
        if (!viewModel.isCreateMode() && viewModel.getContact().getValue() != null) {
            contact.setId(viewModel.getContact().getValue().getId());
        }

        return contact;
    }

    /**
     * Confirmer la suppression d'un contact
     */
    private void confirmDeleteContact() {
        DialogUtils.showConfirmationDialog(
                this,
                getString(R.string.delete_contact),
                "Êtes-vous sûr de vouloir supprimer ce contact ?",
                () -> viewModel.deleteContact()
        );
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
        tvLocationInfo.setText(R.string.location_retrieved);
    }

    @Override
    public void onLocationError(String message) {
        // Si c'est en mode édition, on garde les coordonnées existantes
        if (!viewModel.isCreateMode() && viewModel.getContact().getValue() != null) {
            return;
        }

        Toast.makeText(this, "Erreur de localisation : " + message, Toast.LENGTH_LONG).show();
        tvLocationInfo.setText(R.string.location_error);
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