package com.drogpulseai.database.converter;

import com.drogpulseai.database.ProductEntity;
import com.drogpulseai.models.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductConverter {

    public static ProductEntity fromProduct(Product product) {
        ProductEntity entity = new ProductEntity(
                product.getId(),
                product.getReference(),
                product.getLabel(),
                product.getName(),
                product.getDescription(),
                product.getPhotoUrl(),
                product.getBarcode(),
                product.getQuantity(),
                product.getPrice(),
                product.getUserId()
        );

        // Marquer comme "dirty" si c'est une nouvelle entité ou une modification locale
        entity.setDirty(true);
        return entity;
    }

    public static Product toProduct(ProductEntity entity) {
        Product product = new Product();
        product.setId(entity.getId());
        product.setReference(entity.getReference());
        product.setLabel(entity.getLabel());
        product.setName(entity.getName());
        product.setDescription(entity.getDescription());
        product.setPhotoUrl(entity.getPhotoUrl());
        product.setBarcode(entity.getBarcode());
        product.setQuantity(entity.getQuantity());
        product.setPrice(entity.getPrice());
        product.setUserId(entity.getUserId());
        return product;
    }

    public static List<Product> toProductList(List<ProductEntity> entities) {
        List<Product> products = new ArrayList<>();
        for (ProductEntity entity : entities) {
            products.add(toProduct(entity));
        }
        return products;
    }
}