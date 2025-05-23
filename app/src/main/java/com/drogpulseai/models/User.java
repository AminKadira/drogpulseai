package com.drogpulseai.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class User implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("nom")
    private String nom;

    @SerializedName("prenom")
    private String prenom;

    @SerializedName("telephone")
    private String telephone;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("type_user")
    private String typeUser;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    // Types d'utilisateurs disponibles
    public static final String TYPE_ADMIN = "Admin";
    public static final String TYPE_COMMERCIAL = "Commercial";
    public static final String TYPE_VENDEUR = "Vendeur";
    public static final String TYPE_INVITE = "Invité";
    public static final String TYPE_MANAGER = "Manager";

    // Array pour faciliter l'utilisation dans les spinners
    public static final String[] USER_TYPES = {
            TYPE_COMMERCIAL,  // Par défaut en premier
            TYPE_ADMIN,
            TYPE_MANAGER,
            TYPE_VENDEUR,
            TYPE_INVITE
    };

    // Constructeur par défaut
    public User() {
        this.typeUser = TYPE_COMMERCIAL; // Valeur par défaut
    }

    // Constructeur pour l'inscription
    public User(String nom, String prenom, String telephone, String email,
                String password, String typeUser, double latitude, double longitude) {
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.email = email;
        this.password = password;
        this.typeUser = typeUser != null ? typeUser : TYPE_COMMERCIAL;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Constructeur sans type (rétrocompatibilité)
    public User(String nom, String prenom, String telephone, String email,
                String password, double latitude, double longitude) {
        this(nom, prenom, telephone, email, password, TYPE_COMMERCIAL, latitude, longitude);
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTypeUser() { return typeUser; }
    public void setTypeUser(String typeUser) {
        this.typeUser = typeUser != null ? typeUser : TYPE_COMMERCIAL;
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // Méthodes utilitaires
    public String getFullName() {
        return prenom + " " + nom;
    }

    public String getDisplayInfo() {
        return getFullName() + " (" + typeUser + ")";
    }

    // Vérification des permissions
    public boolean isAdmin() {
        return TYPE_ADMIN.equals(typeUser);
    }

    public boolean isManager() {
        return TYPE_MANAGER.equals(typeUser);
    }

    public boolean canManageThisOption() {
        return isAdmin() || isManager() || TYPE_COMMERCIAL.equals(typeUser);
    }

    public boolean canViewReports() {
        return isAdmin() || isManager();
    }

    // Pour l'affichage en debug
    @Override
    public String toString() {
        return prenom + " " + nom + " (" + email + " - " + typeUser + ")";
    }
}