package com.drogpulseai.activities.appuser;

import android.os.Bundle;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.drogpulseai.R;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.LocationUtils;
import com.drogpulseai.utils.SessionManager;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité pour l'ajout d'utilisateurs par les administrateurs
 */
public class AdminAddUserActivity extends AppCompatActivity implements LocationUtils.LocationCallback {

    // UI Components
    private EditText etNom, etPrenom, etTelephone, etEmail, etPassword, etConfirmPassword;
    private AutoCompleteTextView spinnerTypeUser;
    private TextInputLayout tilTypeUser;
    private Button btnCreateUser, btnGetLocation;
    private ProgressBar progressBar;

    // Utilities
    private ApiService apiService;
    private LocationUtils locationUtils;
    private SessionManager sessionManager;

    // Données
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String selectedUserType = User.TYPE_COMMERCIAL; // Valeur par défaut
    private User currentAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_user);

        // Vérification des droits d'accès
        sessionManager = new SessionManager(this);
        currentAdmin = sessionManager.getUser();

        if (currentAdmin == null || !currentAdmin.isAdmin()) {
            Toast.makeText(this, "Accès refusé : droits administrateur requis", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.add_user);
        }

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        locationUtils = new LocationUtils(this, this);

        // Initialisation des vues
        initializeViews();

        // Configuration du spinner des types d'utilisateurs
        setupUserTypeSpinner();

        // Configuration des listeners
        setupListeners();
    }

    /**
     * Initialisation des vues
     */
    private void initializeViews() {
        etNom = findViewById(R.id.et_nom);
        etPrenom = findViewById(R.id.et_prenom);
        etTelephone = findViewById(R.id.et_telephone);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        spinnerTypeUser = findViewById(R.id.spinner_type_user);
        tilTypeUser = findViewById(R.id.til_type_user);
        btnCreateUser = findViewById(R.id.btn_create_user);
        btnGetLocation = findViewById(R.id.btn_get_location);
        progressBar = findViewById(R.id.progress_bar);
    }

    /**
     * Configuration du spinner des types d'utilisateurs
     */
    private void setupUserTypeSpinner() {
        // Récupérer les tableaux de ressources
        String[] userTypeLabels = getResources().getStringArray(R.array.user_types);
        String[] userTypeValues = getResources().getStringArray(R.array.user_types_values);

        // Créer l'adaptateur pour le dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                userTypeLabels
        );

        spinnerTypeUser.setAdapter(adapter);

        // Définir la valeur par défaut (Commercial)
        spinnerTypeUser.setText(userTypeLabels[0], false);
        selectedUserType = userTypeValues[0];

        // Listener pour la sélection
        spinnerTypeUser.setOnItemClickListener((parent, view, position, id) -> {
            selectedUserType = userTypeValues[position];
            showUserTypeInfo(selectedUserType);
        });
    }

    /**
     * Affiche des informations spécifiques selon le type d'utilisateur sélectionné
     */
    private void showUserTypeInfo(String userType) {
        String message = "";

        switch (userType) {
            case User.TYPE_ADMIN:
                message = "⚠️ Accès complet à toutes les fonctionnalités";
                break;
            case User.TYPE_MANAGER:
                message = "📊 Accès aux rapports et gestion d'équipe";
                break;
            case User.TYPE_COMMERCIAL:
                message = "🛍️ Gestion des produits et des ventes";
                break;
            case User.TYPE_VENDEUR:
                message = "💼 Accès aux ventes et aux clients";
                break;
            case User.TYPE_INVITE:
                message = "👁️ Accès limité en consultation";
                break;
        }

        if (!message.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Configuration des écouteurs d'événements
     */
    private void setupListeners() {
        // Bouton de création d'utilisateur
        btnCreateUser.setOnClickListener(v -> createUser());

        // Bouton pour obtenir la localisation
        btnGetLocation.setOnClickListener(v -> {
            btnGetLocation.setText("Récupération...");
            btnGetLocation.setEnabled(false);
            locationUtils.getCurrentLocation();
        });
    }

    /**
     * Processus de création d'utilisateur
     */
    private void createUser() {
        // Récupération et validation des données saisies
        String nom = etNom.getText().toString().trim();
        String prenom = etPrenom.getText().toString().trim();
        String telephone = etTelephone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validation des champs obligatoires
        if (!validateForm(nom, prenom, telephone, email, password, confirmPassword)) {
            return;
        }

        // Afficher la progression
        setLoading(true);

        // Création de l'objet utilisateur avec le type sélectionné
        // Note: La géolocalisation est optionnelle pour les admins
        User user = new User(nom, prenom, telephone, email, password, selectedUserType, latitude, longitude);

        // Appel à l'API d'inscription
        apiService.register(user).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();

                    boolean success = (boolean) result.get("success");

                    if (success) {
                        Toast.makeText(AdminAddUserActivity.this,
                                "Utilisateur créé avec succès ✅", Toast.LENGTH_LONG).show();

                        // Réinitialiser le formulaire
                        resetForm();

                        // Optionnel : Fermer l'activité après succès
                        // finish();
                    } else {
                        String message = (String) result.get("message");
                        Toast.makeText(AdminAddUserActivity.this,
                                "Erreur : " + message, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(AdminAddUserActivity.this,
                            "Erreur de connexion au serveur", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(AdminAddUserActivity.this,
                        "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
                System.out.println("Erreur réseau : " + t.getMessage());
            }
        });
    }

    /**
     * Validation complète du formulaire
     */
    private boolean validateForm(String nom, String prenom, String telephone, String email, String password, String confirmPassword) {
        // Validation du nom
        if (nom.isEmpty()) {
            etNom.setError("Nom requis");
            etNom.requestFocus();
            return false;
        }

        // Validation du prénom
        if (prenom.isEmpty()) {
            etPrenom.setError("Prénom requis");
            etPrenom.requestFocus();
            return false;
        }

        // Validation du type d'utilisateur
        if (selectedUserType == null || selectedUserType.isEmpty()) {
            tilTypeUser.setError("Type d'utilisateur requis");
            return false;
        } else {
            tilTypeUser.setError(null);
        }

        // Validation du téléphone
        if (telephone.isEmpty()) {
            etTelephone.setError("Téléphone requis");
            etTelephone.requestFocus();
            return false;
        }

        if (!Patterns.PHONE.matcher(telephone).matches()) {
            etTelephone.setError("Format du némuro téléphone invalide");
            etTelephone.requestFocus();
            return false;
        }

        // Validation de l'email
        if (email.isEmpty()) {
            etEmail.setError("Email requis");
            etEmail.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Format d'email invalide");
            etEmail.requestFocus();
            return false;
        }

        // Validation du mot de passe
        if (password.isEmpty()) {
            etPassword.setError("Mot de passe requis");
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError("Minimum 6 caractères");
            etPassword.requestFocus();
            return false;
        }

        // Validation de la confirmation du mot de passe
        if (!confirmPassword.equals(password)) {
            etConfirmPassword.setError("Mots de passe non identiques");
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Réinitialise le formulaire après création réussie
     */
    private void resetForm() {
        etNom.setText("");
        etPrenom.setText("");
        etTelephone.setText("");
        etEmail.setText("");
        etPassword.setText("");
        etConfirmPassword.setText("");

        // Remettre le type par défaut
        String[] userTypeLabels = getResources().getStringArray(R.array.user_types);
        String[] userTypeValues = getResources().getStringArray(R.array.user_types_values);
        spinnerTypeUser.setText(userTypeLabels[0], false);
        selectedUserType = userTypeValues[0];

        // Réinitialiser la localisation
        latitude = 0.0;
        longitude = 0.0;
        btnGetLocation.setText(R.string.get_location);
        btnGetLocation.setEnabled(true);
    }

    /**
     * Gérer l'état de chargement de l'interface
     */
    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCreateUser.setEnabled(!isLoading);
        btnGetLocation.setEnabled(!isLoading && latitude == 0.0 && longitude == 0.0);

        // Désactiver les champs pendant le chargement
        etNom.setEnabled(!isLoading);
        etPrenom.setEnabled(!isLoading);
        spinnerTypeUser.setEnabled(!isLoading);
        etTelephone.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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

        btnGetLocation.setText("📍 Localisation obtenue");
        btnGetLocation.setEnabled(false);

        Toast.makeText(this, "Localisation obtenue avec succès", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationError(String message) {
        btnGetLocation.setText(R.string.get_location);
        btnGetLocation.setEnabled(true);

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
        if (locationUtils != null) {
            locationUtils.stopLocationUpdates();
        }
    }
}