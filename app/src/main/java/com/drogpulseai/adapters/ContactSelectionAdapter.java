package com.drogpulseai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drogpulseai.R;
import com.drogpulseai.models.Contact;

import java.util.List;

public class ContactSelectionAdapter extends RecyclerView.Adapter<ContactSelectionAdapter.ViewHolder> {

    private final List<Contact> contacts;
    private final OnContactClickListener listener;
    private final LayoutInflater inflater;
    private int selectedPosition = -1;

    public interface OnContactClickListener {
        void onContactClick(Contact contact, boolean isSelected);
    }

    public ContactSelectionAdapter(Context context, List<Contact> contacts, OnContactClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_contact_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = contacts.get(position);

        // Afficher les informations du contact
        holder.tvName.setText(contact.getFullName());
        holder.tvPhone.setText(contact.getTelephone());

        // Gérer l'email s'il est disponible
        if (contact.getEmail() != null && !contact.getEmail().isEmpty()) {
            holder.tvEmail.setVisibility(View.VISIBLE);
            holder.tvEmail.setText(contact.getEmail());
        } else {
            holder.tvEmail.setVisibility(View.GONE);
        }

        // Gérer l'état de sélection
        holder.radioButton.setChecked(position == selectedPosition);

        // Configurer les clics
        View.OnClickListener clickListener = v -> {
            int oldSelectedPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Mettre à jour l'élément précédemment sélectionné
            if (oldSelectedPosition != -1) {
                notifyItemChanged(oldSelectedPosition);
            }

            // Mettre à jour l'élément actuellement sélectionné
            notifyItemChanged(selectedPosition);

            // Notifier le listener
            if (listener != null) {
                listener.onContactClick(contact, true);
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.radioButton.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvEmail;
        RadioButton radioButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvPhone = itemView.findViewById(R.id.tv_contact_phone);
            tvEmail = itemView.findViewById(R.id.tv_contact_email);
            radioButton = itemView.findViewById(R.id.radio_select_contact);
        }
    }
}