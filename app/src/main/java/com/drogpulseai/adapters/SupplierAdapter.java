package com.drogpulseai.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drogpulseai.R;
import com.drogpulseai.models.Supplier;

import java.util.List;
import java.util.Locale;

public class SupplierAdapter extends RecyclerView.Adapter<SupplierAdapter.ViewHolder> {

    private final List<Supplier> suppliers;
    private final Context context;
    private final OnSupplierClickListener listener;

    public interface OnSupplierClickListener {
        void onSupplierClick(Supplier supplier);
        void onCallSupplier(Supplier supplier);
    }

    public SupplierAdapter(Context context, List<Supplier> suppliers, OnSupplierClickListener listener) {
        this.context = context;
        this.suppliers = suppliers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_supplier, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Supplier supplier = suppliers.get(position);

        // Afficher les informations du fournisseur
        holder.tvSupplierName.setText(supplier.getFullName());
        holder.tvSupplierPhone.setText(supplier.getTelephone());

        // Afficher le badge "Principal" si nécessaire
        holder.tvPrimaryBadge.setVisibility(supplier.isPrimary() ? View.VISIBLE : View.GONE);

        // Afficher le prix
        if (supplier.getPrice() > 0) {
            holder.tvSupplierPrice.setVisibility(View.VISIBLE);
            holder.tvSupplierPrice.setText(String.format(Locale.US, "%.2f MAD", supplier.getPrice()));
        } else {
            holder.tvSupplierPrice.setVisibility(View.GONE);
        }

        // Afficher le délai de livraison
        if (supplier.getDeliveryTime() != null && supplier.getDeliveryTime() > 0) {
            holder.tvDeliveryTime.setVisibility(View.VISIBLE);
            holder.tvDeliveryTime.setText(context.getString(R.string.delivery_time_format, supplier.getDeliveryTime()));
        } else {
            holder.tvDeliveryTime.setVisibility(View.GONE);
        }

        // Afficher les notes
        if (supplier.getNotes() != null && !supplier.getNotes().isEmpty()) {
            holder.tvSupplierNotes.setVisibility(View.VISIBLE);
            holder.tvSupplierNotes.setText(supplier.getNotes());
        } else {
            holder.tvSupplierNotes.setVisibility(View.GONE);
        }

        // Configurer les clics
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSupplierClick(supplier);
            }
        });

        holder.btnCallSupplier.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCallSupplier(supplier);
            } else {
                // Appeler directement si pas de listener
                callSupplier(supplier.getTelephone());
            }
        });
    }

    @Override
    public int getItemCount() {
        return suppliers.size();
    }

    /**
     * Helper pour appeler un fournisseur
     */
    private void callSupplier(String phone) {
        if (phone != null && !phone.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            context.startActivity(intent);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvSupplierName;
        final TextView tvSupplierPhone;
        final TextView tvPrimaryBadge;
        final TextView tvSupplierPrice;
        final TextView tvDeliveryTime;
        final TextView tvSupplierNotes;
        final Button btnCallSupplier;

        ViewHolder(View itemView) {
            super(itemView);
            tvSupplierName = itemView.findViewById(R.id.tv_supplier_name);
            tvSupplierPhone = itemView.findViewById(R.id.tv_supplier_phone);
            tvPrimaryBadge = itemView.findViewById(R.id.tv_primary_badge);
            tvSupplierPrice = itemView.findViewById(R.id.tv_supplier_price);
            tvDeliveryTime = itemView.findViewById(R.id.tv_delivery_time);
            tvSupplierNotes = itemView.findViewById(R.id.tv_supplier_notes);
            btnCallSupplier = itemView.findViewById(R.id.btn_call_supplier);
        }
    }

    /**
     * Mettre à jour la liste des fournisseurs
     */
    public void updateData(List<Supplier> newSuppliers) {
        this.suppliers.clear();
        this.suppliers.addAll(newSuppliers);
        notifyDataSetChanged();
    }
}