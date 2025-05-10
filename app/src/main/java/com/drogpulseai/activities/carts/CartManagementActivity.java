package com.drogpulseai.activities.carts;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.drogpulseai.R;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;

/**
 * Activity for cart management options
 * Provides access to cart creation and viewing all carts
 */
public class CartManagementActivity extends AppCompatActivity {

    // Session manager
    private SessionManager sessionManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_management);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.cart_management);
        }

        // Initialize session manager
        sessionManager = new SessionManager(this);
        currentUser = sessionManager.getUser();

        // Initialize the UI components
        initializeViews();
    }

    /**
     * Initialize the view components and set up click listeners
     */
    private void initializeViews() {
        // Carte Créer un panier
        MaterialCardView cardCreateCart = findViewById(R.id.card_create_cart);
        cardCreateCart.setOnClickListener(v -> {
           Intent intent= new Intent(CartManagementActivity.this, ContactSelectionActivity.class);
            startActivity(intent);
        });

        // Carte Tous les paniers
        MaterialCardView cardAllCarts = findViewById(R.id.card_all_carts);
        cardAllCarts.setOnClickListener(v -> {
            Intent intent =new Intent(CartManagementActivity.this, FilteredCartsActivity.class);
            intent.putExtra("status", "pending");
            startActivity(intent);
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle back button
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}