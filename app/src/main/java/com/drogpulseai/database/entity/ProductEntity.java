package com.drogpulseai.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class ProductEntity {
    @PrimaryKey
    private int id;
    private String reference;
    private String label;
    private String name;
    private String description;
    private String photoUrl;
    private String photoUrl2;
    private String photoUrl3;
    private String barcode;
    private int quantity;
    private double price;
    private int userId;
    private long lastUpdated;
    private boolean isDirty; // Indique si ce produit a des modifications locales non synchronisées

    // Constructeur
    public ProductEntity(int id, String reference, String label, String name,
                         String description,
                         String photoUrl, String photoUrl2, String photoUrl3,
                         String barcode, int quantity, double price, int userId) {
        this.id = id;
        this.reference = reference;
        this.label = label;
        this.name = name;
        this.description = description;
        this.photoUrl = photoUrl;
        this.photoUrl2 = photoUrl2;
        this.photoUrl3 = photoUrl3;
        this.barcode = barcode;
        this.quantity = quantity;
        this.price = price;
        this.userId = userId;
        this.lastUpdated = System.currentTimeMillis();
        this.isDirty = false;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public boolean isDirty() { return isDirty; }
    public void setDirty(boolean dirty) { isDirty = dirty; }

    public void setPhotoUrl2(String photoUrl2) {
        this.photoUrl2 = photoUrl2;
    }

    public String getPhotoUrl2() {
        return photoUrl2;
    }

    public void setPhotoUrl3(String photoUrl3) {
        this.photoUrl3 = photoUrl3;
    }

    public String getPhotoUrl3() {
        return photoUrl3;
    }
}
