package com.drogpulseai;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import com.drogpulseai.sync.SyncManager;
import com.drogpulseai.utils.Config;
import com.drogpulseai.utils.LanguageManager;

import java.util.Locale;

/**
 * Classe d'application principale pour DrogPulseAI
 */
public class DrogPulseAIApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialiser la configuration
        Config.init(this);

        // Initialiser le SyncManager
        SyncManager.getInstance(this);

        // Initialiser la langue de l'application
        LanguageManager.initLanguage(this);

        // Autres initialisations si nécessaire...
    }

    /**
     * Cette méthode est appelée quand la configuration de l'application change
     * Utile pour maintenir la langue sélectionnée lors des changements de configuration
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Vérifier si la langue du téléphone a changé
        if (LanguageManager.hasDeviceLanguageChanged(this)) {
            // Si oui et que l'utilisateur a choisi de suivre la langue du téléphone,
            // réappliquer la nouvelle langue de l'appareil
            LanguageManager.initLanguage(this);
        } else {
            // Sinon, réappliquer la langue enregistrée
            LanguageManager.initLanguage(this);
        }
    }

    /**
     * Cette méthode est appelée avant la création de chaque activité
     * Elle permet d'appliquer la langue choisie à toutes les activités
     */
    @Override
    protected void attachBaseContext(Context base) {
        // Récupérer la langue active (tenant compte des préférences utilisateur)
        String languageCode = LanguageManager.getActiveLanguage(base);

        // Créer une locale avec la langue active
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        // Créer une nouvelle configuration avec la locale
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);

        // Appliquer la configuration
        Context newContext = base.createConfigurationContext(config);

        super.attachBaseContext(newContext);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // Nettoyer les ressources
        SyncManager.getInstance(this).cleanup();
    }
}