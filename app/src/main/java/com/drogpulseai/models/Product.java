package com.drogpulseai.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Product implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("reference")
    private String reference;

    @SerializedName("label")
    private String label;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("photo_url")
    private String photoUrl;

    @SerializedName("photo_url2")
    private String photoUrl2;

    @SerializedName("photo_url3")
    private String photoUrl3;

    @SerializedName("barcode")
    private String barcode;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("price")
    private double price;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Additional fields
    @SerializedName("cout_de_revient_unitaire")
    private double coutDeRevientUnitaire;

    @SerializedName("prix_min_vente")
    private double prixMinVente;

    @SerializedName("prix_vente_conseille")
    private double prixVenteConseille;

    // Non-serialized fields (for local tracking)
    private boolean isAssociatedWithSupplier;
    private Double supplierPrice;
    private String supplierNotes;
    private String deliveryConditions;
    private Integer deliveryTime;
    private boolean isPrimarySupplier;
    private Integer associationId; // ID de l'association dans product_suppliers
    private transient boolean dirty = false;
    private transient long lastUpdated = 0;

    // Default constructor
    public Product() {
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getPhotoUrl2() {
        return photoUrl2;
    }

    public void setPhotoUrl2(String photoUrl2) {
        this.photoUrl2 = photoUrl2;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getPhotoUrl3() {
        return photoUrl3;
    }

    public void setPhotoUrl3(String photoUrl3) {
        this.photoUrl3 = photoUrl3;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters and Setters for additional fields
    public double getCoutDeRevientUnitaire() {
        return coutDeRevientUnitaire;
    }

    public void setCoutDeRevientUnitaire(double coutDeRevientUnitaire) {
        this.coutDeRevientUnitaire = coutDeRevientUnitaire;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public double getPrixMinVente() {
        return prixMinVente;
    }

    public void setPrixMinVente(double prixMinVente) {
        this.prixMinVente = prixMinVente;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    public double getPrixVenteConseille() {
        return prixVenteConseille;
    }

    public void setPrixVenteConseille(double prixVenteConseille) {
        this.prixVenteConseille = prixVenteConseille;
        this.dirty = true;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getter and Setter for dirty flag
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (dirty) {
            this.lastUpdated = System.currentTimeMillis();
        }
    }

    // Getter and Setter for lastUpdated
    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long timestamp) {
        this.lastUpdated = timestamp;
    }

    // Method to reset dirty state
    public void resetDirty() {
        this.dirty = false;
    }

    // Method to get all photo URLs as a list
    public List<String> getAllPhotoUrls() {
        List<String> urls = new ArrayList<>();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            urls.add(photoUrl);
        }
        if (photoUrl2 != null && !photoUrl2.isEmpty()) {
            urls.add(photoUrl2);
        }
        if (photoUrl3 != null && !photoUrl3.isEmpty()) {
            urls.add(photoUrl3);
        }
        return urls;
    }

    // Utility methods

    /**
     * Calculate gross margin percentage
     * @return Gross margin percentage or 0 if cost is 0
     */
    public double getMargePercent() {
        if (coutDeRevientUnitaire <= 0) {
            return 0;
        }

        double marge = price - coutDeRevientUnitaire;
        return (marge / coutDeRevientUnitaire) * 100;
    }

    /**
     * Check if selling price is below minimum recommended price
     * @return true if price is below minimum
     */
    public boolean isPrixInferieurMin() {
        return prixMinVente > 0 && price < prixMinVente;
    }

    /**
     * Calculate gross margin value
     * @return Gross margin in monetary value
     */
    public double getMargeBrute() {
        return price - coutDeRevientUnitaire;
    }

    /**
     * Check if selling price generates negative margin
     * @return true if price is below cost
     */
    public boolean hasNegativeMargin() {
        return coutDeRevientUnitaire > 0 && price < coutDeRevientUnitaire;
    }

    /**
     * Check if selling price is close to recommended price
     * @param tolerance Tolerance percentage (e.g., 5 for 5%)
     * @return true if price is within tolerance of recommended price
     */
    public boolean isPrixConforme(double tolerance) {
        if (prixVenteConseille <= 0) {
            return true; // No recommended price defined
        }

        double ecartPermis = prixVenteConseille * (tolerance / 100);
        return (price >= (prixVenteConseille - ecartPermis)) &&
                (price <= (prixVenteConseille + ecartPermis));
    }
    // Getters et setters

    // Getters et setters pour les nouveaux champs
    public boolean isAssociatedWithSupplier() {
        return isAssociatedWithSupplier;
    }

    public void setAssociatedWithSupplier(boolean associatedWithSupplier) {
        this.isAssociatedWithSupplier = associatedWithSupplier;
    }

    public Double getSupplierPrice() {
        return supplierPrice;
    }

    public void setSupplierPrice(Double supplierPrice) {
        this.supplierPrice = supplierPrice;
    }

    public String getSupplierNotes() {
        return supplierNotes;
    }

    public void setSupplierNotes(String supplierNotes) {
        this.supplierNotes = supplierNotes;
    }

    public String getDeliveryConditions() {
        return deliveryConditions;
    }

    public void setDeliveryConditions(String deliveryConditions) {
        this.deliveryConditions = deliveryConditions;
    }

    public Integer getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(Integer deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public boolean isPrimarySupplier() {
        return isPrimarySupplier;
    }

    public void setPrimarySupplier(boolean primarySupplier) {
        isPrimarySupplier = primarySupplier;
    }

    public Integer getAssociationId() {
        return associationId;
    }

    public void setAssociationId(Integer associationId) {
        this.associationId = associationId;
    }
    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", name='" + name + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}