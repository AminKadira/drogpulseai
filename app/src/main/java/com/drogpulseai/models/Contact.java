package com.drogpulseai.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Contact implements Serializable {
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

    @SerializedName("notes")
    private String notes;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("user_id")
    private int userId;


    public Contact(String nom, String prenom, String telephone, String email,
                   String notes, double latitude, double longitude, int userId) {
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.email = email;
        this.notes = notes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.userId = userId;
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

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    // Pour l'affichage en debug
    @Override
    public String toString() {
        return prenom + " " + nom + " (" + telephone + ")";
    }

    // Pour l'affichage dans l'interface
    public String getFullName() {
        return prenom + " " + nom;
    }
}