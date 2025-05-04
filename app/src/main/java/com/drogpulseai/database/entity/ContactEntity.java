package com.drogpulseai.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class ContactEntity {
    @PrimaryKey(autoGenerate = true)
    private long roomId; // ID local pour Room

    private int id; // ID du serveur
    private String nom;
    private String prenom;
    private String telephone;
    private String email;
    private String notes;
    private double latitude;
    private double longitude;
    private int userId;
    private boolean isSynced;
    private boolean isDeleted;

    // Constructeur
    public ContactEntity(int id, String nom, String prenom, String telephone,
                         String email, String notes, double latitude,
                         double longitude, int userId) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.email = email;
        this.notes = notes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.userId = userId;
        this.isSynced = id > 0; // Si id > 0, alors déjà synchronisé
        this.isDeleted = false;
    }

    // Getters et Setters
    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

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

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
}