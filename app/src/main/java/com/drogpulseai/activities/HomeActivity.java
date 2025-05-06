package com.drogpulseai.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.drogpulseai.R;
import com.drogpulseai.activities.appuser.LoginActivity;
import com.drogpulseai.activities.carts.ContactSelectionActivity;
import com.drogpulseai.activities.products.ProductListActivity;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.CameraPermissionHelper;
import com.drogpulseai.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;

public class HomeActivity extends AppCompatActivity implements CameraPermissionHelper.PermissionCallback {

    private SessionManager sessionManager;
    private User currentUser;
    private CameraPermissionHelper cameraPermissionHelper;

    // Constantes pour les actions nécessitant la caméra
    private static final int ACTION_NONE = 0;
    private static final int ACTION_SCAN_BARCODE = 1;
    private static final int ACTION_TAKE_PHOTO = 2;

    // Action en attente de permission
    private int pendingCameraAction = ACTION_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialisation des utilitaires
        sessionManager = new SessionManager(this);

        // Initialiser le helper de permission caméra
        cameraPermissionHelper = new CameraPermissionHelper(this, this);

        // Vérifier si l'utilisateur est connecté
        if (!sessionManager.isLoggedIn()) {
            // Rediriger vers la page de connexion
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Récupérer les données de l'utilisateur
        currentUser = sessionManager.getUser();

        // Afficher le nom de l'utilisateur
        TextView tvUserName = findViewById(R.id.tv_user_name);
        tvUserName.setText(currentUser.getFullName());

        // Configuration des cartes
        setupCards();

        // Vérifier les permissions de caméra au démarrage
        checkCameraPermission();
    }

    private void setupCards() {
        // Carte Contacts
        MaterialCardView cardContacts = findViewById(R.id.card_contacts);
        cardContacts.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
        });

        // Carte Produits
        MaterialCardView cardProducts = findViewById(R.id.card_products);
        cardProducts.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ProductListActivity.class));
        });

        // Carte Créer un panier
        MaterialCardView cardCreateCart = findViewById(R.id.card_create_cart);
        cardCreateCart.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ContactSelectionActivity.class));
        });
    }

    /**
     * Vérifier la permission de la caméra
     */
    private void checkCameraPermission() {
        if (!cameraPermissionHelper.isCameraPermissionGranted()) {
            // Demander la permission au démarrage pour une meilleure expérience utilisateur
            cameraPermissionHelper.requestCameraPermission();
        }
    }

    /**
     * Lancer le scanner de code-barres (après vérification de la permission)
     */
    private void scanBarcode() {
        pendingCameraAction = ACTION_SCAN_BARCODE;

        if (cameraPermissionHelper.checkAndRequestPermission()) {
            // La permission est déjà accordée, lancer le scanner immédiatement
            startBarcodeScanner();
        }
        // Sinon, onPermissionGranted sera appelé si l'utilisateur accorde la permission
    }

    /**
     * Lancer l'appareil photo pour prendre une photo (après vérification de la permission)
     */
    private void takePhoto() {
        pendingCameraAction = ACTION_TAKE_PHOTO;

        if (cameraPermissionHelper.checkAndRequestPermission()) {
            // La permission est déjà accordée, lancer l'appareil photo immédiatement
            startCamera();
        }
        // Sinon, onPermissionGranted sera appelé si l'utilisateur accorde la permission
    }

    /**
     * Lancer l'activité de scan de code-barres
     */
    private void startBarcodeScanner() {
        Toast.makeText(this, "Lancement du scanner de code-barres", Toast.LENGTH_SHORT).show();

        // Ici vous pouvez lancer votre activité de scan de code-barres
        // Exemple : ProductScanActivity

        // Intent intent = new Intent(this, ProductScanActivity.class);
        // startActivity(intent);
    }

    /**
     * Lancer l'activité de prise de photo
     */
    private void startCamera() {
        Toast.makeText(this, "Lancement de l'appareil photo", Toast.LENGTH_SHORT).show();

        // Ici vous pouvez lancer votre activité de prise de photo
        // ou utiliser l'intent de la caméra système

        // Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
        //     startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        // }
    }

    /**
     * Callback appelé lorsque la permission caméra est accordée
     */
    @Override
    public void onPermissionGranted() {
        // Exécuter l'action en attente
        switch (pendingCameraAction) {
            case ACTION_SCAN_BARCODE:
                startBarcodeScanner();
                break;
            case ACTION_TAKE_PHOTO:
                startCamera();
                break;
            case ACTION_NONE:
                // Aucune action en attente, juste informer l'utilisateur
                Toast.makeText(this, "Permission caméra accordée", Toast.LENGTH_SHORT).show();
                break;
        }

        // Réinitialiser l'action en attente
        pendingCameraAction = ACTION_NONE;
    }

    /**
     * Callback appelé lorsque la permission caméra est refusée
     */
    @Override
    public void onPermissionDenied() {
        Toast.makeText(this,
                "La permission caméra est nécessaire pour certaines fonctionnalités de l'application",
                Toast.LENGTH_LONG).show();

        // Réinitialiser l'action en attente
        pendingCameraAction = ACTION_NONE;
    }

    /**
     * Gérer le résultat de la demande de permission
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Déléguer le traitement au helper
        cameraPermissionHelper.handlePermissionResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        // Ajouter les options pour la caméra et le scan
        menu.add(Menu.NONE, R.id.action_scan, Menu.NONE, R.string.scan_barcode);
        menu.add(Menu.NONE, R.id.action_camera, Menu.NONE, R.string.take_photo);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            // Déconnexion
            sessionManager.logout();
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
            return true;
        } else if (id == R.id.action_scan) {
            // Lancer le scanner de code-barres (avec vérification de permission)
            scanBarcode();
            return true;
        } else if (id == R.id.action_camera) {
            // Lancer l'appareil photo (avec vérification de permission)
            takePhoto();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}