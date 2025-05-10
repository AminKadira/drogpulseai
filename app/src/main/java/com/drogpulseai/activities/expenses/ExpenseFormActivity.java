package com.drogpulseai.activities.expenses;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.drogpulseai.R;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Expense;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.FileUtils;
import com.drogpulseai.utils.FormValidator;
import com.drogpulseai.utils.ImageHelper;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.utils.SessionManager;
import com.drogpulseai.utils.ValidationMap;
import com.drogpulseai.utils.NetworkResult;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity for creating or editing expense records
 */
public class ExpenseFormActivity extends AppCompatActivity implements ImageHelper.ImageSelectionCallback {

    // Constants
    private static final String MODE_CREATE = "create";
    private static final String MODE_EDIT = "edit";

    // UI Components
    private EditText etAmount;
    private EditText etDate;
    private EditText etDescription;
    private Spinner spinnerType;
    private ImageView ivReceipt;
    private Button btnAddReceipt;
    private Button btnSave;
    private ProgressBar progressBar;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;
    private User currentUser;
    private ImageHelper imageHelper;
    private FormValidator formValidator;
    private ValidationMap validationMap;

    // Data
    private String mode;
    private int expenseId;
    private Expense currentExpense;
    private Uri receiptImageUri;
    private String photoPath; // Path to the uploaded photo

