package com.drogpulseai.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.drogpulseai.R;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.models.Product;

import java.util.List;
import java.util.Locale;

/**
 * Adaptateur pour afficher les produits dans un RecyclerView
 * Mis à jour pour prendre en charge plusieurs photos par produit
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private static final String TAG = "ProductAdapter";
    private final List<Product> products;
    private final OnProductClickListener listener;
    private final LayoutInflater inflater;
    private final Context context;
    /**
     * Interface pour gérer les clics sur les produits
     */
    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onViewSuppliersClick(Product product);

    }

    /**
     * Constructeur
     */
    public ProductAdapter(Context context, List<Product> products, OnProductClickListener listener) {
        this.context = context;
        this.products = products;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);

        // Afficher les informations du produit
        holder.tvReference.setText(product.getReference());
        holder.tvName.setText(product.getName());

        // Afficher la quantité
        holder.tvQuantity.setText(context.getString(R.string.stock_format, product.getQuantity()));
        if(product.getQuantity() > 20 ){
            holder.tvQuantity.setBackgroundColor(ContextCompat.getColor(context, R.color.primary));
        } else if (product.getQuantity() > 0 && product.getQuantity() < 20) {
            holder.tvQuantity.setBackgroundColor(ContextCompat.getColor(context, R.color.accent));
        } else if (product.getQuantity() == 0) {
            holder.tvQuantity.setBackgroundColor(ContextCompat.getColor(context, R.color.error));
        }
        // Afficher le prix
        if (product.getPrice() > 0) {
            holder.tvPrice.setVisibility(View.VISIBLE);
            holder.tvPrice.setText(String.format(Locale.US, "%.2f MAD", product.getPrixVenteConseille()));
        } else {
            holder.tvPrice.setVisibility(View.GONE);
        }

        // Charger l'image avec une gestion améliorée pour plusieurs photos
        loadProductImage(holder.ivThumbnail, product);

        // Configurer le clic sur l'élément
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });

        holder.btnViewSuppliers.setOnClickListener(v -> {
            if (listener != null) {
                // Créer une nouvelle méthode dans l'interface
                listener.onViewSuppliersClick(product);
            }
        });
    }

    /**
     * Méthode améliorée pour charger l'image du produit
     * Supporte plusieurs photos par produit
     */
    private void loadProductImage(ImageView imageView, Product product) {
        // Déterminer quelle URL de photo utiliser, en priorité la première
        String photoUrl = null;

        if (product.getPhotoUrl() != null && !product.getPhotoUrl().isEmpty()) {
            photoUrl = product.getPhotoUrl();
        } else if (product.getPhotoUrl2() != null && !product.getPhotoUrl2().isEmpty()) {
            photoUrl = product.getPhotoUrl2();
        } else if (product.getPhotoUrl3() != null && !product.getPhotoUrl3().isEmpty()) {
            photoUrl = product.getPhotoUrl3();
        }

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
                // S'assurer que l'URL se termine par un slash si nécessaire
                if (!baseUrl.endsWith("/") && !photoUrl.startsWith("/")) {
                    baseUrl += "/";
                }
                fullUrl = baseUrl + photoUrl;
            }

            Log.d(TAG, "Loading image from URL: " + fullUrl);

            // Charger l'image avec Glide
            Glide.with(context)
                    .load(fullUrl)
                    .apply(options)
                    .into(imageView);
        } else {
            Log.d(TAG, "No image URL available, using placeholder");
            // Pas d'URL d'image, utiliser le placeholder par défaut
            Glide.with(context)
                    .load(R.drawable.ic_image_placeholder)
                    .into(imageView);
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    /**
     * ViewHolder pour les éléments de produit
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        Button btnViewSuppliers;
        TextView tvReference, tvName, tvQuantity, tvPrice;
        ImageView ivThumbnail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReference = itemView.findViewById(R.id.tv_product_reference);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvQuantity = itemView.findViewById(R.id.tv_product_quantity);
            tvPrice = itemView.findViewById(R.id.tv_product_price);
            ivThumbnail = itemView.findViewById(R.id.iv_product_thumbnail);
            btnViewSuppliers = itemView.findViewById(R.id.btn_view_suppliers);

        }
    }

    /**
     * Mettre à jour la liste de produits
     */
    public void updateData(List<Product> newProducts) {
        this.products.clear();
        this.products.addAll(newProducts);
        notifyDataSetChanged();
    }
}