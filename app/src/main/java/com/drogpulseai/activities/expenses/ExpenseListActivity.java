package com.drogpulseai.activities.expenses;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drogpulseai.R;
import com.drogpulseai.adapters.ExpenseAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Expense;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExpenseListActivity extends AppCompatActivity implements ExpenseAdapter.OnExpenseClickListener {

    // UI Components
    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView tvEmptyList;
    private FloatingActionButton fabAddExpense;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;

    // Data
    private List<Expense> expenses;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        // Configuration de la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.expenses);
        }

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);
        currentUser = sessionManager.getUser();

        // Initialisation des vues
        initializeViews();

        // Configuration du RecyclerView
        setupRecyclerView();

        // Configuration des listeners
        setupListeners();

        // Chargement des frais
        loadExpenses();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        tvEmptyList = findViewById(R.id.tv_empty_list);
        fabAddExpense = findViewById(R.id.fab_add_expense);
    }

    private void setupRecyclerView() {
        expenses = new ArrayList<>();
        adapter = new ExpenseAdapter(this, expenses, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadExpenses);

        // Bouton d'ajout de frais
        fabAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(ExpenseListActivity.this, ExpenseFormActivity.class);
            intent.putExtra("mode", "create");
            startActivity(intent);
        });
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
                        expenses.clear();
                        expenses.addAll(response.body());
                        adapter.notifyDataSetChanged();

                        updateEmptyView();
                    } else {
                        Toast.makeText(ExpenseListActivity.this, "Erreur lors du chargement des frais", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<List<Expense>> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(ExpenseListActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_LONG).show();
                    updateEmptyView();
                }
            });
        } else {
            // Mode hors ligne
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "Mode hors ligne - chargement des frais impossible", Toast.LENGTH_SHORT).show();
            updateEmptyView();
        }
    }

    private void updateEmptyView() {
        if (expenses.isEmpty()) {
            tvEmptyList.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyList.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.expenses_menu, menu);
        return true;
    }
    @Override
    public void onExpenseClick(Expense expense) {
        Intent intent = new Intent(ExpenseListActivity.this, ExpenseFormActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("expense_id", expense.getId());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_daily_view) {
            // Lancer l'activité de vue journalière
            startActivity(new Intent(this, ExpenseDailyListActivity.class));
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