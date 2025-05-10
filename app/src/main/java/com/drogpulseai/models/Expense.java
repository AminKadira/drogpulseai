package com.drogpulseai.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Date;

public class Expense implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("type")
    private String type;  // Type de frais (carburant, hôtel, etc.)

    @SerializedName("amount")
    private double amount;  // Montant du frais

    @SerializedName("date")
    private Date date;  // Date du frais

    @SerializedName("description")
    private String description;  // Description/notes

    @SerializedName("receipt_photo_url")
    private String receiptPhotoUrl;  // URL de la photo du justificatif

    @SerializedName("user_id")
    private int userId;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Constructeur par défaut
    public Expense() {
    }

    // Constructeur pour création
    public Expense(String type, double amount, Date date, String description,
                   String receiptPhotoUrl, int userId) {
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.receiptPhotoUrl = receiptPhotoUrl;
        this.userId = userId;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReceiptPhotoUrl() { return receiptPhotoUrl; }
    public void setReceiptPhotoUrl(String receiptPhotoUrl) {
        this.receiptPhotoUrl = receiptPhotoUrl;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return type + " - " + amount + " MAD (" + date + ")";
    }
}