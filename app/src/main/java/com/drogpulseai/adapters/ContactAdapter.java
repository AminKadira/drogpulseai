package com.drogpulseai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drogpulseai.R;
import com.drogpulseai.models.Contact;

import java.util.List;

/**
 * Adaptateur pour afficher les contacts dans un RecyclerView
 */
public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private final List<Contact> contacts;
    private final OnContactClickListener listener;
    private final LayoutInflater inflater;

    /**
     * Interface pour gérer les clics sur les contacts et les boutons
     */
    public interface OnContactClickListener {
        void onContactClick(Contact contact);
        void onViewCartsClick(Contact contact); // Nouvelle méthode
    }

    // Constructeur inchangé
    public ContactAdapter(Context context, List<Contact> contacts, OnContactClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = contacts.get(position);

        // Afficher les informations du contact
        holder.tvName.setText(contact.getFullName());
        holder.tvPhone.setText(contact.getTelephone());

        // Si l'email est disponible, l'afficher
        if (contact.getEmail() != null && !contact.getEmail().isEmpty()) {
            holder.tvEmail.setVisibility(View.VISIBLE);
            holder.tvEmail.setText(contact.getEmail());
        } else {
            holder.tvEmail.setVisibility(View.GONE);
        }

        // Configurer le clic sur l'élément
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContactClick(contact);
            }
        });

        // Configurer le clic sur le bouton Panier
        holder.btnViewCarts.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewCartsClick(contact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    /**
     * ViewHolder pour les éléments de contact
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvEmail;
        Button btnViewCarts; // Ajout du bouton

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvPhone = itemView.findViewById(R.id.tv_contact_phone);
            tvEmail = itemView.findViewById(R.id.tv_contact_email);
            btnViewCarts = itemView.findViewById(R.id.btn_view_carts);
        }
    }

    // Méthode pour mettre à jour les données (inchangée)
    public void updateData(List<Contact> newContacts) {
        this.contacts.clear();
        this.contacts.addAll(newContacts);
        notifyDataSetChanged();
    }
}