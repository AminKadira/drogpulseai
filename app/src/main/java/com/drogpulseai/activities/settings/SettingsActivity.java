package com.drogpulseai.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.drogpulseai.R;
import com.drogpulseai.utils.LanguageManager;

/**
 * Activité principale des paramètres de l'application
 */
public class SettingsActivity extends AppCompatActivity {

    private LinearLayout layoutLanguage;
    private TextView tvLanguageCurrent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings);
        }

        // Initialiser les vues
        initializeViews();

        // Mise à jour de l'affichage de la langue actuelle
        updateCurrentLanguageDisplay();
    }

    /**
     * Initialise les vues de l'activité
     */
    private void initializeViews() {
        // Section langue
        layoutLanguage = findViewById(R.id.layout_language);
        tvLanguageCurrent = findViewById(R.id.tv_language_current);

        // Ajouter les écouteurs de clic
        layoutLanguage.setOnClickListener(v -> {
            // Ouvrir l'activité de sélection de langue
            Intent intent = new Intent(SettingsActivity.this, LanguageSettingsActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Met à jour l'affichage de la langue actuelle
     */
    private void updateCurrentLanguageDisplay() {
        // Récupérer le code de langue actuel
        String currentLanguageCode = LanguageManager.getCurrentLanguage(this);

        // Récupérer les langues disponibles
        for (LanguageManager.LanguageItem language : LanguageManager.getAvailableLanguages()) {
            if (language.getCode().equals(currentLanguageCode)) {
                tvLanguageCurrent.setText(language.getName());
                break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mettre à jour l'affichage de la langue au retour de l'activité de sélection
        updateCurrentLanguageDisplay();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}