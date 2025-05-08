package com.drogpulseai.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

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

    // Nouveaux champs
    @SerializedName("cout_de_revient_unitaire")
    private double coutDeRevientUnitaire;

    @SerializedName("prix_min_vente")
    private double prixMinVente;

    @SerializedName("prix_vente_conseille")
    private double prixVenteConseille;

    // Champs non sérialisés (pour le suivi local)
    private transient boolean dirty = false;
    private transient long lastUpdated = 0;

    // Constructeur par défaut
    public Product() {
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters et Setters
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

    // Getters et Setters pour les nouveaux champs
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

    // Getter et Setter pour l'attribut dirty
    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (dirty) {
            this.lastUpdated = System.currentTimeMillis();
        }
    }

    // Getter et Setter pour lastUpdated
    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long timestamp) {
        this.lastUpdated = timestamp;
    }

    // Méthode pour réinitialiser l'état dirty
    public void resetDirty() {
        this.dirty = false;
    }

    // Méthodes utilitaires

    /**
     * Calcule la marge brute en pourcentage
     * @return Le pourcentage de marge brute ou 0 si le coût de revient est 0
     */
    public double getMargePercent() {
        if (coutDeRevientUnitaire <= 0) {
            return 0;
        }

        double marge = price - coutDeRevientUnitaire;
        return (marge / coutDeRevientUnitaire) * 100;
    }

    /**
     * Vérifie si le prix de vente est en dessous du prix minimum recommandé
     * @return true si le prix est inférieur au prix minimum
     */
    public boolean isPrixInferieurMin() {
        return prixMinVente > 0 && price < prixMinVente;
    }

    /**
     * Calcule la marge brute en valeur
     * @return La marge brute en valeur monétaire
     */
    public double getMargeBrute() {
        return price - coutDeRevientUnitaire;
    }

    /**
     * Vérifie si le prix de vente génère une marge négative
     * @return true si le prix est inférieur au coût de revient
     */
    public boolean hasNegativeMargin() {
        return coutDeRevientUnitaire > 0 && price < coutDeRevientUnitaire;
    }

    /**
     * Vérifie si le prix de vente est conforme au prix conseillé
     * @param tolerance Pourcentage de tolérance (ex: 5 pour 5%)
     * @return true si le prix est proche du prix conseillé dans la limite de la tolérance
     */
    public boolean isPrixConforme(double tolerance) {
        if (prixVenteConseille <= 0) {
            return true; // Pas de prix conseillé défini
        }

        double ecartPermis = prixVenteConseille * (tolerance / 100);
        return (price >= (prixVenteConseille - ecartPermis)) &&
                (price <= (prixVenteConseille + ecartPermis));
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