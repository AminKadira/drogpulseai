package com.drogpulseai.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.drogpulseai.R;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.models.Product;
import com.drogpulseai.models.ProductCartItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CartProductAdapter extends RecyclerView.Adapter<CartProductAdapter.ViewHolder> {

    private static final String TAG = "CartProductAdapter";
    private static final int DEFAULT_QUANTITY = 1;
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 99;

    private final List<Product> allProducts; // Liste de tous les produits
    private final List<Product> filteredProducts; // Liste filtrée pour la recherche
    private final LayoutInflater inflater;
    private final Context context;
    private final Map<Integer, Integer> productQuantities = new HashMap<>(); // Map des quantités (ID produit -> quantité)
    private final Set<Integer> selectedProducts = new HashSet<>(); // Ensemble des IDs de produits sélectionnés
    private final OnProductSelectionChangeListener listener;
    private String currentSearchQuery = ""; // Requête de recherche actuelle

    public interface OnProductSelectionChangeListener {
        void onSelectionChanged(int count, int totalItems);
    }

    public CartProductAdapter(Context context, List<Product> products, OnProductSelectionChangeListener listener) {
        this.context = context;
        this.allProducts = new ArrayList<>();
        this.allProducts.addAll(products);
        this.filteredProducts = new ArrayList<>(this.allProducts);
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;

        Log.d(TAG, "Adaptateur initialisé avec " + this.allProducts.size() + " produits");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_cart_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = filteredProducts.get(position);
        int productId = product.getId();

        // Afficher les informations du produit
        holder.tvReference.setText(product.getReference());
        holder.tvName.setText(product.getName());
        holder.tvQuantity.setText(context.getString(R.string.stock_format, product.getQuantity()));

        // Afficher le prix s'il est disponible
        if (product.getPrice() > 0) {
            holder.tvPrice.setVisibility(View.VISIBLE);
            holder.tvPrice.setText(context.getString(R.string.price_format, product.getPrice()));
        } else {
            holder.tvPrice.setVisibility(View.GONE);
        }

        // Gérer l'état de sélection
        boolean isSelected = selectedProducts.contains(productId);
        holder.checkBox.setChecked(isSelected);

        // Afficher ou masquer le contrôle de quantité selon l'état de sélection
        holder.quantityControlLayout.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        // Mettre à jour la valeur de quantité
        int quantity = productQuantities.getOrDefault(productId, DEFAULT_QUANTITY);
        holder.tvQuantityValue.setText(String.valueOf(quantity));

        // Charger l'image avec Glide
        loadProductImage(holder.ivThumbnail, product);

        // Configurer les clics sur l'élément et la checkbox
        View.OnClickListener selectionClickListener = v -> {
            toggleSelection(productId);
            notifyItemChanged(holder.getAdapterPosition());

            // Notifier le listener du changement de sélection
            if (listener != null) {
                listener.onSelectionChanged(selectedProducts.size(), getTotalItemCount());
            }
        };

        holder.itemView.setOnClickListener(selectionClickListener);
        holder.checkBox.setOnClickListener(selectionClickListener);

        // Configurer les boutons de quantité
        holder.btnDecrease.setOnClickListener(v -> {
            if (!selectedProducts.contains(productId)) return;

            int currentQty = productQuantities.getOrDefault(productId, DEFAULT_QUANTITY);
            if (currentQty > MIN_QUANTITY) {
                currentQty--;
                productQuantities.put(productId, currentQty);
                holder.tvQuantityValue.setText(String.valueOf(currentQty));

                // Notifier le listener du changement de quantité
                if (listener != null) {
                    listener.onSelectionChanged(selectedProducts.size(), getTotalItemCount());
                }
            }
        });

        holder.btnIncrease.setOnClickListener(v -> {
            if (!selectedProducts.contains(productId)) return;

            int currentQty = productQuantities.getOrDefault(productId, DEFAULT_QUANTITY);
            if (currentQty < MAX_QUANTITY) {
                currentQty++;
                productQuantities.put(productId, currentQty);
                holder.tvQuantityValue.setText(String.valueOf(currentQty));

                // Notifier le listener du changement de quantité
                if (listener != null) {
                    listener.onSelectionChanged(selectedProducts.size(), getTotalItemCount());
                }
            }
        });
    }

    private void toggleSelection(int productId) {
        if (selectedProducts.contains(productId)) {
            // Déselectionner le produit
            selectedProducts.remove(productId);
            productQuantities.remove(productId);
        } else {
            // Sélectionner le produit
            selectedProducts.add(productId);
            productQuantities.put(productId, DEFAULT_QUANTITY);
        }
    }

    @Override
    public int getItemCount() {
        return filteredProducts.size();
    }

    /**
     * Obtenir le nombre total d'articles (somme des quantités)
     */
    private int getTotalItemCount() {
        int total = 0;
        for (Integer productId : selectedProducts) {
            total += productQuantities.getOrDefault(productId, DEFAULT_QUANTITY);
        }
        return total;
    }

    /**
     * Méthode pour charger l'image du produit
     */
    private void loadProductImage(ImageView imageView, Product product) {
        String photoUrl = product.getPhotoUrl();

        // Définir une image par défaut
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        if (photoUrl != null && !photoUrl.isEmpty()) {
            // Construire l'URL complète si nécessaire
            String fullUrl;

            if (photoUrl.startsWith("http") || photoUrl.startsWith("https")) {
                // URL déjà complète
                fullUrl = photoUrl;
            } else {
                // Construire l'URL complète
                String baseUrl = ApiClient.getBaseUrl();
                if (!baseUrl.endsWith("/") && !photoUrl.startsWith("/")) {
                    baseUrl += "/";
                }
                fullUrl = baseUrl + photoUrl;
            }

            // Charger l'image avec Glide
            Glide.with(context)
                    .load(fullUrl)
                    .apply(options)
                    .into(imageView);
        } else {
            // Pas d'URL d'image, utiliser le placeholder par défaut
            Glide.with(context)
                    .load(R.drawable.ic_image_placeholder)
                    .into(imageView);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReference, tvName, tvQuantity, tvPrice, tvQuantityValue;
        ImageView ivThumbnail;
        CheckBox checkBox;
        LinearLayout quantityControlLayout;
        ImageButton btnDecrease, btnIncrease;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReference = itemView.findViewById(R.id.tv_product_reference);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvQuantity = itemView.findViewById(R.id.tv_product_quantity);
            tvPrice = itemView.findViewById(R.id.tv_product_price);
            ivThumbnail = itemView.findViewById(R.id.iv_product_thumbnail);
            checkBox = itemView.findViewById(R.id.checkbox_select_product);
            quantityControlLayout = itemView.findViewById(R.id.quantity_control_layout);
            tvQuantityValue = itemView.findViewById(R.id.tv_quantity_value);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
        }
    }

    /**
     * Récupérer la liste des produits sélectionnés avec leurs quantités
     */
    public List<ProductCartItem> getSelectedProductItems() {
        List<ProductCartItem> items = new ArrayList<>();

        for (Product product : allProducts) {
            int productId = product.getId();
            if (selectedProducts.contains(productId)) {
                int quantity = productQuantities.getOrDefault(productId, DEFAULT_QUANTITY);
                items.add(new ProductCartItem(product, quantity));
            }
        }

        return items;
    }

    /**
     * Vider la sélection
     */
    public void clearSelection() {
        selectedProducts.clear();
        productQuantities.clear();
        notifyDataSetChanged();

        if (listener != null) {
            listener.onSelectionChanged(0, 0);
        }
    }

    /**
     * Sélectionner tous les produits visibles (filtrés)
     */
    public void selectAll() {
        for (Product product : filteredProducts) {
            int productId = product.getId();
            selectedProducts.add(productId);

            // Initialiser la quantité si elle n'est pas déjà définie
            if (!productQuantities.containsKey(productId)) {
                productQuantities.put(productId, DEFAULT_QUANTITY);
            }
        }

        notifyDataSetChanged();

        if (listener != null) {
            listener.onSelectionChanged(selectedProducts.size(), getTotalItemCount());
        }
    }

    /**
     * Filtrer les produits en fonction de la requête de recherche
     */
    public void filter(String query) {
        currentSearchQuery = query.toLowerCase().trim();
        filteredProducts.clear();

        if (currentSearchQuery.isEmpty()) {
            // Si la requête est vide, afficher tous les produits
            filteredProducts.addAll(allProducts);
        } else {
            // Sinon, filtrer les produits selon la requête
            for (Product product : allProducts) {
                // Vérifier si le produit correspond à la requête
                if (matchesSearchQuery(product, currentSearchQuery)) {
                    filteredProducts.add(product);
                }
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Vérifier si un produit correspond à la requête de recherche
     */
    private boolean matchesSearchQuery(Product product, String query) {
        // Vérifier différents champs du produit
        return product.getName().toLowerCase().contains(query)
                || product.getReference().toLowerCase().contains(query)
                || (product.getBarcode() != null && product.getBarcode().toLowerCase().contains(query))
                || (product.getDescription() != null && product.getDescription().toLowerCase().contains(query));
    }

    /**
     * Mettre à jour la liste des produits
     */
    public void updateProducts(List<Product> newProducts) {
        allProducts.clear();
        allProducts.addAll(newProducts);

        // Mettre également à jour la liste filtrée
        filter(currentSearchQuery);
    }

    /**
     * Réinitialiser la liste filtrée pour afficher tous les produits
     */
    public void showAllProducts() {
        currentSearchQuery = "";
        filteredProducts.clear();
        filteredProducts.addAll(allProducts);
        notifyDataSetChanged();
    }
}