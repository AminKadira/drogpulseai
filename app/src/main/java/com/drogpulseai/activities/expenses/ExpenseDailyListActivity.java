package com.drogpulseai.activities.expenses;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drogpulseai.R;
import com.drogpulseai.adapters.DailyExpensesAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.DailyExpensesGroup;
import com.drogpulseai.models.Expense;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.utils.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExpenseDailyListActivity extends AppCompatActivity implements DailyExpensesAdapter.OnExpenseClickListener {

    // UI Components
    private Toolbar toolbar;
    private TextView tvPeriodInfo;
    private RecyclerView recyclerView;
    private DailyExpensesAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmptyList;
    private TextView tvTotalSummary;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;

    // Data
    private List<Expense> allExpenses;
    private List<DailyExpensesGroup> expenseGroups;
    private User currentUser;
    private double globalTotal = 0.0;
    private Date currentMonth;

    // Formats de date
    private final SimpleDateFormat dateFormatAPI = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat dateFormatDisplay = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_daily_list);

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);
        currentUser = sessionManager.getUser();
        currentMonth = new Date(); // Par défaut, mois courant

        // Initialisation des vues
        initializeViews();

        // Configuration de la toolbar
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setTitle(R.string.daily_expenses);
//        }

        // Configuration du RecyclerView
        setupRecyclerView();

        // Configuration des listeners
        setupListeners();

        // Mise à jour des informations de période
        updatePeriodInfo();

        // Chargement des frais
        loadExpenses();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvPeriodInfo = findViewById(R.id.tv_period_info);
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyList = findViewById(R.id.tv_empty_list);
        tvTotalSummary = findViewById(R.id.tv_total_summary);
    }

    private void setupRecyclerView() {
        expenseGroups = new ArrayList<>();
        adapter = new DailyExpensesAdapter(this, expenseGroups, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadExpenses);

        // Clic sur l'info de période pour changer de mois (à implémenter)
        tvPeriodInfo.setOnClickListener(v -> showMonthPicker());
    }

    private void updatePeriodInfo() {
        tvPeriodInfo.setText(String.format("Période: %s", monthFormat.format(currentMonth)));
    }

    private void loadExpenses() {
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Vérifier s'il y a une connexion Internet
        if (NetworkUtils.isNetworkAvailable(this)) {
            // Mode en ligne - charger depuis l'API
            apiService.getExpenses(currentUser.getId()).enqueue(new Callback<List<Expense>>() {
                @Override
                public void onResponse(Call<List<Expense>> call, Response<List<Expense>> response) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);

                    if (response.isSuccessful() && response.body() != null) {
                        allExpenses = response.body();
                        processExpenses(allExpenses);
                    } else {
                        Toast.makeText(ExpenseDailyListActivity.this, "Erreur lors du chargement des frais", Toast.LENGTH_LONG).show();
                        updateUI(new ArrayList<>());
                    }
                }

                @Override
                public void onFailure(Call<List<Expense>> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(ExpenseDailyListActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
                    updateUI(new ArrayList<>());
                }
            });
        } else {
            // Mode hors ligne
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "Mode hors ligne - chargement des frais impossible", Toast.LENGTH_SHORT).show();
            updateUI(new ArrayList<>());
        }
    }

    private void processExpenses(List<Expense> expenses) {
        // Filtrer les frais pour le mois sélectionné
        List<Expense> filteredExpenses = filterExpensesByMonth(expenses, currentMonth);

        // Grouper les frais par jour
        Map<String, DailyExpensesGroup> groupsMap = new HashMap<>();
        globalTotal = 0.0;

        // Parcourir les frais et les regrouper par jour
        for (Expense expense : filteredExpenses) {
            try {
                // Convertir la date du format API au format d'affichage
                Date expenseDate = dateFormatAPI.parse(expense.getDate());
                String dateKey = dateFormatAPI.format(expenseDate);

                // Créer ou récupérer le groupe pour cette date
                DailyExpensesGroup group = groupsMap.get(dateKey);
                if (group == null) {
                    group = new DailyExpensesGroup(dateKey, expenseDate);
                    groupsMap.put(dateKey, group);
                }

                // Ajouter le frais au groupe
                group.addExpense(expense);

                // Mettre à jour le total global
                globalTotal += expense.getAmount();

            } catch (ParseException e) {
                // Ignorer ce frais en cas d'erreur de format de date
                e.printStackTrace();
            }
        }

        // Convertir la map en liste
        List<DailyExpensesGroup> groups = new ArrayList<>(groupsMap.values());

        // Trier les groupes par date (plus récent en premier)
        Collections.sort(groups, (g1, g2) -> g2.getDate().compareTo(g1.getDate()));

        // Mettre à jour l'UI
        updateUI(groups);
    }

    private List<Expense> filterExpensesByMonth(List<Expense> expenses, Date month) {
        List<Expense> filtered = new ArrayList<>();

        // Configurer un calendrier pour le mois sélectionné
        Calendar cal = Calendar.getInstance();
        cal.setTime(month);
        int selectedYear = cal.get(Calendar.YEAR);
        int selectedMonth = cal.get(Calendar.MONTH);

        // Filtrer les frais pour ce mois
        for (Expense expense : expenses) {
            try {
                Date expenseDate = dateFormatAPI.parse(expense.getDate());
                cal.setTime(expenseDate);
                int expenseYear = cal.get(Calendar.YEAR);
                int expenseMonth = cal.get(Calendar.MONTH);

                if (expenseYear == selectedYear && expenseMonth == selectedMonth) {
                    filtered.add(expense);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return filtered;
    }

    private void updateUI(List<DailyExpensesGroup> groups) {
        // Mettre à jour les groupes
        expenseGroups.clear();
        expenseGroups.addAll(groups);
        adapter.notifyDataSetChanged();

        // Mettre à jour le total
        tvTotalSummary.setText(String.format(Locale.getDefault(), "Total: %.2f MAD", globalTotal));

        // Afficher un message si la liste est vide
        if (groups.isEmpty()) {
            tvEmptyList.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyList.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showMonthPicker() {
        // Cette méthode pourrait afficher un DatePickerDialog en mode mois-année uniquement
        // Pour simplifier, on peut utiliser un DatePickerDialog standard et ignorer le jour
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentMonth);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, dayOfMonth) -> {
                    // Mettre à jour le mois sélectionné
                    calendar.set(Calendar.YEAR, selectedYear);
                    calendar.set(Calendar.MONTH, selectedMonth);
                    currentMonth = calendar.getTime();

                    // Mettre à jour l'affichage de la période
                    updatePeriodInfo();

                    // Recharger les frais pour ce mois
                    if (allExpenses != null) {
                        processExpenses(allExpenses);
                    } else {
                        loadExpenses();
                    }
                },
                year,
                month,
                1 // Jour (pas important pour la sélection du mois)
        );

        datePickerDialog.show();
    }

    @Override
    public void onExpenseClick(Expense expense) {
        // Ouvrir le formulaire d'édition du frais
        Intent intent = new Intent(this, ExpenseFormActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("expense_id", expense.getId());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExpenses();
    }
}