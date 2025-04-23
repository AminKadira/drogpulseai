package com.drogpulseai.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.drogpulseai.R;
import com.drogpulseai.activities.appuser.LoginActivity;
import com.drogpulseai.utils.Config;
import com.drogpulseai.utils.ConnectionChecker;

public class SplashActivity extends AppCompatActivity {
    ConnectionChecker connectionChecker;

    private static final int SPLASH_DURATION = 2000; // 2 secondes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //run configuration
       Config.init(this);

       // Vérifier la connexion au serveur
        connectionChecker = new ConnectionChecker(this, new ConnectionChecker.OnConnectionCheckListener() {
            @Override
            public void onConnectionSuccess() {
                // Afficher le dialogue de succès
                Toast.makeText(SplashActivity.this, connectionChecker.showConnectionDialog(true, null), Toast.LENGTH_LONG).show();

                // Continuer l'initialisation de l'application après un court délai
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Exemple: naviguer vers l'activité suivante
                        // startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                        // finish();
                    }
                }, 1500); // 1.5 secondes
            }

            @Override
            public void onConnectionFailure(String errorMessage) {
                // Afficher le dialogue d'erreur
                connectionChecker.showConnectionDialog(false, errorMessage);

                // Donner à l'utilisateur l'option de réessayer ou de quitter
                new AlertDialog.Builder(SplashActivity.this)
                        .setTitle("Erreur de connexion")
                        .setMessage("Impossible de se connecter au serveur. Voulez-vous réessayer?")
                        .setPositiveButton("Réessayer", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Réessayer
                                connectionChecker.checkConnection();
                            }
                        })
                        .setNegativeButton("Quitter", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .show();
            }
        });

        // Lancer la vérification
        connectionChecker.checkConnection();

        // Différer la redirection pour afficher le splash screen
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Rediriger vers LoginActivity quoi qu'il arrive
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DURATION);
    }
}
