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

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    // Constructeur par défaut
    public User() {
    }

    // Constructeur pour l'inscription
    public User(String nom, String prenom, String telephone, String email, String password,double latitude, double longitude) {
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.email = email;
        this.password=password;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // Pour l'affichage en debug
    @Override
    public String toString() {
        return prenom + " " + nom + " (" + email + ")";
    }

    // Pour l'affichage dans l'interface
    public String getFullName() {
        return prenom + " " + nom;
    }
}