package com.drogpulseai.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.drogpulseai.R;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.LocationUtils;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité de création de compte utilisateur
 */
public class RegisterActivity extends AppCompatActivity implements LocationUtils.LocationCallback {

    // UI Components
    private EditText etNom, etPrenom, etTelephone, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    // Utilities
    private ApiService apiService;
    private LocationUtils locationUtils;

    // Données
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.register);
        }

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        locationUtils = new LocationUtils(this, this);

        // Initialisation des vues
        initializeViews();

        // Configuration des listeners
        setupListeners();

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
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progress_bar);
    }

    /**
     * Configuration des écouteurs d'événements
     */
    private void setupListeners() {
        // Bouton d'inscription
        btnRegister.setOnClickListener(v -> register());

        // Lien vers la connexion
        tvLogin.setOnClickListener(v -> finish());
    }

    /**
     * Processus d'inscription
     */
    private void register() {
        // Récupération et validation des données saisies
        String nom = etNom.getText().toString().trim();
        String prenom = etPrenom.getText().toString().trim();
        String telephone = etTelephone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
//TODO : Add Validation méthode
        // Validation des champs obligatoires
        if (nom.isEmpty()) {
            etNom.setError("Veuillez saisir votre nom");
            etNom.requestFocus();
            return;
        }

        if (prenom.isEmpty()) {
            etPrenom.setError("Veuillez saisir votre prénom");
            etPrenom.requestFocus();
            return;
        }

        if (telephone.isEmpty()) {
            etTelephone.setError("Veuillez saisir votre numéro de téléphone");
            etTelephone.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Veuillez saisir votre email");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Veuillez saisir un mot de passe");
            etPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty() || !confirmPassword.equals(password)) {
            etConfirmPassword.setError("Les mots de passe ne correspondent pas");
            etConfirmPassword.requestFocus();
            return;
        }

        // Vérifier la localisation
        if (latitude == 0.0 && longitude == 0.0) {
            Toast.makeText(this, "Récupération de la localisation en cours...", Toast.LENGTH_SHORT).show();
            locationUtils.getCurrentLocation();
            return;
        }

        // Afficher la progression
        setLoading(true);

        // Création de l'objet utilisateur
        User user = new User(nom, prenom, telephone, email, password, latitude, longitude);

        // Appel à l'API d'inscription
        apiService.register(user).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();

                    boolean success = (boolean) result.get("success");

                    if (success) {
                        Toast.makeText(RegisterActivity.this, "Inscription réussie", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String message = (String) result.get("message");
                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this, "Erreur de connexion au serveur", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
                System.out.println("Erreur réseau : " + t.getMessage());
            }
        });
    }

    /**
     * Gérer l'état de chargement de l'interface
     */
    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        etNom.setEnabled(!isLoading);
        etPrenom.setEnabled(!isLoading);
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
        Toast.makeText(this, "Localisation obtenue", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationError(String message) {
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