package com.drogpulseai.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://192.168.1.15/drogpulseai_Api/api/"; // Pour l'émulateur Android

    // Configuration Retrofit avec OkHttpClient pour le timeout et le logging
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)  // Augmenté à 60 secondes
            .readTimeout(60, TimeUnit.SECONDS)     // Augmenté à 60 secondes
            .writeTimeout(60, TimeUnit.SECONDS)    // Augmenté à 60 secondes
            .build();


    static Gson gson = new GsonBuilder()
            .setLenient()
            .create();
    // Instance Retrofit unique pour toute l'application (singleton)
    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build();

    // Méthode pour obtenir une instance du service API
    public static <T> T createService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }

    // Méthode pour obtenir l'instance par défaut du service ApiService
    public static ApiService getApiService() {
        return createService(ApiService.class);
    }
}