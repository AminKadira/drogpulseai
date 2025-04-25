package com.drogpulseai.api;

import com.drogpulseai.models.Contact;
import com.drogpulseai.models.Product;
import com.drogpulseai.models.User;

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


    // ====== GESTION DES PRODUITS ======

    /**
     * Récupérer tous les produits d'un utilisateur
     */
    @GET("products/list.php")
    Call<List<Product>> getProducts(@Query("user_id") int userId);

    /**
     * Rechercher des produits
     */
    @GET("products/search.php")
    Call<List<Product>> searchProducts(
            @Query("user_id") int userId,
            @Query("query") String query
    );

    /**
     * Obtenir les détails d'un produit
     */
    @GET("products/details.php")
    Call<Product> getProductDetails(@Query("id") int productId);

    /**
     * Créer un nouveau produit
     */
    @POST("products/create.php")
    Call<Map<String, Object>> createProduct(@Body Product product);

    /**
     * Créer un nouveau produit avec Map
     */
    @POST("products/create.php")
    Call<Map<String, Object>> createProductRaw(@Body Map<String, Object> product);

    /**
     * Mettre à jour un produit existant
     */
    @PUT("products/update.php")
    Call<Map<String, Object>> updateProduct(@Body Product product);

    /**
     * Mettre à jour un produit existant avec Map
     */
    @PUT("products/update.php")
    Call<Map<String, Object>> updateProduct(@Body Map<String, Object> product);

    /**
     * Supprimer un produit
     */
    @DELETE("products/delete.php")
    Call<Map<String, Object>> deleteProduct(@Query("id") int productId);

    /**
     * Upload d'une photo de produit
     */
    @Multipart
    @POST("products/upload_photo.php")
    Call<Map<String, Object>> uploadProductPhoto(
            @Part MultipartBody.Part photo,
            @Part("user_id") RequestBody userId
    );
}