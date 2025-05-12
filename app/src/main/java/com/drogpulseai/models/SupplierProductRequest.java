package com.drogpulseai.models;

import com.google.gson.annotations.SerializedName;

public class SupplierProductRequest {
    @SerializedName("product_id")
    private int productId;

    @SerializedName("contact_id")
    private int contactId;

    @SerializedName("is_primary")
    private boolean isPrimary;

    @SerializedName("price")
    private double price;

    @SerializedName("notes")
    private String notes;

    @SerializedName("delivery_conditions")
    private String deliveryConditions;

    @SerializedName("delivery_time")
    private Integer deliveryTime;

    // Getters et setters
    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getContactId() {
        return contactId;
    }

    public void setContactId(int contactId) {
        this.contactId = contactId;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
}