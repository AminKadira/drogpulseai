package com.drogpulseai.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.drogpulseai.models.Product;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository for Product data with local caching
 * A simplified implementation using SharedPreferences before full Room implementation
 */
public class ProductRepository {

    private static final String TAG = "ProductRepository";
    private static final String PREF_NAME = "product_cache";
    private static final String KEY_PRODUCTS = "products";

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private final Executor executor;

    public ProductRepository(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Insert or update a product in the local cache
     */
    public void insertOrUpdateProduct(Product product) {
        if (product == null) {
            return;
        }

        executor.execute(() -> {
            try {
                // Get current products
                List<Product> products = getAllProducts();

                // Find existing product
                boolean found = false;
                for (int i = 0; i < products.size(); i++) {
                    if (products.get(i).getId() == product.getId()) {
                        products.set(i, product);
                        found = true;
                        break;
                    }
                }

                // Add if not found
                if (!found) {
                    products.add(product);
                }

                // Save updated list
                saveProducts(products);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting product", e);
            }
        });
    }

    /**
     * Delete a product from the local cache
     */
    public void deleteProduct(int productId) {
        executor.execute(() -> {
            try {
                // Get current products
                List<Product> products = getAllProducts();

                // Remove product with matching ID
                products.removeIf(p -> p.getId() == productId);

                // Save updated list
                saveProducts(products);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting product", e);
            }
        });
    }

    /**
     * Get a product by ID from local cache
     */
    public Product getProductById(int productId) {
        try {
            List<Product> products = getAllProducts();

            for (Product product : products) {
                if (product.getId() == productId) {
                    return product;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting product", e);
        }

        return null;
    }

    /**
     * Get all products from local cache
     */
    public List<Product> getAllProducts() {
        String json = sharedPreferences.getString(KEY_PRODUCTS, null);

        if (json == null) {
            return new ArrayList<>();
        }

        try {
            Type type = new TypeToken<List<Product>>(){}.getType();
            return gson.fromJson(json, type);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing products", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get products for a specific user
     */
    public List<Product> getProductsForUser(int userId) {
        List<Product> allProducts = getAllProducts();
        List<Product> userProducts = new ArrayList<>();

        for (Product product : allProducts) {
            if (product.getUserId() == userId) {
                userProducts.add(product);
            }
        }

        return userProducts;
    }

    /**
     * Save products to SharedPreferences
     */
    private void saveProducts(List<Product> products) {
        try {
            String json = gson.toJson(products);
            sharedPreferences.edit().putString(KEY_PRODUCTS, json).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving products", e);
        }
    }

    /**
     * Clear all cached products
     */
    public void clearCache() {
        executor.execute(() -> {
            sharedPreferences.edit().clear().apply();
        });
    }

    /**
     * Obtenir l'ID local le plus bas (utilisé pour générer des IDs temporaires)
     * Note: Les IDs temporaires sont négatifs pour les distinguer des IDs serveur positifs
     * @return L'ID local le plus bas, ou -1 si aucun ID local n'existe
     */
    public int getLowestLocalId() {
        List<Product> products = getAllProducts();

        int lowestId = -1; // Valeur par défaut si aucun ID temporaire n'existe encore

        for (Product product : products) {
            int productId = product.getId();

            // Nous ne considérons que les IDs négatifs (locaux)
            if (productId < 0 && (productId < lowestId || lowestId == -1)) {
                lowestId = productId;
            }
        }

        // Si aucun ID local n'a été trouvé, retourner -1
        return lowestId == -1 ? -1 : lowestId;
    }
}