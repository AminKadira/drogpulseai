package com.drogpulseai.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.widget.Toast;

import com.drogpulseai.activities.appuser.LoginActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final String TAG = "Config";
    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties;
    private static boolean initialized = false;

    public static void init(Context context) {
        if (context == null) {
            Log.e(TAG, "Contexte null, impossible d'initialiser la configuration");
            return;
        }

        properties = new Properties();
        try {
            AssetManager assetManager = context.getAssets();
            if (assetManager != null) {
                InputStream inputStream = null;
                try {
                    inputStream = assetManager.open(CONFIG_FILE);
                    properties.load(inputStream);
                    initialized = true;
                    Log.d(TAG, "Configuration chargée avec succès");
                } catch (IOException e) {
                    Log.e(TAG, "Fichier de configuration introuvable: " + e.getMessage());
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Erreur lors de la fermeture du flux: " + e.getMessage());
                        }
                    }
                }
            } else {
                Log.e(TAG, "AssetManager est null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'initialisation: " + e.getMessage());
        }
    }

    public static String getApiBaseUrl() {
        // URL par défaut si la configuration n'est pas initialisée
        String defaultUrl="";

        if (!initialized || properties == null) {
            Log.w(TAG, "Configuration non initialisée, utilisation de l'URL par défaut");
            return defaultUrl;
        }

        try {
            return properties.getProperty("api.base_url", defaultUrl);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la récupération de l'URL: " + e.getMessage());
            return defaultUrl;
        }
    }
}