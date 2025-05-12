package com.drogpulseai.api;

import androidx.annotation.Nullable;

import com.drogpulseai.models.Cart;
import com.drogpulseai.models.Contact;
import com.drogpulseai.models.Expense;
import com.drogpulseai.models.Product;
import com.drogpulseai.models.SupplierProductRequest;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.NetworkResult;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interface définissant les endpoints de l'API
 */
public interface ApiService {

    // ====== AUTHENTIFICATION ======
    @Headers({
            "Content-Type: application/json"
    })

    // Dans ApiService.java
    @GET("system/ping.php")
    Call<Map<String, Object>> pingServer();

    /**
     * Authentification utilisateur
     */
    @FormUrlEncoded
    @POST("auth/login.php")
    Call<Map<String, Object>> login(
            @Field("email") String email,
            @Field("password") String password
    );

    /**
     * Inscription d'un nouvel utilisateur
     */
    @POST("auth/register.php")
    Call<Map<String, Object>> register(@Body User user);


    // ====== GESTION DES CONTACTS ======

    /**
     * Récupérer tous les contacts d'un utilisateur
     */
    @GET("contacts/list.php")
    Call<List<Contact>> getContacts(@Query("user_id") int userId);

    /**
     * Recherche des contacts avec filtrage flexible
     * @param userId ID de l'utilisateur
     * @param query Terme de recherche (peut être vide pour tous les contacts)
     * @param type Type de contact (peut être null pour tous les types)
     * @return Liste des contacts correspondant aux critères
     */
    @GET("contacts/search.php")
    Call<List<Contact>> searchContacts(
            @Query("user_id") int userId,
            @Query("query") @Nullable String query,
            @Query("type") @Nullable String type
    );

    /**
     * Obtenir les détails d'un contact
     */
    @GET("contacts/details.php")
    Call<Contact> getContactDetails(@Query("id") int contactId);

    /**
     * Créer un nouveau contact
     */
    @POST("contacts/create.php")
    Call<Map<String, Object>> createContact(@Body Contact contact);

    /**
     * Mettre à jour un contact existant
     */
    @PUT("contacts/update.php")
    Call<Map<String, Object>> updateContact(@Body Contact contact);

    /**
     * Supprimer un contact
     */
    @DELETE("contacts/delete.php")
    Call<Map<String, Object>> deleteContact(@Query("id") int contactId);


    // ====== PRODUCTS ======

    /**
     * Get all products for a user
     */
    @GET("products/list.php")
    Call<List<Product>> getProducts(@Query("user_id") int userId);

    /**
     * Search products
     */
    @GET("products/search.php")
    Call<List<Product>> searchProducts(
            @Query("user_id") int userId,
            @Query("query") String query
    );

    /**
     * Get product details
     */
    @GET("products/details.php")
    Call<Product> getProductDetails(@Query("id") int productId);

    /**
     * Create product
     */
    @POST("products/create.php")
    Call<NetworkResult<Product>> createProduct(@Body Product product);

    /**
     * Create product with raw map
     */
    @POST("products/create.php")
    Call<NetworkResult<Product>> createProductRaw(@Body Map<String, Object> product);

    /**
     * Update product
     */
    @PUT("products/update.php")
    Call<NetworkResult<Product>> updateProduct(@Body Product product);

    /**
     * Update product with raw map
     */
    @PUT("products/update.php")
    Call<NetworkResult<Product>> updateProductRaw(@Body Map<String, Object> product);

    /**
     * Delete product
     */
    @DELETE("products/delete.php")
    Call<NetworkResult<Void>> deleteProduct(@Query("id") int productId);

    /**
     * Upload product photo
     */
    @Multipart
    @POST("products/upload_photo.php")
    Call<NetworkResult<String>> uploadProductPhoto(
            @Part MultipartBody.Part photo,
            @Part("user_id") RequestBody userId
    );

    /**
     * Associer un fournisseur à un produit
     */
    @POST("products/add_product_supplier.php")
    Call<NetworkResult<Void>> addProductSupplier(@Body SupplierProductRequest request);

    /**
     * Récupère les produits associés à un fournisseur spécifique
     */
    @GET("products/get_supplier_products.php")
    Call<List<Map<String, Object>>> getSupplierProducts(
            @Query("contact_id") int contactId,   // Correction du paramètre
            @Query("user_id") int userId          // Ajout du paramètre user_id
    );
    /**
     * Récupère les fournisseurs associés à un produit spécifique
     */
    @GET("products/get_product_suppliers.php")
    Call<Map<String, Object>> getProductSuppliers(
            @Query("product_id") int productId,
            @Query("only_active") boolean onlyActive
    );
    /**
     * Créer un nouveau panier
     */
    @POST("carts/create.php")
    Call<Map<String, Object>> createCart(@Body Map<String, Object> cartData);
    /**
     * Récupérer un panier par son ID
     */
    @GET("carts/get.php")
    Call<NetworkResult<Map<String, Object>>> getCart(@Query("id") int cartId);

    @GET("carts/get.php")
    Call<Object> getCartRaw(@Query("id") int cartId);

    /**
     * Lister les paniers d'un utilisateur
     */
    @GET("carts/list.php")
    Call<NetworkResult<Map<String, Object>>> getUserCarts(
            @Query("user_id") int userId,
            @Query("page") int page,
            @Query("limit") int limit
    );

    /**
     * Lister les paniers d'un contact
     */
    @GET("carts/list_by_contact.php")
    Call<Object> getContactCartsRaw(
            @Query("contact_id") int contactId,
            @Query("page") int page,
            @Query("limit") int limit
    );

    /**
     * Mettre à jour le statut d'un panier
     */
    @POST("carts/update_status.php")
    Call<NetworkResult<Map<String, Object>>> updateCartStatus(
            @Body Map<String, Object> statusData
    );


    /**
     * Récupérer tous les frais d'un utilisateur
     */
    @GET("expenses/list.php")
    Call<List<Expense>> getExpenses(@Query("user_id") int userId);

    /**
     * Obtenir les détails d'un frais
     */
    @GET("expenses/details.php")
    Call<Expense> getExpenseDetails(@Query("id") int expenseId);

    /**
     * Créer un nouveau frais
     */
    @POST("expenses/create.php")
    Call<NetworkResult<Expense>> createExpense(@Body Expense expense);

    /**
     * Mettre à jour un frais existant
     */
    @PUT("expenses/update.php")
    Call<NetworkResult<Expense>> updateExpense(@Body Expense expense);

    /**
     * Supprimer un frais
     */
    @DELETE("expenses/delete.php")
    Call<NetworkResult<Void>> deleteExpense(@Query("id") int expenseId);

    /**
     * Uploader une photo de justificatif
     */
    @Multipart
    @POST("expenses/upload_receipt.php")
    Call<NetworkResult<String>> uploadReceiptPhoto(
            @Part MultipartBody.Part photo,
            @Part("user_id") RequestBody userId
    );


    /**
     * Get filtered carts based on multiple criteria
     * @param filters Map of filter criteria
     */
    @POST("carts/filter.php") // Adjust endpoint as needed
    Call<Map<String, Object>> getFilteredCarts(@Body Map<String, Object> filters);

    /**
     * Get carts for a specific user with pagination
     * @param userId User ID
     * @param page Page number
     * @param limit Items per page
     */
    @GET("carts/list.php")
    Call<Map<String, Object>> getUserCartsFiltred(
            @Query("user_id") int userId,
            @Query("page") int page,
            @Query("limit") int limit
    );
}