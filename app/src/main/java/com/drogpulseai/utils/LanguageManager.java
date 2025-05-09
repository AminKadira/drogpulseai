package com.drogpulseai.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gestionnaire de langues pour l'application
 * Permet de changer la langue de l'application à tout moment et de la sauvegarder
 */
public class LanguageManager {

    private static final String TAG = "LanguageManager";
    private static final String PREF_NAME = "drogpulseai_language";
    private static final String PREF_LANGUAGE_CODE = "language_code";
    private static final String PREF_USE_DEVICE_LANGUAGE = "use_device_language";
    private static final String DEFAULT_LANGUAGE = "fr"; // Français par défaut

    // Liste des langues disponibles
    public static final String LANGUAGE_FRENCH = "fr";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_ARABIC = "ar";
    public static final String LANGUAGE_SPANISH = "es";

    // Map des codes de langue vers les noms affichables
    private static final Map<String, String> LANGUAGE_NAMES = new HashMap<>();
    static {
        LANGUAGE_NAMES.put(LANGUAGE_FRENCH, "Français");
        LANGUAGE_NAMES.put(LANGUAGE_ENGLISH, "English");
        LANGUAGE_NAMES.put(LANGUAGE_ARABIC, "العربية");
        LANGUAGE_NAMES.put(LANGUAGE_SPANISH, "Español");
    }

    /**
     * Obtient la liste des langues disponibles avec leurs noms affichables
     * @return Liste de paires (code, nom) des langues disponibles
     */
    public static List<LanguageItem> getAvailableLanguages() {
        List<LanguageItem> languages = new ArrayList<>();

        // Ajouter l'option "Langue du téléphone"
        languages.add(new LanguageItem("auto", "Langue du téléphone"));

        // Ajouter les langues disponibles
        for (Map.Entry<String, String> entry : LANGUAGE_NAMES.entrySet()) {
            languages.add(new LanguageItem(entry.getKey(), entry.getValue()));
        }

        return languages;
    }

    /**
     * Obtient la langue actuellement sélectionnée
     * @param context Contexte de l'application
     * @return Code de la langue actuelle ou "auto" si la langue du téléphone est utilisée
     */
    public static String getCurrentLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Vérifier si l'utilisateur a choisi d'utiliser la langue du téléphone
        boolean useDeviceLanguage = prefs.getBoolean(PREF_USE_DEVICE_LANGUAGE, true); // Par défaut, utiliser la langue du téléphone

