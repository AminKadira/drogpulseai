package com.drogpulseai.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Cart implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("contact_id")
    private int contactId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("status")
    private String status;

    @SerializedName("notes")
    private String notes;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("contact_nom")
    private String contactNom;

    @SerializedName("contact_prenom")
    private String contactPrenom;

    @SerializedName("contact_telephone")
    private String contactTelephone;

    @SerializedName("contact_email")
    private String contactEmail;

    @SerializedName("items")
    private List<CartItem> items;

    @SerializedName("total_quantity")
    private int totalQuantity;

    @SerializedName("total_amount")
    private double totalAmount;

    // Constructeur par défaut
    public Cart() {
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getContactId() { return contactId; }
    public void setContactId(int contactId) { this.contactId = contactId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    // Suite des getters et setters

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getContactNom() { return contactNom; }
    public void setContactNom(String contactNom) { this.contactNom = contactNom; }

    public String getContactPrenom() { return contactPrenom; }
    public void setContactPrenom(String contactPrenom) { this.contactPrenom = contactPrenom; }

    public String getContactTelephone() { return contactTelephone; }
    public void setContactTelephone(String contactTelephone) { this.contactTelephone = contactTelephone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    // Méthodes utilitaires
    public String getContactFullName() {
        return contactPrenom + " " + contactNom;
    }

    public boolean isPending() {
        return "pending".equals(status);
    }

    public boolean isConfirmed() {
        return "confirmed".equals(status);
    }

    public boolean isCancelled() {
        return "cancelled".equals(status);
    }

    @Override
    public String toString() {
        return "Panier #" + id + " - " + getContactFullName() + " (" + items.size() + " produits)";
    }
}