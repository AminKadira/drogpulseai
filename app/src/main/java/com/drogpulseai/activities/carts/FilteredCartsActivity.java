package com.drogpulseai.activities.carts;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.drogpulseai.R;
import com.drogpulseai.adapters.CartAdapter;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FilteredCartsActivity extends AppCompatActivity implements CartAdapter.OnCartClickListener {

    private static final String TAG = "FilteredCartsActivity";

    // UI Components
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View progressBar;
    private TextView tvNoData;
    private ChipGroup statusChipGroup;
    private Chip chipPending, chipConfirmed, chipCancelled;
    private Spinner dateFilterSpinner;
    private Button btnCustomDate;
    private View customDateLayout;
    private TextInputEditText etStartDate, etEndDate;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;
    private User currentUser;

    // Filter values
    private List<String> selectedStatuses = new ArrayList<>();
    private String dateFilterType = "all"; // Default value
    private Date startDate;
    private Date endDate;

    // Date formatter
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Carts data
    private List<Map<String, Object>> carts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carts_list_filtered);

        // Set up the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.my_carts);
        }

        // Initialize utilities
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);
        currentUser = sessionManager.getUser();

        // Initialize UI components
        initializeViews();

        // Setup adapters and listeners
        setupSpinner();
        setupListeners();
        setupRecyclerView();

        // Initial data loading
        loadCarts();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        tvNoData = findViewById(R.id.tv_no_data);

        // Filter components
        statusChipGroup = findViewById(R.id.status_chip_group);
        chipPending = findViewById(R.id.chip_pending);
        chipConfirmed = findViewById(R.id.chip_confirmed);
        chipCancelled = findViewById(R.id.chip_cancelled);

        dateFilterSpinner = findViewById(R.id.date_filter_spinner);
        btnCustomDate = findViewById(R.id.btn_custom_date);
        customDateLayout = findViewById(R.id.custom_date_layout);
        etStartDate = findViewById(R.id.et_start_date);
        etEndDate = findViewById(R.id.et_end_date);

        // Set default filter values
        selectedStatuses.add("pending");
        selectedStatuses.add("confirmed");
        selectedStatuses.add("cancelled");
    }

    private void setupSpinner() {
        // Create array adapter for date filter spinner
        String[] dateFilters = {
                getString(R.string.date_all),
                getString(R.string.date_today),
                getString(R.string.date_yesterday),
                getString(R.string.date_this_week),
                getString(R.string.date_last_week),
                getString(R.string.date_this_month),
                getString(R.string.date_last_month),
                getString(R.string.date_this_year),
                getString(R.string.date_custom)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, dateFilters
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateFilterSpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadCarts);

        // Status chip group
        statusChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            selectedStatuses.clear();

            if (chipPending.isChecked()) selectedStatuses.add("pending");
            if (chipConfirmed.isChecked()) selectedStatuses.add("confirmed");
            if (chipCancelled.isChecked()) selectedStatuses.add("cancelled");

            // If no status is selected, select all
            if (selectedStatuses.isEmpty()) {
                chipPending.setChecked(true);
                chipConfirmed.setChecked(true);
                chipCancelled.setChecked(true);
                selectedStatuses.add("pending");
                selectedStatuses.add("confirmed");
                selectedStatuses.add("cancelled");
            }

            loadCarts();
        });

        // Date filter spinner
        dateFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();

                if (selected.equals(getString(R.string.date_custom))) {
                    dateFilterType = "custom";
                    customDateLayout.setVisibility(View.VISIBLE);

                    // Set default dates if not set
                    if (etStartDate.getText().toString().isEmpty()) {
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.DAY_OF_MONTH, -30); // Default to last 30 days
                        startDate = cal.getTime();
                        etStartDate.setText(displayDateFormat.format(startDate));
                    }

                    if (etEndDate.getText().toString().isEmpty()) {
                        endDate = new Date(); // Current date
                        etEndDate.setText(displayDateFormat.format(endDate));
                    }
                } else {
                    customDateLayout.setVisibility(View.GONE);

                    if (selected.equals(getString(R.string.date_all))) {
                        dateFilterType = "all";
                    } else if (selected.equals(getString(R.string.date_today))) {
                        dateFilterType = "today";
                    } else if (selected.equals(getString(R.string.date_yesterday))) {
                        dateFilterType = "yesterday";
                    } else if (selected.equals(getString(R.string.date_this_week))) {
                        dateFilterType = "this_week";
                    } else if (selected.equals(getString(R.string.date_last_week))) {
                        dateFilterType = "last_week";
                    } else if (selected.equals(getString(R.string.date_this_month))) {
                        dateFilterType = "this_month";
                    } else if (selected.equals(getString(R.string.date_last_month))) {
                        dateFilterType = "last_month";
                    } else if (selected.equals(getString(R.string.date_this_year))) {
                        dateFilterType = "this_year";
                    }

                    loadCarts();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Date picker dialogs
        etStartDate.setOnClickListener(v -> showDatePickerDialog(true));
        etEndDate.setOnClickListener(v -> showDatePickerDialog(false));
    }

    private void setupRecyclerView() {
        adapter = new CartAdapter(this, carts, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void showDatePickerDialog(boolean isStartDate) {
        final Calendar calendar = Calendar.getInstance();
        Date currentDate = isStartDate ? startDate : endDate;

        if (currentDate != null) {
            calendar.setTime(currentDate);
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    calendar.set(Calendar.YEAR, selectedYear);
                    calendar.set(Calendar.MONTH, selectedMonth);
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay);

                    Date selectedDate = calendar.getTime();
                    String formattedDate = displayDateFormat.format(selectedDate);

                    if (isStartDate) {
                        startDate = selectedDate;
                        etStartDate.setText(formattedDate);
                    } else {
                        endDate = selectedDate;
                        etEndDate.setText(formattedDate);
                    }

                    // Validate date range
                    validateDateRange();
                },
                year, month, day
        );

        datePickerDialog.setTitle(getString(R.string.select_date));
        datePickerDialog.show();
    }

    private void validateDateRange() {
        if (startDate != null && endDate != null) {
            if (startDate.after(endDate)) {
                Toast.makeText(this, R.string.error_end_date_before_start, Toast.LENGTH_SHORT).show();
                return;
            }

            // Both dates are valid, reload data
            loadCarts();
        }
    }

    private void loadCarts() {
        // Check network connection first
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Show loading state
        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Build filter parameters
        Map<String, Object> filters = new HashMap<>();
        filters.put("user_id", currentUser.getId());

        // Status filter
        if (selectedStatuses.size() < 3) { // If not all statuses are selected
            filters.put("status", selectedStatuses);
        }

        // Date filter
        if (!dateFilterType.equals("all")) {
            if (dateFilterType.equals("custom")) {
                if (startDate != null && endDate != null) {
                    filters.put("start_date", dateFormat.format(startDate));
                    filters.put("end_date", dateFormat.format(endDate));
                }
            } else {
                filters.put("date_filter", dateFilterType);
            }
        }

        // Add debug logging
        Log.d(TAG, "API Call Filters: " + filters.toString());

        try {
            // Try the filtering endpoint first
            Call<Map<String, Object>> call = apiService.getFilteredCarts(filters);
            call.enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    try {
                        Log.d(TAG, "Filter API responded with code: " + response.code());

                        // Special handling for network errors
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Filter API error: " + response.code() + " - " + response.message());

                            // Try the fallback endpoint for client errors (4xx)
                            if (response.code() >= 400 && response.code() < 500) {
                                Log.d(TAG, "Trying fallback API due to client error");
                                loadCartsFallback();
                                return;
                            }
                        }

                        // Process the response
                        processCartsResponse(response);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in filter API response callback", e);
                        loadCartsFallback();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Log.e(TAG, "Filter API call failed", t);
                    loadCartsFallback();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception when making filter API call", e);
            loadCartsFallback();
        }
    }

    /**
     * Fallback method to load carts using the standard endpoint
     */
    private void loadCartsFallback() {
        Log.d(TAG, "Using standard API fallback");
        try {
            Call<Map<String, Object>> call = apiService.getUserCartsFiltred(
                    currentUser.getId(), 1, 100);

            call.enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    try {
                        Log.d(TAG, "Standard API responded with code: " + response.code());
                        processCartsResponse(response);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in standard API response callback", e);
                        handleApiFailure(e);
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Log.e(TAG, "Standard API call failed", t);
                    handleApiFailure(t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception when making standard API call", e);
            handleApiFailure(e);
        }
    }

    /**
     * Process API response for cart data with improved error handling and type safety
     * Specific fix for the exact API response structure
     */
    private void processCartsResponse(Response<Map<String, Object>> response) {
        try {
            // First make sure UI elements exist
            if (progressBar == null || swipeRefreshLayout == null || tvNoData == null || recyclerView == null) {
                Log.e(TAG, "UI elements are null, activity might be destroyed");
                return;
            }

            // Update UI safely
            runOnUiThread(() -> {
                try {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                } catch (Exception e) {
                    Log.e(TAG, "Error updating UI elements", e);
                }
            });

            // Process response
            if (response != null && response.isSuccessful() && response.body() != null) {
                try {
                    // Parse response with thorough null and type checking
                    Map<String, Object> result = response.body();

                    // Log debugging info without the entire response to avoid log overflow
                    Log.d(TAG, "API Response structure: success=" + result.containsKey("success") +
                            ", data=" + result.containsKey("data") +
                            ", contains pagination=" + (result.containsKey("data") && ((Map<?,?>)result.get("data")).containsKey("pagination")));

                    // Check for success status - handle different response formats
                    boolean isSuccess = false;

                    // Try to get success flag - could be Boolean or String
                    if (result.containsKey("success")) {
                        Object successObj = result.get("success");
                        if (successObj instanceof Boolean) {
                            isSuccess = (Boolean) successObj;
                        } else if (successObj instanceof String) {
                            isSuccess = Boolean.parseBoolean((String) successObj);
                        } else if (successObj instanceof Number) {
                            isSuccess = ((Number) successObj).intValue() == 1;
                        }
                    }

                    if (isSuccess) {
                        // Based on the actual API response structure: {success=true, data={carts=[...], pagination={...}}}
                        List<Map<String, Object>> cartsList = new ArrayList<>();

                        // Get data object
                        if (result.containsKey("data")) {
                            Object dataObj = result.get("data");
                            if (dataObj instanceof Map) {
                                try {
                                    Map<String, Object> data = (Map<String, Object>) dataObj;

                                    // Get carts array from data
                                    if (data.containsKey("carts")) {
                                        Object cartsObj = data.get("carts");
                                        if (cartsObj instanceof List) {
                                            List<?> rawList = (List<?>) cartsObj;

                                            for (Object item : rawList) {
                                                try {
                                                    if (item instanceof Map) {
                                                        @SuppressWarnings("unchecked")
                                                        Map<String, Object> cartMap = (Map<String, Object>) item;
                                                        cartsList.add(normalizeCartData(cartMap));
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error processing cart item", e);
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing data object", e);
                                }
                            }
                        }

                        // Log info about extracted carts
                        Log.d(TAG, "Extracted " + cartsList.size() + " carts from response");

                        if (cartsList.isEmpty()) {
                            // No carts found or extraction failed
                            Log.w(TAG, "No carts found in response");
                            showNoCartsFound();
                            return;
                        }

                        // Filter carts based on our criteria
                        final List<Map<String, Object>> filteredCarts = filterCarts(cartsList);
                        Log.d(TAG, "After filtering: " + filteredCarts.size() + " carts remain");

                        // Update UI on main thread
                        runOnUiThread(() -> {
                            try {
                                // Update adapter data safely
                                if (carts != null) {
                                    carts.clear();
                                    carts.addAll(filteredCarts);
                                }

                                // Notify adapter safely
                                if (adapter != null) {
                                    adapter.notifyDataSetChanged();
                                }

                                // Show empty view if no carts
                                if (filteredCarts.isEmpty()) {
                                    if (tvNoData != null) tvNoData.setVisibility(View.VISIBLE);
                                    if (recyclerView != null) recyclerView.setVisibility(View.GONE);
                                } else {
                                    if (tvNoData != null) tvNoData.setVisibility(View.GONE);
                                    if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                                }

                                // Update title with count
                                if (getSupportActionBar() != null) {
                                    try {
                                        getSupportActionBar().setSubtitle(
                                                getString(R.string.total_carts, filteredCarts.size())
                                        );
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error setting subtitle", e);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating UI after successful API response", e);
                            }
                        });
                    } else {
                        // API returned an error
                        String message = result.containsKey("message") ?
                                result.get("message").toString() : "Erreur inconnue";
                        showErrorState("Erreur: " + message);
                    }
                } catch (ClassCastException e) {
                    Log.e(TAG, "Error parsing response: Class cast exception", e);
                    showErrorState("Format de réponse incorrect: " + e.getClass().getSimpleName());
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing response: " + e.getClass().getSimpleName(), e);
                    showErrorState("Erreur lors du traitement des données: " + e.getClass().getSimpleName());
                }
            } else {
                // HTTP error
                String errorMsg = "Erreur serveur: " + (response != null ? response.code() : "unknown");
                try {
                    if (response != null && response.errorBody() != null) {
                        errorMsg += " - " + response.errorBody().string();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading error body", e);
                }

                showErrorState(errorMsg);
            }
        } catch (Exception e) {
            Log.e(TAG, "Unhandled exception in processCartsResponse", e);
            showErrorState("Erreur inattendue: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Helper method to show no carts found message
     */
    private void showNoCartsFound() {
        runOnUiThread(() -> {
            try {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                if (carts != null) carts.clear();
                if (adapter != null) adapter.notifyDataSetChanged();

                if (tvNoData != null) {
                    tvNoData.setText(R.string.no_carts_found);
                    tvNoData.setVisibility(View.VISIBLE);
                }

                if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "Error showing no carts found", e);
            }
        });
    }

    /**
     * Normalize cart data to ensure consistent types for all fields
     */
    private Map<String, Object> normalizeCartData(Map<String, Object> cart) {
        // Create new map to avoid modifying the original
        Map<String, Object> normalizedCart = new HashMap<>(cart);

        // Ensure consistent types for critical fields
        // Convert ID to Double
        if (normalizedCart.containsKey("id")) {
            Object idObj = normalizedCart.get("id");
            Double doubleId = convertToDouble(idObj);
            normalizedCart.put("id", doubleId);
        } else {
            normalizedCart.put("id", 0.0);
        }

        // Ensure status is a string
        if (normalizedCart.containsKey("status")) {
            Object statusObj = normalizedCart.get("status");
            if (statusObj == null) {
                normalizedCart.put("status", "unknown");
            } else {
                normalizedCart.put("status", statusObj.toString());
            }
        } else {
            normalizedCart.put("status", "unknown");
        }

        // Convert numeric fields
        convertToDouble(normalizedCart, "contact_id");
        convertToDouble(normalizedCart, "user_id");
        convertToDouble(normalizedCart, "items_count");

        // Handle total_quantity which can be string or number
        if (normalizedCart.containsKey("total_quantity")) {
            Object qtyObj = normalizedCart.get("total_quantity");
            if (qtyObj instanceof String) {
                try {
                    normalizedCart.put("total_quantity", Double.parseDouble((String) qtyObj));
                } catch (NumberFormatException e) {
                    normalizedCart.put("total_quantity", 0.0);
                }
            } else if (!(qtyObj instanceof Number)) {
                normalizedCart.put("total_quantity", 0.0);
            }
        }

        // Handle total_amount which can be string or number
        if (normalizedCart.containsKey("total_amount")) {
            Object amountObj = normalizedCart.get("total_amount");
            if (amountObj instanceof String) {
                try {
                    normalizedCart.put("total_amount", Double.parseDouble((String) amountObj));
                } catch (NumberFormatException e) {
                    normalizedCart.put("total_amount", 0.0);
                }
            } else if (!(amountObj instanceof Number)) {
                normalizedCart.put("total_amount", 0.0);
            }
        }

        // Ensure contact name is present
        if (!normalizedCart.containsKey("contact_name") || normalizedCart.get("contact_name") == null) {
            normalizedCart.put("contact_name", "Client inconnu");
        }

        return normalizedCart;
    }

    /**
     * Convert a value to Double
     */
    private Double convertToDouble(Object value) {
        if (value == null) {
            return 0.0;
        }

        if (value instanceof Double) {
            return (Double) value;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }

        return 0.0;
    }

    /**
     * Ensure a field in a map is a Double
     */
    private void convertToDouble(Map<String, Object> map, String field) {
        if (map.containsKey(field)) {
            Object value = map.get(field);
            map.put(field, convertToDouble(value));
        } else {
            map.put(field, 0.0);
        }
    }

    /**
     * Helper method to safely extract carts list from various response formats
     */
    private List<Map<String, Object>> extractCartsList(Object cartsObj) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (!(cartsObj instanceof List)) {
            return result;
        }

        List<?> rawList = (List<?>) cartsObj;
        for (Object item : rawList) {
            if (item instanceof Map) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cartMap = (Map<String, Object>) item;

                    // Ensure required fields exist to prevent crashes later
                    if (!cartMap.containsKey("id")) {
                        Log.w(TAG, "Cart missing ID field, adding default");
                        cartMap.put("id", 0.0); // Default ID if missing
                    }

                    if (!cartMap.containsKey("status")) {
                        Log.w(TAG, "Cart missing status field, adding default");
                        cartMap.put("status", "unknown"); // Default status if missing
                    }

                    // Handle integer values that might be returned as different types
                    if (cartMap.containsKey("id") && !(cartMap.get("id") instanceof Double)) {
                        Object rawId = cartMap.get("id");
                        double doubleId;
                        if (rawId instanceof Integer) {
                            doubleId = ((Integer) rawId).doubleValue();
                        } else if (rawId instanceof String) {
                            doubleId = Double.parseDouble((String) rawId);
                        } else if (rawId instanceof Number) {
                            doubleId = ((Number) rawId).doubleValue();
                        } else {
                            doubleId = 0.0;
                        }
                        cartMap.put("id", doubleId);
                    }

                    result.add(cartMap);
                } catch (Exception e) {
                    Log.e(TAG, "Error converting cart item", e);
                }
            }
        }

        return result;
    }
    /**
     * Helper method to show error state on UI thread
     */
    private void showErrorState(final String message) {
        Log.e(TAG, "Error state: " + message);
        runOnUiThread(() -> {
            if (tvNoData != null && recyclerView != null) {
                tvNoData.setText(message);
                tvNoData.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);

                Toast.makeText(FilteredCartsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Improved error handling for API failures
     */
    private void handleApiFailure(Throwable t) {
        try {
            Log.e(TAG, "API call failed", t);

            // Get more detailed error info
            String errorMessage;
            if (t instanceof java.net.SocketTimeoutException) {
                errorMessage = "Le délai d'attente de la connexion a expiré. Veuillez réessayer.";
            } else if (t instanceof java.io.IOException) {
                errorMessage = "Erreur de connexion réseau. Veuillez vérifier votre connexion.";
            } else if (t instanceof retrofit2.HttpException) {
                retrofit2.HttpException httpException = (retrofit2.HttpException) t;
                errorMessage = "Erreur serveur: " + httpException.code() + " " + httpException.message();
            } else if (t instanceof java.net.UnknownHostException) {
                errorMessage = "Serveur introuvable. Vérifiez votre connexion internet.";
            } else if (t instanceof com.google.gson.JsonSyntaxException) {
                errorMessage = "Erreur de format de données. Contactez le support.";
            } else {
                errorMessage = "Erreur réseau: " + t.getMessage();
            }

            // Update UI on main thread
            final String finalErrorMessage = errorMessage;
            runOnUiThread(() -> {
                try {
                    // Hide loading indicators
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    // Show error message
                    if (tvNoData != null && recyclerView != null) {
                        tvNoData.setText(finalErrorMessage);
                        tvNoData.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }

                    // Show toast message
                    Toast.makeText(FilteredCartsActivity.this, finalErrorMessage, Toast.LENGTH_LONG).show();

                    // Clear and notify adapter
                    if (carts != null && adapter != null) {
                        carts.clear();
                        adapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating UI after API failure", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in handleApiFailure", e);
        }
    }

    /**
     * Filter carts based on selected criteria with improved error handling
     */
    private List<Map<String, Object>> filterCarts(List<Map<String, Object>> allCarts) {
        List<Map<String, Object>> result = new ArrayList<>();

        // Return empty list if input is null
        if (allCarts == null) {
            Log.w(TAG, "Input carts list is null");
            return result;
        }

        try {
            for (Map<String, Object> cart : allCarts) {
                if (cart == null) {
                    continue; // Skip null carts
                }

                try {
                    // Apply status filter
                    Object statusObj = cart.get("status");
                    if (statusObj == null) {
                        Log.w(TAG, "Cart has null status");
                        continue;
                    }

                    String status = statusObj.toString();
                    if (!selectedStatuses.contains(status)) {
                        continue;
                    }

                    // Apply date filter only if not "all"
                    if (!dateFilterType.equals("all")) {
                        Object createdAtObj = cart.get("created_at");
                        if (createdAtObj == null) {
                            Log.w(TAG, "Cart has null created_at date");
                            continue;
                        }

                        String createdAt = createdAtObj.toString();
                        if (!isWithinDateRange(createdAt)) {
                            continue;
                        }
                    }

                    // Cart passed all filters
                    result.add(cart);
                } catch (Exception e) {
                    Log.e(TAG, "Error filtering cart: " + e.getMessage(), e);
                    // Continue to next cart instead of failing completely
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in filterCarts", e);
        }

        return result;
    }
    /**
     * Check if a date is within the selected range with improved error handling
     */
    private boolean isWithinDateRange(String dateStr) {
        // Default to true for "all" filter type to avoid issues
        if (dateFilterType.equals("all")) {
            return true;
        }

        // Safety check for null or empty date
        if (dateStr == null || dateStr.isEmpty()) {
            Log.w(TAG, "Empty date string provided to isWithinDateRange");
            return false;
        }

        try {
            // Parse the cart date
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date cartDate = dateFormat.parse(dateStr);

            if (cartDate == null) {
                Log.w(TAG, "Failed to parse date: " + dateStr);
                return false;
            }

            // For custom date range
            if (dateFilterType.equals("custom")) {
                // If dates aren't set, default to include
                if (startDate == null || endDate == null) {
                    return true;
                }

                // Add one day to end date to include the whole day
                Calendar cal = Calendar.getInstance();
                cal.setTime(endDate);
                cal.add(Calendar.DAY_OF_MONTH, 1);
                Date adjustedEndDate = cal.getTime();

                return !cartDate.before(startDate) && cartDate.before(adjustedEndDate);
            }

            // For predefined ranges
            Calendar cal = Calendar.getInstance();
            Calendar cartCal = Calendar.getInstance();
            cartCal.setTime(cartDate);

            switch (dateFilterType) {
                case "today":
                    return isSameDay(cartCal, cal);

                case "yesterday":
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                    return isSameDay(cartCal, cal);

                case "this_week":
                    cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                    return !cartDate.before(cal.getTime());

                case "last_week":
                    cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                    Date thisWeekStart = cal.getTime();
                    cal.add(Calendar.WEEK_OF_YEAR, -1);
                    Date lastWeekStart = cal.getTime();
                    return cartDate.after(lastWeekStart) && cartDate.before(thisWeekStart);

                case "this_month":
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    return !cartDate.before(cal.getTime());

                case "last_month":
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    Date thisMonthStart = cal.getTime();
                    cal.add(Calendar.MONTH, -1);
                    Date lastMonthStart = cal.getTime();
                    return cartDate.after(lastMonthStart) && cartDate.before(thisMonthStart);

                case "this_year":
                    cal.set(Calendar.DAY_OF_YEAR, 1);
                    return !cartDate.before(cal.getTime());

                default:
                    // Unknown filter type, default to include
                    return true;
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateStr, e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in isWithinDateRange: " + e.getMessage(), e);
            return false;
        }
    }
    /**
     * Check if two calendar instances represent the same day
     */
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public void onCartClick(Map<String, Object> cart) {
        // Navigate to cart details
        int cartId = ((Double) cart.get("id")).intValue();
        Log.d(TAG, "Cart clicked: " + cartId);

        // For now, just show a toast
        Toast.makeText(this, "Panier #" + cartId + " sélectionné", Toast.LENGTH_SHORT).show();

        // TODO: Navigate to cart details activity
         Intent intent = new Intent(this, CartDetailsActivity.class);
         intent.putExtra("cart_id", cartId);
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
}