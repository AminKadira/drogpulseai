package com.drogpulseai.activities.contacts;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.drogpulseai.R;
import com.drogpulseai.activities.carts.CartDetailsActivity;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.models.Contact;
import com.drogpulseai.utils.DialogUtils;
import com.drogpulseai.utils.FormValidator;
import com.drogpulseai.utils.LocationUtils;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.utils.ValidationMap;
import com.drogpulseai.viewmodels.ContactFormViewModel;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité pour la création et la modification de contacts, avec support hors-ligne
 * Supporte également l'assignation directe d'un nouveau contact à un panier
 */
public class ContactFormActivity extends AppCompatActivity implements LocationUtils.LocationCallback {

    // Mode de l'activité
    private static final String MODE_CREATE = "create";
    private static final String MODE_EDIT = "edit";
    private static final String MODE_ASSIGN = "assign_contact";
    //private static final String MODE_ASSIGN_CART = "assign_cart_to_contact";
    private static final String TAG = "ContactFormActivity";

    // Variables membres
    private int cartId = -1;
    private String mode;


    // UI Components
    private EditText etNom, etPrenom, etTelephone, etEmail, etNotes;
    private Button btnSave, btnDelete, btnAssignCart;
    private ProgressBar progressBar;
    private TextView tvLocationInfo;
    private AutoCompleteTextView actType;
    private TextInputLayout tilType;
    private TextView tvCartInfo;
    // Utilities
    private LocationUtils locationUtils;
    private FormValidator validator;

    // ViewModel
    private ContactFormViewModel viewModel;


