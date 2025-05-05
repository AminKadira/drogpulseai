package com.drogpulseai.api;

import com.drogpulseai.models.Cart;
import com.drogpulseai.models.Contact;
import com.drogpulseai.models.Product;
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
     * Rechercher des contacts
     */
    @GET("contacts/search.php")
    Call<List<Contact>> searchContacts(
            @Query("user_id") int userId,
            @Query("query") String query
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
     * Créer un nouveau panier
     */
    @POST("carts/create.php")
    Call<NetworkResult<Cart>> createCart(@Body Map<String, Object> cartData);

    /**
     * Récupérer un panier par son ID
     */
    @GET("carts/get.php")
    Call<NetworkResult<Cart>> getCart(@Query("id") int cartId);

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
    Call<NetworkResult<Map<String, Object>>> getContactCarts(
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
}