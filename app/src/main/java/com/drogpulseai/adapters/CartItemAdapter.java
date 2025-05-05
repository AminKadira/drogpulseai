package com.drogpulseai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drogpulseai.R;
import com.drogpulseai.models.CartItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.ViewHolder> {

    private final Context context;
    private List<CartItem> items;

    public CartItemAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = items.get(position);

        // Numéro de l'article
        holder.tvItemNumber.setText(String.valueOf(position + 1));

        // Informations produit
        holder.tvProductInfo.setText(String.format("%s\nRéf: %s",
                item.getProductName(),
                item.getProductReference()));

        // Quantité
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        // Prix unitaire
        holder.tvUnitPrice.setText(String.format(Locale.getDefault(),
                "%.2f €",
                item.getPrice()));

        // Prix total
        holder.tvTotalPrice.setText(String.format(Locale.getDefault(),
                "%.2f €",
                item.getTotalPrice()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemNumber, tvProductInfo, tvQuantity, tvUnitPrice, tvTotalPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemNumber = itemView.findViewById(R.id.tv_item_number);
            tvProductInfo = itemView.findViewById(R.id.tv_product_info);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            tvUnitPrice = itemView.findViewById(R.id.tv_unit_price);
            tvTotalPrice = itemView.findViewById(R.id.tv_total_price);
        }
    }
}