package com.drogpulseai.models;

/**
 * Classe représentant un élément de panier avec un produit et sa quantité
 */
public class ProductCartItem {
    private final Product product;
    private final int quantity;

    public ProductCartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getTotalPrice() {
        return product.getPrice() * quantity;
    }
}