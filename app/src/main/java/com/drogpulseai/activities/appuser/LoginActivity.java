package com.drogpulseai.activities.appuser;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.drogpulseai.R;
import com.drogpulseai.activities.HomeActivity;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.Config;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.utils.SessionManager;
import com.google.gson.Gson;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activité d'authentification - Point d'entrée de l'application
 */
public class LoginActivity extends AppCompatActivity {

    // UI Components
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //run configuration
        Config.init(this);

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);

       // Toast.makeText(LoginActivity.this, "Welcome to DrogpulseAi "+ApiClient.getBaseUrl().substring(8,15), Toast.LENGTH_SHORT).show();

        TextView tv1 = (TextView)findViewById(R.id.tv_app_subtitle);
        tv1.setText(tv1.getText());

        // Vérifier si l'utilisateur est déjà connecté
        if (sessionManager.isLoggedIn()) {
            navigateToMainActivity();
            finish();
            return;
        }

        // Initialisation des vues
        initializeViews();

        // Configuration des listeners
        setupListeners();
    }

    /**
     * Initialisation des vues
     */
    private void initializeViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);
    }

    /**
     * Configuration des écouteurs d'événements
     */
    private void setupListeners() {
        // Bouton de connexion
        btnLogin.setOnClickListener(v -> login());

    }

    /**
     * Processus de connexion
     */
    private void login() {
        // Récupération et validation des données saisies
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Veuillez saisir votre email");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Veuillez saisir votre mot de passe");
            etPassword.requestFocus();
            return;
        }

        // Afficher la progression
        setLoading(true);

        // Appel à l'API d'authentification
        apiService.login(email, password).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();

                    boolean success = (boolean) result.get("success");

                    if (success) {
                        // Traitement des données utilisateur reçues
                        Map<String, Object> userData = (Map<String, Object>) result.get("user");

                        // Conversion des données en objet User
                        Gson gson = new Gson();
                        User user = gson.fromJson(gson.toJson(userData), User.class);

                        user.setTypeUser(userData.get("typeUser").toString());

                        // Enregistrement de la session
                        sessionManager.createSession(user);

                        Toast.makeText(LoginActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();

                        // Navigation vers la page principale
                        navigateToMainActivity();
                        finish();
                    } else {
                        String message = (String) result.get("message");
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Erreur de connexion au serveur", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
                System.out.println("Erreur réseau : " + t.getMessage());
//                if (NetworkUtils.isNetworkAvailable(this)) {
//
//                }

            }
        });

    }

    /**
     * Gérer l'état de chargement de l'interface
     */
    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
    }

    /**
     * Navigation vers la page principale
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }


}