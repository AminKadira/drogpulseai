package com.drogpulseai.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drogpulseai.R;
import com.drogpulseai.models.Expense;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    private final List<Expense> expenses;
    private final OnExpenseClickListener listener;
    private final LayoutInflater inflater;
    private final Context context;
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public ExpenseAdapter(Context context, List<Expense> expenses, OnExpenseClickListener listener) {
        this.context = context;
        this.expenses = expenses;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense expense = expenses.get(position);

        // Afficher les informations du frais
        holder.tvType.setText(expense.getType());
        holder.tvAmount.setText(String.format(Locale.getDefault(), "%.2f MAD", expense.getAmount()));
        holder.tvDescription.setText(expense.getDescription());

        // Formater la date
        try {
            Date date = inputFormat.parse(expense.getDate());
            holder.tvDate.setText(date != null ? outputFormat.format(date) : expense.getDate());
        } catch (ParseException e) {
            holder.tvDate.setText(expense.getDate());
        }

        // Définir l'icône en fonction du type de frais
        setExpenseIcon(holder.ivIcon, expense.getType());

        // Configurer le clic sur l'élément
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onExpenseClick(expense);
            }
        });
    }

    private void setExpenseIcon(ImageView imageView, String expenseType) {
        int iconResource;
        switch (expenseType.toLowerCase()) {
            case "carburant":
                iconResource = android.R.drawable.ic_menu_directions;
                break;
            case "hotel":
                iconResource = android.R.drawable.ic_menu_mylocation;
                break;
            case "autoroute":
                iconResource = android.R.drawable.ic_menu_mapmode;
                break;
            case "repas":
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
        return expenses.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDate, tvDescription, tvAmount;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tv_expense_type);
            tvDate = itemView.findViewById(R.id.tv_expense_date);
            tvDescription = itemView.findViewById(R.id.tv_expense_description);
            tvAmount = itemView.findViewById(R.id.tv_expense_amount);
            ivIcon = itemView.findViewById(R.id.iv_expense_icon);
        }
    }

    public void updateData(List<Expense> newExpenses) {
        expenses.clear();
        expenses.addAll(newExpenses);
        notifyDataSetChanged();
    }
}