        if (useDeviceLanguage) {
            return "auto";
        } else {
            return prefs.getString(PREF_LANGUAGE_CODE, getDeviceLanguage());
        }
    }

    /**
     * Obtient la langue actuellement utilisée (pas le paramètre mais la langue réelle)
     * @param context Contexte de l'application
     * @return Code de langue effectivement utilisé
     */
    public static String getActiveLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Vérifier si l'utilisateur a choisi d'utiliser la langue du téléphone
        boolean useDeviceLanguage = prefs.getBoolean(PREF_USE_DEVICE_LANGUAGE, true);

        if (useDeviceLanguage) {
            return getDeviceLanguage();
        } else {
            return prefs.getString(PREF_LANGUAGE_CODE, DEFAULT_LANGUAGE);
        }
    }

    /**
     * Obtient la langue du périphérique
     * @return Code de langue du périphérique ou langue par défaut si non supportée
     */
    public static String getDeviceLanguage() {
        String deviceLanguage = Locale.getDefault().getLanguage();

        // Ajouter un log pour le débogage
        Log.d(TAG, "Langue du périphérique détectée: " + deviceLanguage);

        // Vérifier si la langue du périphérique est supportée
        if (LANGUAGE_NAMES.containsKey(deviceLanguage)) {
            return deviceLanguage;
        }

        // Sinon, retourner la langue par défaut
        return DEFAULT_LANGUAGE;
    }

    /**
     * Change la langue de l'application
     * @param activity Activité courante
     * @param languageCode Code de la langue à définir ou "auto" pour la langue du téléphone
     * @return true si la langue a été changée, false sinon
     */
    public static boolean setLanguage(Activity activity, String languageCode) {
        try {
            // Log pour déboguer
            Log.d(TAG, "Tentative de changement de langue vers: " + languageCode);

            SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Avant le changement
            String oldLanguage = getCurrentLanguage(activity);
            Log.d(TAG, "Ancienne langue: " + oldLanguage);

            if ("auto".equals(languageCode)) {
                // Utiliser la langue du téléphone
                editor.putBoolean(PREF_USE_DEVICE_LANGUAGE, true);
                String deviceLang = getDeviceLanguage();
                editor.putString(PREF_LANGUAGE_CODE, deviceLang); // Sauvegarde de la langue actuelle du téléphone comme référence
                Log.d(TAG, "Mode automatique sélectionné, langue du périphérique: " + deviceLang);
            } else {
                // Vérifier si la langue est supportée
                if (!LANGUAGE_NAMES.containsKey(languageCode)) {
                    Log.e(TAG, "Langue non supportée: " + languageCode);
                    return false;
                }

                // Utiliser la langue spécifiée
                editor.putBoolean(PREF_USE_DEVICE_LANGUAGE, false);
                editor.putString(PREF_LANGUAGE_CODE, languageCode);
                Log.d(TAG, "Mode langue spécifique sélectionné: " + languageCode);
            }

            // Appliquer les modifications
            editor.apply();

            // Vérifier si la langue a bien été changée
            String newLanguage = getCurrentLanguage(activity);
            Log.d(TAG, "Nouvelle langue après changement: " + newLanguage);

            // Mettre à jour la configuration locale
            String langToApply = "auto".equals(languageCode) ? getDeviceLanguage() : languageCode;
            Log.d(TAG, "Application de la configuration pour la langue: " + langToApply);
            updateResources(activity, langToApply);

            // Recréer l'activité pour appliquer la nouvelle langue
            Intent intent = activity.getIntent();
            // Ajout de drapeaux pour garantir un redémarrage complet
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.finish();
            activity.startActivity(intent);
            activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

            Log.d(TAG, "Activité recréée avec succès");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du changement de langue: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Initialise la langue de l'application au démarrage
     * À appeler dans l'activité de démarrage ou la classe Application
     * @param context Contexte de l'application
     */
    public static void initLanguage(Context context) {
        String languageToUse = getActiveLanguage(context);
        Log.d(TAG, "Initialisation de la langue: " + languageToUse);
        updateResources(context, languageToUse);
    }

    /**
     * Vérifie si la langue de l'appareil a changé depuis la dernière vérification
     * @param context Contexte de l'application
     * @return true si la langue du périphérique a changé et que l'app utilise la langue du téléphone
     */
    public static boolean hasDeviceLanguageChanged(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Vérifier si l'utilisateur a choisi d'utiliser la langue du téléphone
        boolean useDeviceLanguage = prefs.getBoolean(PREF_USE_DEVICE_LANGUAGE, true);

        if (useDeviceLanguage) {
            String savedDeviceLanguage = prefs.getString(PREF_LANGUAGE_CODE, DEFAULT_LANGUAGE);
            String currentDeviceLanguage = getDeviceLanguage();

            Log.d(TAG, "Vérification du changement de langue du périphérique: sauvegardée="
                    + savedDeviceLanguage + ", actuelle=" + currentDeviceLanguage);

            // Si la langue du périphérique a changé, mettre à jour la référence
            if (!currentDeviceLanguage.equals(savedDeviceLanguage)) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PREF_LANGUAGE_CODE, currentDeviceLanguage);
                editor.apply();
                Log.d(TAG, "Langue du périphérique modifiée, mise à jour dans les préférences");
                return true;
            }
        }

        return false;
    }

    /**
     * Met à jour les ressources avec la langue spécifiée
     * @param context Contexte de l'application
     * @param languageCode Code de langue
     */
    private static void updateResources(Context context, String languageCode) {
        Log.d(TAG, "Mise à jour des ressources avec la langue: " + languageCode);

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config);
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Pour les langues RTL comme l'arabe
        if (languageCode.equals(LANGUAGE_ARABIC)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLayoutDirection(locale);
                context.createConfigurationContext(config);
            }

            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO ||
                    AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                // Force la direction du layout pour les activités AppCompat
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.getDefaultNightMode());
            }
        }

        Log.d(TAG, "Ressources mises à jour avec succès");
    }

    /**
     * Force le rechargement de la langue actuelle
     * @param activity L'activité à recharger
     */
    public static void forceReload(Activity activity) {
        Log.d(TAG, "Forçage du rechargement de la langue");
        String currentLanguage = getActiveLanguage(activity);
        updateResources(activity, currentLanguage);

        // Recréer l'activité
        Intent intent = activity.getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.finish();
        activity.startActivity(intent);
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        Log.d(TAG, "Rechargement de la langue effectué");
    }

    /**
     * Classe pour représenter un élément de langue dans les listes et spinners
     */
    public static class LanguageItem {
        private final String code;
        private final String name;

        public LanguageItem(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }
}