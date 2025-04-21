package com.drogpulseai.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.drogpulseai.models.User;
import com.google.gson.Gson;

/**
 * Gestionnaire de session utilisateur avec SharedPreferences
 */
public class SessionManager {
    // Constantes pour les SharedPreferences
    private static final String PREF_NAME = "DrogPulseAISession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_DATA = "userData";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    /**
     * Constructeur avec initialisation des SharedPreferences
     */
    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * Enregistre une session utilisateur
     */
    public void createSession(User user) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);

        // Serialisation de l'objet User en JSON
        Gson gson = new Gson();
        String userJson = gson.toJson(user);
        editor.putString(KEY_USER_DATA, userJson);

        // Sauvegarde synchrone
        editor.commit();
    }

    /**
     * Vérifie si un utilisateur est connecté
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Récupère les données de l'utilisateur connecté
     */
    public User getUser() {
        String userJson = prefs.getString(KEY_USER_DATA, null);
        if (userJson == null) {
            return null;
        }

        Gson gson = new Gson();
        return gson.fromJson(userJson, User.class);
    }

    /**
     * Déconnecte l'utilisateur en effaçant la session
     */
    public void logout() {
        editor.clear();
        editor.commit();
    }
}