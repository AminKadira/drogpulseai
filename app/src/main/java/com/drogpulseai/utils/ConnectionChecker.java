package com.drogpulseai.utils;

import android.content.Context;

import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConnectionChecker {

    private final Context context;
    private final OnConnectionCheckListener listener;

    public interface OnConnectionCheckListener {
        void onConnectionSuccess();
        void onConnectionFailure(String errorMessage);
    }

    public ConnectionChecker(Context context, OnConnectionCheckListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void checkConnection() {
        // Créer le service API
        ApiService apiService = ApiClient.getApiService();

        // Faire l'appel API
        apiService.pingServer().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> result = response.body();
                    boolean success = (boolean) result.get("success");

                    if (success) {
                        if (listener != null) {
                            listener.onConnectionSuccess();
                        }
                    } else {
                        String message = (String) result.get("message");
                        if (listener != null) {
                            listener.onConnectionFailure(message);
                        }
                    }
                } else {
                    if (listener != null) {
                        listener.onConnectionFailure("Erreur serveur : " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (listener != null) {
                    listener.onConnectionFailure("Erreur réseau : " + t.getMessage());
                }
            }
        });
    }

    public String showConnectionDialog(boolean isConnected, String errorMessage) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setTitle("Statut de connexion");

        if (isConnected) {
            return "Connexion ok";
//            builder.setMessage("Connexion OK");
//            builder.setIcon(android.R.drawable.ic_dialog_info);
        } else {
            return "connexion ko";
//            builder.setMessage("Connexion KO\n" + errorMessage);
//            builder.setIcon(android.R.drawable.ic_dialog_alert);
        }

        //builder.setPositiveButton("OK", null);
//        builder.show();
    }
}