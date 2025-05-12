package com.drogpulseai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drogpulseai.R;
import com.drogpulseai.models.DailyExpensesGroup;
import com.drogpulseai.models.Expense;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DailyExpensesAdapter extends RecyclerView.Adapter<DailyExpensesAdapter.ViewHolder> {

    private final List<DailyExpensesGroup> expenseGroups;
    private final Context context;
    private final LayoutInflater inflater;
    private final OnExpenseClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE dd MMMM yyyy", Locale.getDefault());

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public DailyExpensesAdapter(Context context, List<DailyExpensesGroup> expenseGroups, OnExpenseClickListener listener) {
        this.context = context;
        this.expenseGroups = expenseGroups;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_daily_expenses_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyExpensesGroup group = expenseGroups.get(position);

        // Afficher la date et le total pour le jour
        holder.tvDayDate.setText(dateFormat.format(group.getDate()));
        holder.tvDayTotal.setText(String.format(Locale.US, "%.2f MAD", group.getTotalAmount()));

        // Vider d'abord le conteneur
        holder.llExpensesContainer.removeAllViews();

        // Ajouter chaque frais au conteneur
        for (Expense expense : group.getExpenses()) {
            View expenseView = inflater.inflate(R.layout.item_daily_expense, holder.llExpensesContainer, false);

            ImageView ivIcon = expenseView.findViewById(R.id.iv_expense_icon);
            TextView tvType = expenseView.findViewById(R.id.tv_expense_type);
            TextView tvDescription = expenseView.findViewById(R.id.tv_expense_description);
            TextView tvAmount = expenseView.findViewById(R.id.tv_expense_amount);

            // Définir l'icône en fonction du type de frais
            setExpenseIcon(ivIcon, expense.getType());

            // Définir les textes
            tvType.setText(expense.getType());
            tvDescription.setText(expense.getDescription());
            tvAmount.setText(String.format(Locale.US, "%.2f MAD", expense.getAmount()));

            // Configurer le clic sur l'élément
            expenseView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExpenseClick(expense);
                }
            });

            // Ajouter la vue au conteneur
            holder.llExpensesContainer.addView(expenseView);
        }
    }

    private void setExpenseIcon(ImageView imageView, String expenseType) {
        int iconResource;
        switch (expenseType.toLowerCase()) {
            case "carburant":
                iconResource = android.R.drawable.ic_menu_directions;
                break;
            case "hôtel":
            case "hotel":
                iconResource = android.R.drawable.ic_menu_mylocation;
                break;
            case "autoroute":
                iconResource = android.R.drawable.ic_menu_mapmode;
                break;
            case "repas":
            case "restauration":
                iconResource = android.R.drawable.ic_menu_share;
                break;
            case "transport":
                iconResource = android.R.drawable.ic_menu_send;
                break;
            default:
                iconResource = android.R.drawable.ic_menu_agenda;
                break;
        }
        imageView.setImageResource(iconResource);
    }

    @Override
    public int getItemCount() {
        return expenseGroups.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayDate, tvDayTotal;
        LinearLayout llExpensesContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayDate = itemView.findViewById(R.id.tv_day_date);
            tvDayTotal = itemView.findViewById(R.id.tv_day_total);
            llExpensesContainer = itemView.findViewById(R.id.ll_expenses_container);
        }
    }

    public void updateData(List<DailyExpensesGroup> newGroups) {
        this.expenseGroups.clear();
        this.expenseGroups.addAll(newGroups);
        notifyDataSetChanged();
    }
}