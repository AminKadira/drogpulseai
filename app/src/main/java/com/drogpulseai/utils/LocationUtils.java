package com.drogpulseai.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Utilitaire pour obtenir la localisation GPS
 */
public class LocationUtils {

    // Callback pour recevoir la localisation
    public interface LocationCallback {
        void onLocationReceived(double latitude, double longitude);
        void onLocationError(String message);
    }

    private final Activity activity;
    private final LocationCallback callback;
    private final FusedLocationProviderClient fusedLocationClient;
    private com.google.android.gms.location.LocationCallback locationCallback;

    /**
     * Constructeur
     */
    public LocationUtils(Activity activity, LocationCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
    }

    /**
     * Demande la localisation actuelle
     */
    public void getCurrentLocation() {
        // Vérifier si les permissions sont accordées
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {

            // Demander les permissions
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    100);
            return;
        }

        // Configurer la requête de localisation
        LocationRequest locationRequest = new LocationRequest.Builder(5000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        // Callback pour recevoir les mises à jour de localisation
        locationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // Arrêter les mises à jour après avoir reçu un résultat
                stopLocationUpdates();

                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        callback.onLocationReceived(location.getLatitude(), location.getLongitude());
                        return;
                    }
                }

                callback.onLocationError("Impossible d'obtenir la localisation");
            }
        };

        // Demander les mises à jour de localisation
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        // Essayer d'obtenir la dernière localisation connue en premier
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                callback.onLocationReceived(location.getLatitude(), location.getLongitude());
                stopLocationUpdates();
            }
        }).addOnFailureListener(e -> {
            // La dernière localisation n'a pas pu être obtenue, les mises à jour sont déjà demandées
        });
    }

    /**
     * Arrêter les mises à jour de localisation
     */
    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    /**
     * Traiter le résultat de la demande de permission
     */
    public void handlePermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                callback.onLocationError("Permission de localisation refusée");
            }
        }
    }
}