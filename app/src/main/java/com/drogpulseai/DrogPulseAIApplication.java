package com.drogpulseai;

import android.app.Application;

import com.drogpulseai.sync.SyncManager;
import com.drogpulseai.utils.Config;

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

        // Autres initialisations si nécessaire...
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // Nettoyer les ressources
        SyncManager.getInstance(this).cleanup();
    }
}