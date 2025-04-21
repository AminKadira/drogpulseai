package com.drogpulseai.api;

import com.drogpulseai.models.Contact;
import com.drogpulseai.models.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interface définissant les endpoints de l'API
 */
public interface ApiService {

    // ====== AUTHENTIFICATION ======

    // Dans ApiService.java
    @GET("ping.php")
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
}