    // Date format
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_expense_form);

            // Initialize utilities
            apiService = ApiClient.getApiService();
            sessionManager = new SessionManager(this);
            imageHelper = new ImageHelper(this, this);
            formValidator = new FormValidator(this);

            // Get the current user
            currentUser = sessionManager.getUser();
            if (currentUser == null) {
                finish();
                return;
            }

            // Determine mode (create or edit)
            Intent intent = getIntent();
            mode = intent.getStringExtra("mode");
            if (mode == null) {
                mode = MODE_CREATE;
            }

            // If edit mode, get expense ID
            if (MODE_EDIT.equals(mode)) {
                expenseId = intent.getIntExtra("expense_id", -1);
                if (expenseId == -1) {
                    Toast.makeText(this, R.string.error_loading_expense, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }

            // Configure ActionBar
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(MODE_CREATE.equals(mode) ?
                        R.string.add_expense : R.string.edit_expense);
            }

            // Initialize UI
            initializeViews();
            setupValidation();
            setupListeners();

            // If edit mode, load expense data
            if (MODE_EDIT.equals(mode)) {
                loadExpenseData();
            }
        } catch (Exception e) {
            // Capturer et logger toute exception pour faciliter le débogage
            Log.e("ExpenseFormActivity", "Erreur dans onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Erreur lors du chargement de l'écran: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish(); // Fermer l'activité en cas d'erreur critique
        }

    }

    /**
     * Initialize views
     */
    private void initializeViews() {
        etAmount = findViewById(R.id.et_expense_amount);
        etDate = findViewById(R.id.et_expense_date);
        etDescription = findViewById(R.id.et_expense_description);
        spinnerType = findViewById(R.id.spinner_expense_type);
        ivReceipt = findViewById(R.id.iv_receipt);
        btnAddReceipt = findViewById(R.id.btn_add_receipt);
        btnSave = findViewById(R.id.btn_save_expense);
        progressBar = findViewById(R.id.progress_bar);

        // Set today's date by default
        etDate.setText(dateFormat.format(calendar.getTime()));

        // Set up type spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.expense_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
    }

    /**
     * Set up form validation
     */
    private void setupValidation() {
        validationMap = new ValidationMap();
        validationMap.add(etAmount,
                formValidator.required(getString(R.string.field_required)),
                formValidator.decimal(getString(R.string.invalid_amount)));
        validationMap.add(etDate,
                formValidator.required(getString(R.string.field_required)));
    }

    /**
     * Set up event listeners
     */
    private void setupListeners() {
        // Date selection
        etDate.setOnClickListener(v -> showDatePicker());

        // Receipt photo
        btnAddReceipt.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, R.string.add_photo_no_connection, Toast.LENGTH_SHORT).show();
                return;
            }
            imageHelper.showImageSourceDialog();
        });

        // Save button
        btnSave.setOnClickListener(v -> saveExpense());
    }

    /**
     * Show date picker dialog
     */
    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            etDate.setText(dateFormat.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Load expense data for editing
     */
    private void loadExpenseData() {
        setLoading(true);

        apiService.getExpenseDetails(expenseId).enqueue(new Callback<Expense>() {
            @Override
            public void onResponse(Call<Expense> call, Response<Expense> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    currentExpense = response.body();
                    populateForm(currentExpense);
                } else {
                    Toast.makeText(ExpenseFormActivity.this,
                            R.string.error_loading_expense, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Expense> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ExpenseFormActivity.this,
                        getString(R.string.error_network) + ": " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Populate form with expense data
     */
    private void populateForm(Expense expense) {
        etAmount.setText(String.valueOf(expense.getAmount()));
        etDate.setText(expense.getDate().toString());
        etDescription.setText(expense.getDescription());

        // Select the expense type in spinner
        ArrayAdapter adapter = (ArrayAdapter) spinnerType.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(expense.getType())) {
                spinnerType.setSelection(i);
                break;
            }
        }

        // Load receipt photo if available
        photoPath = expense.getReceiptPhotoUrl();
        if (photoPath != null && !photoPath.isEmpty()) {
            imageHelper.displayImage(photoPath, ivReceipt);
            btnAddReceipt.setText(R.string.change_receipt);
        }
    }

    /**
     * Save expense (create or update)
     */
    private void saveExpense() {
        // Validate form
        if (!formValidator.validate(validationMap)) {
            return;
        }

        // Get values from form
        String type = spinnerType.getSelectedItem().toString();
        double amount;
        try {
            amount = Double.parseDouble(etAmount.getText().toString().trim());
        } catch (NumberFormatException e) {
            etAmount.setError(getString(R.string.invalid_amount));
            return;
        }

        Date date;
        try {
            date = dateFormat.parse(etDate.getText().toString().trim());
        } catch (ParseException e) {
            etDate.setError(getString(R.string.field_required));
            return;
        }

        String description = etDescription.getText().toString().trim();

        // Upload receipt photo if selected
        if (receiptImageUri != null) {
            uploadReceiptPhoto(type, amount, date, description);
        } else {
            // Use existing photo path in edit mode
            saveExpenseToApi(type, amount, date, description, photoPath);
        }
    }

    /**
     * Upload receipt photo
     */
    private void uploadReceiptPhoto(String type, double amount, Date date, String description) {
        setLoading(true);

        // Create a temporary file from the image URI
        File imageFile = FileUtils.getFileFromUri(this, receiptImageUri);
        if (imageFile == null) {
            setLoading(false);
            Toast.makeText(this, R.string.error_processing_image, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create multipart request
        RequestBody userId = RequestBody.create(MediaType.parse("text/plain"),
                String.valueOf(currentUser.getId()));

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part photoPart = MultipartBody.Part.createFormData("photo",
                imageFile.getName(), requestFile);

        // Upload photo
        apiService.uploadReceiptPhoto(photoPart, userId).enqueue(new Callback<NetworkResult<String>>() {
            @Override
            public void onResponse(Call<NetworkResult<String>> call,
                                   Response<NetworkResult<String>> response) {

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Get the photo URL from response
                    String photoUrl = response.body().getData();
                    // Save expense with the photo URL
                    saveExpenseToApi(type, amount, date, description, photoUrl);
                } else {
                    setLoading(false);
                    Toast.makeText(ExpenseFormActivity.this,
                            R.string.error_upload_photo, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NetworkResult<String>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ExpenseFormActivity.this,
                        getString(R.string.error_network) + ": " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Save expense to API
     */
    private void saveExpenseToApi(String type, double amount, Date date, String description,
                                  String photoUrl) {

        // Create or update Expense object
        Expense expense = new Expense();

        if (MODE_EDIT.equals(mode) && currentExpense != null) {
            expense.setId(currentExpense.getId());
        }

        expense.setType(type);
        expense.setAmount(amount);
        expense.setDate(date);
        expense.setDescription(description);
        expense.setReceiptPhotoUrl(photoUrl);
        expense.setUserId(currentUser.getId());

        // API call based on mode
        Call<NetworkResult<Expense>> call;

        if (MODE_CREATE.equals(mode)) {
            call = apiService.createExpense(expense);
        } else {
            call = apiService.updateExpense(expense);
        }

        call.enqueue(new Callback<NetworkResult<Expense>>() {
            @Override
            public void onResponse(Call<NetworkResult<Expense>> call,
                                   Response<NetworkResult<Expense>> response) {

                setLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Show success message
                    Toast.makeText(ExpenseFormActivity.this,
                            MODE_CREATE.equals(mode) ?
                                    R.string.expense_created : R.string.expense_updated,
                            Toast.LENGTH_SHORT).show();

                    // Return to list activity
                    setResult(RESULT_OK);
                    finish();
                } else {
                    // Show error message
                    String message = response.body() != null ?
                            response.body().getMessage() : getString(R.string.error_server_connection);
                    Toast.makeText(ExpenseFormActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NetworkResult<Expense>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ExpenseFormActivity.this,
                        getString(R.string.error_network) + ": " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Handle image selection callback
     */
    @Override
    public void onImageSelected(Uri imageUri) {
        receiptImageUri = imageUri;
        ivReceipt.setImageURI(imageUri);
        btnAddReceipt.setText(R.string.change_receipt);
    }

    /**
     * Handle activity result for image picking
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imageHelper.handleActivityResult(requestCode, resultCode, data);
    }

    /**
     * Set loading state
     */
    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        etAmount.setEnabled(!isLoading);
        etDate.setEnabled(!isLoading);
        etDescription.setEnabled(!isLoading);
        spinnerType.setEnabled(!isLoading);
        btnAddReceipt.setEnabled(!isLoading);
        btnSave.setEnabled(!isLoading);
    }

    /**
     * Handle back button
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Clean up resources
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageHelper != null) {
            imageHelper.cleanup();
        }
    }
}