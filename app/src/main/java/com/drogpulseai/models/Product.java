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

    @SerializedName("user_id")
    private int user_id;

    public Product(){}

    // Constructeur pour création
    public Product(String reference, String label, String name, String description,
                   String photoUrl, String barcode, int quantity, int user_id) {
        this.reference = reference;
        this.label = label;
        this.name = name;
        this.description = description;
        if(photoUrl.isEmpty()){ this.photoUrl= "C:/Desktop/picTestjpeg.jpg";}else{this.photoUrl = photoUrl;}
        this.barcode = barcode;
        this.quantity = quantity;
        this.user_id = user_id;
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
    public void setPhotoUrl(String photoUrl) {
        if(photoUrl.isEmpty()){ this.photoUrl= "C:/Desktop/picTestjpeg.jpg";}
        else{this.photoUrl = photoUrl;}
    }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getUserId() { return user_id; }
    public void setUserId(int userId) { this.user_id = userId; }

    @Override
    public String toString() {
        return reference + " - " + name;
    }

}