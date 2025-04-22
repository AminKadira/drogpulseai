package com.drogpulseai.api;

import com.drogpulseai.utils.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // L'URL de base sera récupérée depuis la configuration
    private static String BASE_URL;
    private static Retrofit retrofit = null;

    // Ajouter cette méthode getter
    public static String getBaseUrl() {
        return BASE_URL;
    }
    // Configuration Retrofit avec OkHttpClient pour le timeout et le logging
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)  // Augmenté à 60 secondes
            .readTimeout(60, TimeUnit.SECONDS)     // Augmenté à 60 secondes
            .writeTimeout(60, TimeUnit.SECONDS)    // Augmenté à 60 secondes
            .build();


    // Configuration de Gson pour mieux gérer les erreurs de format
    static Gson gson = new GsonBuilder()
            .setLenient()
            .registerTypeAdapter(String.class, new StringAdapter())
            .create();

    public static <T> T createService(Class<T> serviceClass) {
        if (retrofit == null) {
            // Récupérer l'URL de base depuis la configuration
            BASE_URL = Config.getApiBaseUrl();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build();
        }
        return retrofit.create(serviceClass);
    }

    // Méthode pour obtenir l'instance par défaut du service ApiService
    public static ApiService getApiService() {
        return createService(ApiService.class);
    }


    // Adaptateur pour gérer les chaînes de caractères non valides
    private static class StringAdapter extends TypeAdapter<String> {
        @Override
        public String read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }
            try {
                return reader.nextString();
            } catch (Exception e) {
                // Si nous ne pouvons pas lire comme une chaîne, lisons comme un token quelconque
                reader.skipValue();
                return null;
            }
        }

        @Override
        public void write(JsonWriter writer, String value) throws IOException {
            if (value == null) {
                writer.nullValue();
                return;
            }
            writer.value(value);
        }
    }
}