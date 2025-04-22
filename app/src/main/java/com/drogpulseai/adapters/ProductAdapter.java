package com.drogpulseai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.drogpulseai.R;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.models.Product;

import java.util.List;

/**
 * Adaptateur pour afficher les produits dans un RecyclerView
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private final List<Product> products;
    private final OnProductClickListener listener;
    private final LayoutInflater inflater;
    private final Context context;

    /**
     * Interface pour gérer les clics sur les produits
     */
    public interface OnProductClickListener {
        void onProductClick(Product product);
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
        holder.tvLabel.setText(product.getLabel());

        // Afficher le code-barres s'il est disponible
        if (product.getBarcode() != null && !product.getBarcode().isEmpty()) {
            holder.tvBarcode.setVisibility(View.VISIBLE);
            holder.tvBarcode.setText(product.getBarcode());
        } else {
            holder.tvBarcode.setVisibility(View.GONE);
        }

        // Afficher la quantité
        holder.tvQuantity.setText(context.getString(R.string.stock_format, product.getQuantity()));

        // Charger l'image si disponible
        if (product.getPhotoUrl() != null && !product.getPhotoUrl().isEmpty()) {
            Glide.with(context)
                    .load(ApiClient.getBaseUrl().endsWith("/") ? ApiClient.getBaseUrl() + product.getPhotoUrl() : ApiClient.getBaseUrl() + "/" + product.getPhotoUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .centerCrop()
                    .into(holder.ivThumbnail);
        } else {
            // Image par défaut
            holder.ivThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Configurer le clic sur l'élément
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    /**
     * ViewHolder pour les éléments de produit
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReference, tvName, tvLabel, tvBarcode, tvQuantity;
        ImageView ivThumbnail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReference = itemView.findViewById(R.id.tv_product_reference);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvLabel = itemView.findViewById(R.id.tv_product_label);
            tvBarcode = itemView.findViewById(R.id.tv_product_barcode);
            tvQuantity = itemView.findViewById(R.id.tv_product_quantity);
            ivThumbnail = itemView.findViewById(R.id.iv_product_thumbnail);
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