    // Données
    private double latitude = 0.0;
    private double longitude = 0.0;
    Contact contact;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_form);

        // Récupérer le mode et les données
        mode = getIntent().getStringExtra("mode");
        int contactId = getIntent().getIntExtra("contact_id", -1);

        // Récupérer l'ID du panier selon le mode
        if (MODE_ASSIGN.equals(mode)) {
            cartId = getIntent().getIntExtra("cart_id", -1);
            if (cartId == -1) {
                Toast.makeText(this, "ID du panier invalide", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Log.d(TAG, "Mode assignation contact avec cart_id: " + cartId);
        } else if (MODE_ASSIGN.equals(mode)) {
            cartId = getIntent().getIntExtra("cart_id", -1);
            if (cartId == -1 || contactId == -1) {
                Toast.makeText(this, "ID du panier ou du contact invalide", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Log.d(TAG, "Mode assignation panier avec contact_id: " + contactId + " et cart_id: " + cartId);
        }

        // Initialiser le ViewModel
        viewModel = new ViewModelProvider(this).get(ContactFormViewModel.class);

        // Initialiser le ViewModel selon le mode
        if (MODE_ASSIGN.equals(mode)) {
            viewModel.initializeMode(mode, contactId, cartId);
        } else {
            viewModel.initializeMode(mode, contactId);
        }

        // Initialiser les utilitaires
        locationUtils = new LocationUtils(this, this);
        validator = new FormValidator(this);

        // Initialisation des vues
        initializeViews();

        // Configuration des listeners
        setupListeners();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (viewModel.isCreateMode()) {
                // Convertir R.string.create_contact en String pour avoir des types compatibles
                getSupportActionBar().setTitle(MODE_ASSIGN.equals(mode) ?
                        "Créer un contact pour ce panier" : getString(R.string.create_contact));
            } else if (viewModel.isAssignCartMode()) {
                getSupportActionBar().setTitle("Associer le panier au contact");
            } else {
                getSupportActionBar().setTitle(R.string.edit_contact);
            }
        }

        // Configurer l'adaptateur pour la liste déroulante
        String[] types = {getString(R.string.contact_type_supplier),
                getString(R.string.contact_type_seller),
                getString(R.string.contact_type_distributor),
                getString(R.string.contact_type_other)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown, types);
        actType.setAdapter(adapter);

        // Configuration selon le mode
        if (viewModel.isCreateMode()) {
            btnDelete.setVisibility(View.GONE);
            btnAssignCart.setVisibility(View.GONE);
        } else if (viewModel.isAssignCartMode()) {
            // En mode assignation de panier, on désactive la modification
            //setFormEnabled(false);
            btnDelete.setVisibility(View.GONE);
            btnSave.setVisibility(View.GONE);
            btnAssignCart.setVisibility(View.VISIBLE);

            // Afficher l'info du panier
            tvCartInfo.setVisibility(View.VISIBLE);
            tvCartInfo.setText("Panier #" + cartId);
        } else {
            btnAssignCart.setVisibility(View.GONE);
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
        actType = findViewById(R.id.act_type);
        tilType = findViewById(R.id.til_type);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        btnAssignCart = findViewById(R.id.btn_assign_cart);
        progressBar = findViewById(R.id.progress_bar);
        tvLocationInfo = findViewById(R.id.tv_location_info);
        tvCartInfo = findViewById(R.id.tv_cart_info);

        // Par défaut, masquer l'info du panier et le bouton d'assignation
        tvCartInfo.setVisibility(View.GONE);
        btnAssignCart.setVisibility(View.GONE);
    }

    /**
     * Configuration des écouteurs d'événements
     */
    private void setupListeners() {
        // Bouton de sauvegarde
        btnSave.setOnClickListener(v -> validateAndSaveContact());

        // Bouton de suppression
        btnDelete.setOnClickListener(v -> confirmDeleteContact());

        // Bouton d'assignation de panier
        btnAssignCart.setOnClickListener(v -> confirmAssignCart());
    }

    /**
     * Confirmer l'assignation du panier au contact
     */
    private void confirmAssignCart() {
        DialogUtils.showConfirmationDialog(
                this,
                "Associer le panier",
                "Voulez-vous associer le panier #" + cartId + " au contact " +
                        (etPrenom.getText().toString() + " " + etNom.getText().toString()).trim() + " ?",
                () -> {

                    viewModel.assignCartToContact();
                }
        );
    }

    /**
     * Configuration des observateurs LiveData
     */
    private void setupObservers() {
        // Observer le chargement
        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!viewModel.isAssignCartMode()) {
                setFormEnabled(!isLoading);
            }
            btnAssignCart.setEnabled(!isLoading);
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
                // Si en mode assign, effectuer l'assignation avant de fermer
                if (MODE_ASSIGN.equals(mode) && cartId != -1) {
                    // Récupérer l'ID du contact nouvellement créé
                    if (viewModel.getContact().getValue() != null) {
                        int newContactId = viewModel.getContact().getValue().getId();
                        assignContactToCart(newContactId);
                    } else {
                        Toast.makeText(this, "Erreur: ID de contact non disponible",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else {
                    // Comportement normal pour les autres modes
                    finish();
                }
            }
        });

        // Observer le succès de l'assignation du panier
        viewModel.getAssignmentSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Panier associé au contact avec succès", Toast.LENGTH_SHORT).show();

                // Rediriger vers les détails du panier
                Intent intent = new Intent(this, CartDetailsActivity.class);
                intent.putExtra("cart_id", cartId);
                startActivity(intent);
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

        if (contact.getType() != null) {
            actType.setText(contact.getType(), false);
        }

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
        actType.setEnabled(enabled);
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
        //validationMap.add(etPrenom, validator.required("Prénom requis"));
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
        contact = getContactFromForm();

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
        String type = actType.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // Créer l'objet contact
        Contact contact = new Contact(nom, prenom, telephone, email, notes, type, latitude, longitude, viewModel.getCurrentUserId());

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

    /**
     * Assigne un contact nouvellement créé au panier
     */
    private void assignContactToCart(int contactId) {
        Log.d(TAG, "Assignation du contact " + contactId + " au panier " + cartId);

        // Afficher un dialogue de progression
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("Association en cours")
                .setMessage("Association du contact au panier...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Créer les données pour l'API
        Map<String, Object> assignData = new HashMap<>();
        assignData.put("cart_id", cartId);
        assignData.put("contact_id", contactId);
        assignData.put("user_id", viewModel.getCurrentUserId());

        // Appeler l'API
        ApiClient.getApiService().assignContactToCart(assignData)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call,
                                           Response<Map<String, Object>> response) {
                        progressDialog.dismiss();

                        if (response.isSuccessful() && response.body() != null) {
                            // Vérifier le succès de l'opération
                            boolean success = false;
                            try {
                                success = (boolean) response.body().get("success");
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur lors du parsing de la réponse", e);
                            }

                            if (success) {
                                Toast.makeText(ContactFormActivity.this,
                                        "Contact créé et assigné au panier avec succès",
                                        Toast.LENGTH_SHORT).show();

                                // Rediriger vers les détails du panier
                                Intent intent = new Intent(ContactFormActivity.this,
                                        CartDetailsActivity.class);
                                intent.putExtra("cart_id", cartId);
                                startActivity(intent);
                                finish();
                            } else {
                                String message = response.body().containsKey("message") ?
                                        (String) response.body().get("message") :
                                        "Erreur lors de l'assignation";

                                Toast.makeText(ContactFormActivity.this,
                                        "Contact créé mais erreur lors de l'assignation: " + message,
                                        Toast.LENGTH_LONG).show();
                                finish();
                            }
                        } else {
                            String errorBody = "";
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erreur lors de la lecture de l'erreur", e);
                            }

                            Log.e(TAG, "Erreur API: " + response.code() + " - " + errorBody);

                            Toast.makeText(ContactFormActivity.this,
                                    "Contact créé mais erreur de serveur lors de l'assignation",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Échec de la requête d'assignation", t);

                        Toast.makeText(ContactFormActivity.this,
                                "Contact créé mais erreur réseau: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                        finish();
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