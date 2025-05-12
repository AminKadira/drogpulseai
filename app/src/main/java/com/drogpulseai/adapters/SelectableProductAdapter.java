package com.drogpulseai.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SelectableProductAdapter extends RecyclerView.Adapter<SelectableProductAdapter.ViewHolder> {

    private final List<Product> products;
    private final Context context;
    private final OnProductSelectionListener listener;
    // Nouveau champ pour stocker les IDs de produits sélectionnés
    private Set<Integer> selectedProductIds = new HashSet<>();

    public interface OnProductSelectionListener {
        void onProductSelected(int productId, boolean isSelected);
    }

    public SelectableProductAdapter(Context context, List<Product> products, OnProductSelectionListener listener) {
        this.context = context;
        this.products = products;
        this.listener = listener;
    }

    /**
     * Définir les IDs de produits sélectionnés
     * @param selectedProductIds Ensemble des IDs de produits sélectionnés
     */
    public void setSelectedProductIds(Set<Integer> selectedProductIds) {
        this.selectedProductIds = new HashSet<>(selectedProductIds);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selectable_product, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);

        // Afficher les informations du produit
        holder.tvReference.setText(product.getReference());
        holder.tvName.setText(product.getName());

        if(product.getQuantity() > 20 ){
            holder.tvQuantity.setBackgroundColor(ContextCompat.getColor(context, R.color.primary));
        } else if (product.getQuantity() > 0 && product.getQuantity() < 20) {
            holder.tvQuantity.setBackgroundColor(ContextCompat.getColor(context, R.color.accent));
        } else if (product.getQuantity() <= 0) {
            holder.tvQuantity.setBackgroundColor(ContextCompat.getColor(context, R.color.error));
        }
        holder.tvQuantity.setText(context.getString(R.string.stock_format, product.getQuantity()));

        // Afficher le prix
        if (product.isAssociatedWithSupplier() && product.getSupplierPrice() != null && product.getSupplierPrice() > 0) {
            // Afficher le prix du fournisseur s'il existe
            holder.tvPrice.setVisibility(View.VISIBLE);
            holder.tvPrice.setText(String.format(Locale.US, "%.2f MAD", product.getSupplierPrice()));
            holder.tvPrice.setBackgroundResource(R.color.supplier_price_bg); // Couleur différente pour les prix fournisseur
        } else if (product.getPrice() > 0) {
            // Sinon afficher le prix standard
            holder.tvPrice.setVisibility(View.VISIBLE);
            holder.tvPrice.setText(String.format(Locale.US, "%.2f MAD", product.getPrice()));
            holder.tvPrice.setBackgroundResource(R.color.accent);
        } else {
            holder.tvPrice.setVisibility(View.GONE);
        }

        // Indiquer visuellement que le produit est déjà associé au fournisseur
        if (product.isAssociatedWithSupplier()) {
            // Ajouter un indicateur visuel différent pour les fournisseurs principaux
            if (product.isPrimarySupplier()) {
                holder.itemView.setBackgroundResource(R.drawable.bg_primary_supplier_product);
                // Ajouter un badge ou indicateur
                holder.tvPrimaryBadge.setVisibility(View.VISIBLE);
            } else {
                holder.itemView.setBackgroundResource(R.drawable.bg_associated_product);
                holder.tvPrimaryBadge.setVisibility(View.GONE);
            }
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
            holder.tvPrimaryBadge.setVisibility(View.GONE);
        }

        // Configurer la case à cocher
        holder.checkBox.setOnCheckedChangeListener(null);
        // Vérifier si ce produit est sélectionné
        holder.checkBox.setChecked(selectedProductIds.contains(product.getId()));

        // Configurer le clic sur l'élément
        holder.itemView.setOnClickListener(v -> {
            holder.checkBox.setChecked(!holder.checkBox.isChecked());
        });

        // Configurer le changement d'état de la case à cocher
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onProductSelected(product.getId(), isChecked);
            }
        });

        // Charger l'image du produit
        loadProductImage(holder.ivProductImage, product);
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkBox;
        final TextView tvReference;
        final TextView tvName;
        final TextView tvQuantity;
        final TextView tvPrice;
        final ImageView ivProductImage;
        final TextView tvPrimaryBadge;

        ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox_product);
            tvReference = itemView.findViewById(R.id.tv_product_reference);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvQuantity = itemView.findViewById(R.id.tv_product_quantity);
            tvPrice = itemView.findViewById(R.id.tv_product_price);
            ivProductImage = itemView.findViewById(R.id.iv_product_image);
            tvPrimaryBadge = itemView.findViewById(R.id.tv_primary_badge);
        }
    }

    /**
     * Méthode pour charger l'image du produit
     */
    private void loadProductImage(ImageView imageView, Product product) {
        // Le reste du code reste inchangé
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
}