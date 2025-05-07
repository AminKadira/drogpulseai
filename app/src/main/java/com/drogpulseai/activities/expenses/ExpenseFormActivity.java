package com.drogpulseai.activities.expenses;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.drogpulseai.R;
import com.drogpulseai.api.ApiClient;
import com.drogpulseai.api.ApiService;
import com.drogpulseai.models.Expense;
import com.drogpulseai.models.User;
import com.drogpulseai.utils.CameraPermissionHelper;
import com.drogpulseai.utils.FileUtils;
import com.drogpulseai.utils.ImageHelper;
import com.drogpulseai.utils.NetworkResult;
import com.drogpulseai.utils.NetworkUtils;
import com.drogpulseai.utils.SessionManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExpenseFormActivity extends AppCompatActivity implements CameraPermissionHelper.PermissionCallback, ImageHelper.ImageSelectionCallback {

    // Constants
    private static final String MODE_CREATE = "create";
    private static final String MODE_EDIT = "edit";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;

    // UI Components
    private ImageView ivReceiptPhoto;
    private Button btnAddReceipt, btnSave, btnDelete;
    private AutoCompleteTextView etExpenseType;
    private TextInputEditText etAmount, etDate, etDescription;
    private ProgressBar progressBar;

    // Utilities
    private ApiService apiService;
    private SessionManager sessionManager;
    private ImageHelper imageHelper;
    private CameraPermissionHelper cameraPermissionHelper;

    // Data
    private String mode;
    private int expenseId = -1;
    private User currentUser;
    private Expense currentExpense;
    private String receiptPhotoPath;
    private Uri selectedImageUri;
    private Calendar calendar;
    private SimpleDateFormat dateFormat;

    // Types de frais prédéfinis
    private final String[] expenseTypes = {
            "Carburant", "Hôtel", "Restauration", "Autoroute", "Transport", "Fournitures", "Autre"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_form);

        // Récupérer les extras de l'intent
        mode = getIntent().getStringExtra("mode");
        if (MODE_EDIT.equals(mode)) {
            expenseId = getIntent().getIntExtra("expense_id", -1);
        }

        // Configuration de la barre d'action
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(MODE_CREATE.equals(mode) ?
                    R.string.add_expense : R.string.edit_expense);
        }

        // Initialisation des utilitaires
        apiService = ApiClient.getApiService();
        sessionManager = new SessionManager(this);
        currentUser = sessionManager.getUser();
        imageHelper = new ImageHelper(this, this);
        cameraPermissionHelper = new CameraPermissionHelper(this, this);
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Initialisation des vues
        initializeViews();

        // Configuration des listeners
        setupListeners();

        // Configuration de la liste déroulante des types de frais
        setupSpinner();

        // Charger les données si en mode édition
        if (MODE_EDIT.equals(mode) && expenseId > 0) {
            loadExpenseDetails();
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            // En mode création, initialiser avec la date du jour
            etDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
        }
    }

    private void initializeViews() {
        ivReceiptPhoto = findViewById(R.id.iv_receipt_photo);
        btnAddReceipt = findViewById(R.id.btn_add_receipt);
        etExpenseType = findViewById(R.id.et_expense_type);
        etAmount = findViewById(R.id.et_expense_amount);
        etDate = findViewById(R.id.et_expense_date);
        etDescription = findViewById(R.id.et_expense_description);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, expenseTypes);
        etExpenseType.setAdapter(adapter);
    }

    private void setupListeners() {
        // Bouton d'ajout de photo
        btnAddReceipt.setOnClickListener(v -> showImageSourceDialog());

        // Champ de date avec DatePicker
        etDate.setOnClickListener(v -> showDatePickerDialog());

        // Bouton d'enregistrement
        btnSave.setOnClickListener(v -> saveExpense());

        // Bouton de suppression
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Mettre à jour le champ date
                    etDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showImageSourceDialog() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.add_photo_no_connection, Toast.LENGTH_SHORT).show();
            return;
        }

        imageHelper.showImageSourceDialog();
    }

    private void loadExpenseDetails() {
        setLoading(true);

        apiService.getExpenseDetails(expenseId).enqueue(new Callback<Expense>() {
            @Override
            public void onResponse(@NonNull Call<Expense> call, @NonNull Response<Expense> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    currentExpense = response.body();
                    displayExpenseDetails(currentExpense);
                } else {
                    Toast.makeText(ExpenseFormActivity.this, R.string.error_loading_expense, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Expense> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(ExpenseFormActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayExpenseDetails(Expense expense) {
        etExpenseType.setText(expense.getType());
        etAmount.setText(String.format(Locale.getDefault(), "%.2f", expense.getAmount()));
        etDescription.setText(expense.getDescription());

        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateFormat.parse(expense.getDate()));
            calendar = cal;
            etDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime()));
        } catch (Exception e) {
            etDate.setText(expense.getDate());
        }

        // Charger l'image du reçu si disponible
        if (expense.getReceiptPhotoUrl() != null && !expense.getReceiptPhotoUrl().isEmpty()) {
            receiptPhotoPath = expense.getReceiptPhotoUrl();
            loadReceiptImage(receiptPhotoPath);
            btnAddReceipt.setText(R.string.change_receipt);
        }
    }

    private void loadReceiptImage(String photoUrl) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return;
        }

        String fullUrl;
        if (photoUrl.startsWith("http") || photoUrl.startsWith("https")) {
            fullUrl = photoUrl;
        } else {
            String baseUrl = ApiClient.getBaseUrl();
            if (!baseUrl.endsWith("/") && !photoUrl.startsWith("/")) {
                baseUrl += "/";
            }
            fullUrl = baseUrl + photoUrl;
        }

        Glide.with(this)
                .load(fullUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .centerCrop()
                .into(ivReceiptPhoto);

        btnAddReceipt.setVisibility(View.GONE);
    }

    private void saveExpense() {
        if (!validateForm()) {
            return;
        }

        // Vérifier la connectivité réseau
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Préparer les données
        String type = etExpenseType.getText().toString().trim();
        double amount;
        try {
            amount = Double.parseDouble(etAmount.getText().toString().replace(",", "."));
        } catch (NumberFormatException e) {
            amount = 0.0;
        }
        String date = dateFormat.format(calendar.getTime());
        String description = etDescription.getText().toString().trim();

        // Si nous avons une nouvelle image à télécharger, faire cela d'abord
        if (selectedImageUri != null) {
            uploadReceiptPhoto(selectedImageUri, type, amount, date, description);
        } else {
            // Pas de nouvelle image, sauvegarder directement
            saveExpenseData(type, amount, date, description, receiptPhotoPath);
        }
    }

    private void uploadReceiptPhoto(Uri imageUri, String type, double amount, String date, String description) {
        File imageFile = FileUtils.compressAndResizeImage(this, imageUri, 1024, 1024);
        if (imageFile == null) {
            Toast.makeText(this, R.string.error_processing_image, Toast.LENGTH_SHORT).show();
            setLoading(false);
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part photoPart = MultipartBody.Part.createFormData("photo", imageFile.getName(), requestFile);
        RequestBody userId = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(currentUser.getId()));

        apiService.uploadReceiptPhoto(photoPart, userId).enqueue(new Callback<NetworkResult<String>>() {
            @Override
            public void onResponse(@NonNull Call<NetworkResult<String>> call, @NonNull Response<NetworkResult<String>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    String photoUrl = response.body().getData();
                    saveExpenseData(type, amount, date, description, photoUrl);
                } else {
                    setLoading(false);
                    Toast.makeText(ExpenseFormActivity.this, R.string.error_upload_photo, Toast.LENGTH_SHORT).show();
                }

                // Nettoyer le fichier temporaire
                if (imageFile.exists()) {
                    imageFile.delete();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NetworkResult<String>> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(ExpenseFormActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();

                // Nettoyer le fichier temporaire
                if (imageFile.exists()) {
                    imageFile.delete();
                }
            }
        });
    }

    private void saveExpenseData(String type, double amount, String date, String description, String photoUrl) {
        // Créer ou mettre à jour l'objet de frais
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

        // Appel API approprié selon le mode
        Call<NetworkResult<Expense>> call;
        if (MODE_CREATE.equals(mode)) {
            call = apiService.createExpense(expense);
        } else {
            call = apiService.updateExpense(expense);
        }

        call.enqueue(new Callback<NetworkResult<Expense>>() {
            @Override
            public void onResponse(@NonNull Call<NetworkResult<Expense>> call, @NonNull Response<NetworkResult<Expense>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ExpenseFormActivity.this,
                            MODE_CREATE.equals(mode) ? R.string.expense_created : R.string.expense_updated,
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : getString(R.string.error_server_connection);
                    Toast.makeText(ExpenseFormActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NetworkResult<Expense>> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(ExpenseFormActivity.this, R.string.error_network, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_expense)
                .setMessage(R.string.delete_expense_confirmation)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> deleteExpense())
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteExpense() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        apiService.deleteExpense(expenseId).enqueue(new Callback<NetworkResult<Void>>() {
            @Override
            public void onResponse(@NonNull Call<NetworkResult<Void>> call, @NonNull Response<NetworkResult<Void>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ExpenseFormActivity.this, R.string.expense_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String errorMsg = response.body() != null ? response.body().getMessage() : getString(R.string.error_server_connection);
                    Toast.makeText(ExpenseFormActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NetworkResult<Void>> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(ExpenseFormActivity.this, R.string.error_network, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;

        String type = etExpenseType.getText().toString().trim();
        String amount = etAmount.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        // Validation du type
        if (type.isEmpty()) {
            etExpenseType.setError(getString(R.string.field_required));
            valid = false;
        } else {
            etExpenseType.setError(null);
        }

        // Validation du montant
        if (amount.isEmpty()) {
            etAmount.setError(getString(R.string.field_required));
            valid = false;
        } else {
            try {
                double value = Double.parseDouble(amount.replace(",", "."));
                if (value <= 0) {
                    etAmount.setError(getString(R.string.invalid_amount));
                    valid = false;
                } else {
                    etAmount.setError(null);
                }
            } catch (NumberFormatException e) {
                etAmount.setError(getString(R.string.invalid_amount));
                valid = false;
            }
        }

        // Validation de la date
        if (date.isEmpty()) {
            etDate.setError(getString(R.string.field_required));
            valid = false;
        } else {
            etDate.setError(null);
        }

        return valid;
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoading);
        btnAddReceipt.setEnabled(!isLoading);
        etExpenseType.setEnabled(!isLoading);
        etAmount.setEnabled(!isLoading);
        etDate.setEnabled(!isLoading);
        etDescription.setEnabled(!isLoading);
        if (MODE_EDIT.equals(mode)) {
          //  btnDelete.setEnabled(!isLoading);
        }
    }

    @Override
    public void onPermissionGranted() {
        // Permission caméra accordée, lancer l'appareil photo
        imageHelper.showImageSourceDialog();
    }

    @Override
    public void onPermissionDenied() {
        Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onImageSelected(Uri imageUri) {
        if (imageUri != null) {
            selectedImageUri = imageUri;

            // Afficher l'image sélectionnée
            Glide.with(this)
                    .load(imageUri)
                    .centerCrop()
                    .into(ivReceiptPhoto);

            btnAddReceipt.setVisibility(View.GONE);
            btnAddReceipt.setText(R.string.change_receipt);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imageHelper.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraPermissionHelper.handlePermissionResult(requestCode, permissions, grantResults);
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