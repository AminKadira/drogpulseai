package com.drogpulseai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.drogpulseai.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FilteredCartAdapter extends RecyclerView.Adapter<FilteredCartAdapter.ViewHolder> {

    private final List<Map<String, Object>> carts;
    private final Context context;
    private final OnCartClickListener listener;
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnCartClickListener {
        void onCartClick(Map<String, Object> cart);
    }

    public FilteredCartAdapter(Context context, List<Map<String, Object>> carts, OnCartClickListener listener) {
        this.context = context;
        this.carts = carts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_filtered_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> cart = carts.get(position);

        // Récupérer les données du panier
        int cartId = ((Double) cart.get("id")).intValue();
        String contactName = (String) cart.get("contact_name");
        String createdAt = (String) cart.get("created_at");
        String status = (String) cart.get("status");
        int itemsCount = ((Double) cart.get("items_count")).intValue();
        int totalQuantity = ((Double) cart.get("total_quantity")).intValue();
        double totalAmount = ((Double) cart.get("total_amount"));

        // Formater la date
        String formattedDate = formatDate(createdAt);

        // Afficher les informations du panier
        holder.tvCartId.setText(String.format(Locale.getDefault(), "Panier #%d", cartId));
        holder.tvCartDate.setText(formattedDate);
        holder.tvContactName.setText(contactName);

        // Afficher le statut
        holder.tvStatus.setText(getStatusLabel(status));
        holder.tvStatus.setBackgroundColor(getStatusColor(status));

        // Afficher les détails
        holder.tvDetails.setText(String.format(Locale.getDefault(),
                "%d produit(s), %d article(s), %.2f €",
                itemsCount, totalQuantity, totalAmount));

        // Configurer le clic
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCartClick(cart);
            }
        });
    }

    @Override
    public int getItemCount() {
        return carts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCartId, tvCartDate, tvContactName, tvStatus, tvDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCartId = itemView.findViewById(R.id.tv_cart_id);
            tvCartDate = itemView.findViewById(R.id.tv_cart_date);
            tvContactName = itemView.findViewById(R.id.tv_contact_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvDetails = itemView.findViewById(R.id.tv_details);
        }
    }

    private String formatDate(String dateString) {
        try {
            Date date = inputFormat.parse(dateString);
            return date != null ? outputFormat.format(date) : dateString;
        } catch (ParseException e) {
            return dateString;
        }
    }

    private String getStatusLabel(String status) {
        switch (status) {
            case "pending":
                return "En attente";
            case "confirmed":
                return "Confirmé";
            case "cancelled":
                return "Annulé";
            default:
                return status;
        }
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "pending":
                return ContextCompat.getColor(context, R.color.warning);            
            case "confirmed":
                return ContextCompat.getColor(context, R.color.success);
            case "cancelled":
                return ContextCompat.getColor(context, R.color.error);
            default:
                return ContextCompat.getColor(context, R.color.primaryText);
        }
    }
}