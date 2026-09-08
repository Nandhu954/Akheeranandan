package com.campusfind.app.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusfind.app.R;
import com.campusfind.app.database.DatabaseHelper;
import com.campusfind.app.models.Item;
import com.campusfind.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ReportLostActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout tilItemName, tilCategory, tilLocation, tilDate, tilDescription;
    private TextInputEditText etItemName, etLocation, etDate, etDescription;
    private AutoCompleteTextView actvCategory;
    private MaterialButton btnSubmitLost;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_lost);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupCategoryDropdown();
        setupDatePicker();
        setupSubmitButton();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarReportLost);
        tilItemName = findViewById(R.id.tilItemName);
        tilCategory = findViewById(R.id.tilCategory);
        tilLocation = findViewById(R.id.tilLocation);
        tilDate = findViewById(R.id.tilDate);
        tilDescription = findViewById(R.id.tilDescription);

        etItemName = findViewById(R.id.etItemName);
        actvCategory = findViewById(R.id.actvCategory);
        etLocation = findViewById(R.id.etLocation);
        etDate = findViewById(R.id.etDate);
        etDescription = findViewById(R.id.etDescription);

        btnSubmitLost = findViewById(R.id.btnSubmitLost);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategoryDropdown() {
        String[] categories = getResources().getStringArray(R.array.categories_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        actvCategory.setAdapter(adapter);
    }

    private void setupDatePicker() {
        // Set today's date as default
        updateDateField();

        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateField();
        };

        etDate.setOnClickListener(v -> new DatePickerDialog(ReportLostActivity.this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show());
    }

    private void updateDateField() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        etDate.setText(sdf.format(calendar.getTime()));
    }

    private void setupSubmitButton() {
        btnSubmitLost.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        String itemName = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
        String category = actvCategory.getText() != null ? actvCategory.getText().toString().trim() : "";
        String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        tilItemName.setError(null);
        tilCategory.setError(null);
        tilLocation.setError(null);
        tilDate.setError(null);

        boolean hasError = false;

        if (TextUtils.isEmpty(itemName)) {
            tilItemName.setError("Item name is required");
            tilItemName.requestFocus();
            hasError = true;
        }

        if (TextUtils.isEmpty(category)) {
            tilCategory.setError("Please select a category");
            if (!hasError) tilCategory.requestFocus();
            hasError = true;
        }

        if (TextUtils.isEmpty(location)) {
            tilLocation.setError("Location lost is required");
            if (!hasError) tilLocation.requestFocus();
            hasError = true;
        }

        if (TextUtils.isEmpty(date)) {
            tilDate.setError("Date lost is required");
            if (!hasError) tilDate.requestFocus();
            hasError = true;
        }

        if (hasError) return;

        Item lostItem = new Item();
        lostItem.setUserId(sessionManager.getUserId());
        lostItem.setItemType("Lost");
        lostItem.setItemName(itemName);
        lostItem.setCategory(category);
        lostItem.setLocation(location);
        lostItem.setDate(date);
        lostItem.setDescription(description);
        lostItem.setStatus("Active");

        long id = dbHelper.addLostItem(lostItem);
        if (id > 0) {
            Toast.makeText(this, "Lost item reported successfully!", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to submit report. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
