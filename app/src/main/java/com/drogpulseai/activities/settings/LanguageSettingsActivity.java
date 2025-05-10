package com.drogpulseai.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.drogpulseai.R;
import com.drogpulseai.activities.HomeActivity;
import com.drogpulseai.activities.carts.CartManagementActivity;
import com.drogpulseai.activities.carts.ContactSelectionActivity;
import com.drogpulseai.adapters.LanguageAdapter;
import com.drogpulseai.utils.LanguageManager;

import java.util.List;

/**
 * Activité permettant à l'utilisateur de changer la langue de l'application
 * Supporte maintenant l'option de langue automatique (langue du téléphone)
 */
public class LanguageSettingsActivity extends AppCompatActivity {

    private static final String TAG = "LanguageSettingsActiv";
    private ListView languageListView;
    private List<LanguageManager.LanguageItem> languageItems;
    private String currentLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_settings);

        // Configurer la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.language_settings);
        }

        // Récupérer la langue actuelle
        currentLanguage = LanguageManager.getCurrentLanguage(this);
        Log.d(TAG, "Langue actuelle: " + currentLanguage);

        // Initialiser la liste des langues
        languageListView = findViewById(R.id.list_view_languages);
        setupLanguageList();
    }

    /**
     * Configure la liste des langues disponibles avec un adaptateur personnalisé
     */
    private void setupLanguageList() {
        // Récupérer les langues disponibles (maintenant avec l'option "Langue du téléphone")
        languageItems = LanguageManager.getAvailableLanguages();
        Log.d(TAG, "Nombre de langues disponibles: " + languageItems.size());

        // Créer un adaptateur personnalisé pour la liste
        LanguageAdapter adapter = new LanguageAdapter(this, languageItems, currentLanguage);

        // Définir l'adaptateur
        languageListView.setAdapter(adapter);

        // Définir le listener de clic
        languageListView.setOnItemClickListener((parent, view, position, id) -> {
            LanguageManager.LanguageItem selectedLanguage = languageItems.get(position);
            Log.d(TAG, "Langue sélectionnée: " + selectedLanguage.getCode() + " (" + selectedLanguage.getName() + ")");

            // Ne rien faire si la langue sélectionnée est déjà la langue actuelle
            if (selectedLanguage.getCode().equals(currentLanguage)) {
                Log.d(TAG, "Langue déjà sélectionnée, aucune action");
                return;
            }

            // Changer la langue (maintenant avec support pour "auto")
            Log.d(TAG, "Tentative de changement de langue vers: " + selectedLanguage.getCode());
            boolean success = LanguageManager.setLanguage(this, selectedLanguage.getCode());

            if (!success) {
                Log.e(TAG, "Erreur lors du changement de langue");
                Toast.makeText(this, R.string.language_change_error, Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "Langue changée avec succès");
                // L'activité sera recréée automatiquement
                Intent intent= new Intent(this, HomeActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Vérifier si la langue a changé et mettre à jour l'adaptateur si nécessaire
        String newLanguage = LanguageManager.getCurrentLanguage(this);
        Log.d(TAG, "onResume - Langue précédente: " + currentLanguage + ", Langue actuelle: " + newLanguage);

        if (!newLanguage.equals(currentLanguage)) {
            currentLanguage = newLanguage;
            setupLanguageList();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Log.d(TAG, "Bouton retour actionné");
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}