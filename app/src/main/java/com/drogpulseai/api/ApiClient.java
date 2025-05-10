package com.drogpulseai.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Base URL will be retrieved from configuration
    private static String BASE_URL;
    private static Retrofit retrofit = null;
    private static final int CONNECT_TIMEOUT = 30; // seconds
    private static final int READ_TIMEOUT = 30; // seconds
    private static final int WRITE_TIMEOUT = 30; // seconds

    // Getter for base URL
    public static String getBaseUrl() {
        return BASE_URL;
    }

    // Create OkHttpClient with better configuration
    private static OkHttpClient createOkHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor);

        // Add request interceptor for debugging
        httpClient.addInterceptor(chain -> {
            Request request = chain.request();
            android.util.Log.d("ApiClient", "Making request to: " + request.url());
            return chain.proceed(request);
        });

        return httpClient.build();
    }

    // Configure Gson to handle incorrect response formats
    private static Gson createGson() {
        return new GsonBuilder()
                .setLenient() // Be lenient with malformed JSON
                .setDateFormat("yyyy-MM-dd") // Format compatible avec votre API
                .registerTypeAdapter(String.class, new StringAdapter())
                .registerTypeAdapter(Integer.class, new SafeIntegerAdapter())
                .registerTypeAdapter(Double.class, new SafeDoubleAdapter())
                .registerTypeAdapter(Boolean.class, new SafeBooleanAdapter())
                .create();
    }

    public static <T> T createService(Class<T> serviceClass, String baseUrl) {
        // Update base URL if provided
        if (baseUrl != null && !baseUrl.isEmpty()) {
            BASE_URL = baseUrl;
        } else if (BASE_URL == null || BASE_URL.isEmpty()) {
            // Get from config if not set
            BASE_URL = com.drogpulseai.utils.Config.getApiBaseUrl();
        }

        // Create Retrofit instance
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(createGson()))
                .client(createOkHttpClient())
                .build();

        return retrofit.create(serviceClass);
    }

    public static <T> T createService(Class<T> serviceClass) {
        return createService(serviceClass, null);
    }

    // Method to obtain the default ApiService instance
    public static ApiService getApiService() {
        return createService(ApiService.class);
    }

    // Custom type adapter for Strings to handle null values
    private static class StringAdapter extends TypeAdapter<String> {
        @Override
        public String read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return ""; // Return empty string instead of null
            }
            try {
                return reader.nextString();
            } catch (Exception e) {
                // If we can't read as a string, skip the value
                reader.skipValue();
                return "";
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

    // Custom type adapter for Integer to handle incorrect types
    private static class SafeIntegerAdapter extends TypeAdapter<Integer> {
        @Override
        public Integer read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return 0;
            }

            try {
                if (reader.peek() == JsonToken.NUMBER) {
                    return (int) reader.nextDouble();
                } else if (reader.peek() == JsonToken.STRING) {
                    try {
                        return Integer.parseInt(reader.nextString());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                } else {
                    reader.skipValue();
                    return 0;
                }
            } catch (Exception e) {
                reader.skipValue();
                return 0;
            }
        }

        @Override
        public void write(JsonWriter writer, Integer value) throws IOException {
            if (value == null) {
                writer.nullValue();
                return;
            }
            writer.value(value);
        }
    }

    // Custom type adapter for Double to handle incorrect types
    private static class SafeDoubleAdapter extends TypeAdapter<Double> {
        @Override
        public Double read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return 0.0;
            }

            try {
                if (reader.peek() == JsonToken.NUMBER) {
                    return reader.nextDouble();
                } else if (reader.peek() == JsonToken.STRING) {
                    try {
                        return Double.parseDouble(reader.nextString());
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                } else {
                    reader.skipValue();
                    return 0.0;
                }
            } catch (Exception e) {
                reader.skipValue();
                return 0.0;
            }
        }

        @Override
        public void write(JsonWriter writer, Double value) throws IOException {
            if (value == null) {
                writer.nullValue();
                return;
            }
            writer.value(value);
        }
    }

    // Custom type adapter for Boolean to handle incorrect types
    private static class SafeBooleanAdapter extends TypeAdapter<Boolean> {
        @Override
        public Boolean read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return false;
            }

            try {
                if (reader.peek() == JsonToken.BOOLEAN) {
                    return reader.nextBoolean();
                } else if (reader.peek() == JsonToken.STRING) {
                    String value = reader.nextString();
                    return "true".equalsIgnoreCase(value) || "1".equals(value);
                } else if (reader.peek() == JsonToken.NUMBER) {
                    return reader.nextInt() == 1;
                } else {
                    reader.skipValue();
                    return false;
                }
            } catch (Exception e) {
                reader.skipValue();
                return false;
            }
        }

        @Override
        public void write(JsonWriter writer, Boolean value) throws IOException {
            if (value == null) {
                writer.nullValue();
                return;
            }
            writer.value(value);
        }
    }
}