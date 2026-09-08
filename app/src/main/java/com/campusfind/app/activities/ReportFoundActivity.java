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

public class ReportFoundActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputLayout tilItemName, tilCategory, tilLocation, tilDate, tilDescription;
    private TextInputEditText etItemName, etLocation, etDate, etDescription;
    private AutoCompleteTextView actvCategory;
    private MaterialButton btnSubmitFound;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_found);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupCategoryDropdown();
        setupDatePicker();
        setupSubmitButton();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarReportFound);
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

        btnSubmitFound = findViewById(R.id.btnSubmitFound);
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
        updateDateField();

        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateField();
        };

        etDate.setOnClickListener(v -> new DatePickerDialog(ReportFoundActivity.this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show());
    }

    private void updateDateField() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        etDate.setText(sdf.format(calendar.getTime()));
    }

    private void setupSubmitButton() {
        btnSubmitFound.setOnClickListener(v -> submitReport());
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
            tilLocation.setError("Location found is required");
            if (!hasError) tilLocation.requestFocus();
            hasError = true;
        }

        if (TextUtils.isEmpty(date)) {
            tilDate.setError("Date found is required");
            if (!hasError) tilDate.requestFocus();
            hasError = true;
        }

        if (hasError) return;

        Item foundItem = new Item();
        foundItem.setUserId(sessionManager.getUserId());
        foundItem.setItemType("Found");
        foundItem.setItemName(itemName);
        foundItem.setCategory(category);
        foundItem.setLocation(location);
        foundItem.setDate(date);
        foundItem.setDescription(description);
        foundItem.setStatus("Active");

        long id = dbHelper.addFoundItem(foundItem);
        if (id > 0) {
            Toast.makeText(this, "Found item reported successfully!", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to submit report. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
