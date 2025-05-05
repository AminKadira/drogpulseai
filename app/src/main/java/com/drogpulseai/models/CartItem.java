package com.drogpulseai.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class CartItem implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("cart_id")
    private int cartId;

    @SerializedName("product_id")
    private int productId;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("price")
    private double price;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Champs additionnels de produit inclus dans les résultats API
    @SerializedName("product_reference")
    private String productReference;

    @SerializedName("product_name")
    private String productName;

    @SerializedName("product_label")
    private String productLabel;

    // Constructeur par défaut
    public CartItem() {
    }

    // Constructeur pour créer un élément de panier à partir d'un produit
    public CartItem(Product product, int quantity) {
        this.productId = product.getId();
        this.quantity = quantity;
        this.price = product.getPrice();
        this.productReference = product.getReference();
        this.productName = product.getName();
        this.productLabel = product.getLabel();
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCartId() { return cartId; }
    public void setCartId(int cartId) { this.cartId = cartId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getProductReference() { return productReference; }
    public void setProductReference(String productReference) { this.productReference = productReference; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductLabel() { return productLabel; }
    public void setProductLabel(String productLabel) { this.productLabel = productLabel; }

    // Méthodes utilitaires
    public double getTotalPrice() {
        return price * quantity;
    }

    @Override
    public String toString() {
        return quantity + " x " + productName + " (" + productReference + ")";
    }
}