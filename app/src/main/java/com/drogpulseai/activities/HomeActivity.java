package com.drogpulseai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.drogpulseai.R;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;

public class HomeActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialisation des utilitaires
        sessionManager = new SessionManager(this);

        // Vérifier si l'utilisateur est connecté
        if (!sessionManager.isLoggedIn()) {
            // Rediriger vers la page de connexion
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Récupérer les données de l'utilisateur
        currentUser = sessionManager.getUser();

        // Afficher le nom de l'utilisateur
        TextView tvUserName = findViewById(R.id.tv_user_name);
        tvUserName.setText(currentUser.getFullName());

        // Configuration des cartes
        setupCards();
    }

    private void setupCards() {
        // Carte Contacts
        MaterialCardView cardContacts = findViewById(R.id.card_contacts);
        cardContacts.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
        });

        // Carte Produits
        MaterialCardView cardProducts = findViewById(R.id.card_products);
        cardProducts.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProductListActivity.class));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            // Déconnexion
            sessionManager.logout();
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}