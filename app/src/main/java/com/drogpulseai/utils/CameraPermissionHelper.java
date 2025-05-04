package com.drogpulseai.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Classe utilitaire pour gérer les demandes d'autorisation de la caméra
 */
public class CameraPermissionHelper {

    // Permission de caméra
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    // Code de demande pour les permissions (pour la méthode traditionnelle)
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    // Interface pour les callbacks
    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    // Attributs
    private final Context context;
    private final PermissionCallback callback;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    /**
     * Constructeur pour utilisation avec l'API ActivityResultLauncher (recommandé)
     */
    public CameraPermissionHelper(AppCompatActivity activity, PermissionCallback callback) {
        this.context = activity;
        this.callback = callback;

        // Initialiser le launcher de demande de permission
        initializePermissionLauncher(activity);
    }

    /**
     * Vérifie si la permission caméra est accordée et la demande si nécessaire
     * @return true si la permission est déjà accordée
     */
    public boolean checkAndRequestPermission() {
        if (isCameraPermissionGranted()) {
            // La permission est déjà accordée
            if (callback != null) {
                callback.onPermissionGranted();
            }
            return true;
        } else {
            // La permission n'est pas accordée, la demander
            requestCameraPermission();
            return false;
        }
    }

    /**
     * Vérifie si la permission caméra est déjà accordée
     * @return true si la permission est accordée
     */
    public boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(context, CAMERA_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Demande la permission d'accès à la caméra
     */
    public void requestCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, CAMERA_PERMISSION)) {
                // L'utilisateur a déjà refusé la permission, montrer une explication
                showPermissionRationaleDialog();
            } else {
                // Première demande ou "Ne plus demander" coché
                if (requestPermissionLauncher != null) {
                    // Utiliser l'API moderne si disponible
                    requestPermissionLauncher.launch(CAMERA_PERMISSION);
                } else {
                    // Sinon, utiliser l'ancienne méthode
                    ActivityCompat.requestPermissions(
                            (Activity) context,
                            new String[]{CAMERA_PERMISSION},
                            CAMERA_PERMISSION_REQUEST_CODE
                    );
                }
            }
        } else {
            // Sur les appareils avant Android 6.0, la permission est accordée à l'installation
            if (callback != null) {
                callback.onPermissionGranted();
            }
        }
    }

    /**
     * Initialise le launcher pour demander la permission avec l'API moderne
     */
    private void initializePermissionLauncher(AppCompatActivity activity) {
        requestPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission accordée
                        if (callback != null) {
                            callback.onPermissionGranted();
                        }
                    } else {
                        // Permission refusée
                        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION)) {
                            // L'utilisateur a refusé mais n'a pas coché "Ne plus demander"
                            showPermissionDeniedMessage();
                        } else {
                            // L'utilisateur a coché "Ne plus demander"
                            showSettingsDialog();
                        }

                        if (callback != null) {
                            callback.onPermissionDenied();
                        }
                    }
                }
        );
    }

    /**
     * Affiche une boîte de dialogue expliquant pourquoi la permission est nécessaire
     */
    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Autorisation requise")
                .setMessage("L'accès à la caméra est nécessaire pour cette fonctionnalité. " +
                        "Veuillez autoriser l'accès à la caméra.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (requestPermissionLauncher != null) {
                            requestPermissionLauncher.launch(CAMERA_PERMISSION);
                        } else {
                            ActivityCompat.requestPermissions(
                                    (Activity) context,
                                    new String[]{CAMERA_PERMISSION},
                                    CAMERA_PERMISSION_REQUEST_CODE
                            );
                        }
                    }
                })
                .setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (callback != null) {
                            callback.onPermissionDenied();
                        }
                    }
                })
                .create()
                .show();
    }

    /**
     * Affiche un message lorsque la permission est refusée
     */
    private void showPermissionDeniedMessage() {
        Toast.makeText(context,
                "L'accès à la caméra est nécessaire pour utiliser cette fonctionnalité",
                Toast.LENGTH_LONG).show();
    }

    /**
     * Affiche une boîte de dialogue pour ouvrir les paramètres de l'application
     * lorsque l'utilisateur a coché "Ne plus demander"
     */
    private void showSettingsDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Autorisation requise")
                .setMessage("L'accès à la caméra est nécessaire pour cette fonctionnalité. " +
                        "Veuillez l'activer dans les paramètres de l'application.")
                .setPositiveButton("Paramètres", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        openAppSettings();
                    }
                })
                .setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create()
                .show();
    }

    /**
     * Ouvre les paramètres de l'application
     */
    private void openAppSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    /**
     * Méthode à appeler depuis onRequestPermissionsResult de l'activité
     * pour la méthode traditionnelle de gestion des permissions
     */
    public void handlePermissionResult(int requestCode, @NonNull String[] permissions,
                                       @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée
                if (callback != null) {
                    callback.onPermissionGranted();
                }
            } else {
                // Permission refusée
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, CAMERA_PERMISSION)) {
                    // L'utilisateur a refusé mais n'a pas coché "Ne plus demander"
                    showPermissionDeniedMessage();
                } else {
                    // L'utilisateur a coché "Ne plus demander"
                    showSettingsDialog();
                }

                if (callback != null) {
                    callback.onPermissionDenied();
                }
            }
        }
    }
}