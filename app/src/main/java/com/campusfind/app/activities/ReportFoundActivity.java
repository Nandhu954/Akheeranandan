package com.campusfind.app.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.campusfind.app.R;
import com.campusfind.app.database.DatabaseHelper;
import com.campusfind.app.models.Item;
import com.campusfind.app.utils.MatchingEngine;
import com.campusfind.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportFoundActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ImageView ivPhotoPreview;
    private LinearLayout layoutPhotoPlaceholder;
    private TextView tvPhotoError;
    private MaterialButton btnTakePhoto, btnChooseGallery;
    private TextInputLayout tilItemName, tilCategory, tilLocation, tilDate, tilDescription;
    private TextInputEditText etItemName, etLocation, etDate, etDescription;
    private AutoCompleteTextView actvCategory;
    private MaterialButton btnSubmitFound;

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private final Calendar calendar = Calendar.getInstance();

    private Uri photoUri;        // URI of the photo taken/selected
    private String photoPath;    // Absolute path of the saved photo

    // -----------------------------------------------------------------------
    // Activity result launchers
    // -----------------------------------------------------------------------

    /** Launch camera, result = true if photo was taken successfully */
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && photoUri != null) {
                    showPhotoPreview(photoUri);
                }
            });

    /** Launch gallery picker */
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    photoUri = uri;
                    photoPath = uri.toString();
                    showPhotoPreview(uri);
                }
            });

    /** Ask for camera permission */
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Camera permission needed to take a photo", Toast.LENGTH_SHORT).show();
                }
            });

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

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
        setupPhotoButtons();
        setupSubmitButton();
    }

    // -----------------------------------------------------------------------
    // View setup
    // -----------------------------------------------------------------------

    private void initViews() {
        toolbar = findViewById(R.id.toolbarReportFound);
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        layoutPhotoPlaceholder = findViewById(R.id.layoutPhotoPlaceholder);
        tvPhotoError = findViewById(R.id.tvPhotoError);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnChooseGallery = findViewById(R.id.btnChooseGallery);

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
        etDate.setOnClickListener(v -> new DatePickerDialog(this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show());
    }

    private void updateDateField() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        etDate.setText(sdf.format(calendar.getTime()));
    }

    // -----------------------------------------------------------------------
    // Photo handling
    // -----------------------------------------------------------------------

    private void setupPhotoButtons() {
        btnTakePhoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnChooseGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", photoFile);
            photoPath = photoFile.getAbsolutePath();
            cameraLauncher.launch(photoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Could not open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(calendar.getTime());
        String imageFileName = "CAMPUSFIND_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void showPhotoPreview(Uri uri) {
        ivPhotoPreview.setImageURI(uri);
        ivPhotoPreview.setVisibility(View.VISIBLE);
        layoutPhotoPlaceholder.setVisibility(View.GONE);
        tvPhotoError.setVisibility(View.GONE);
    }

    // -----------------------------------------------------------------------
    // Submit
    // -----------------------------------------------------------------------

    private void setupSubmitButton() {
        btnSubmitFound.setOnClickListener(v -> submitReport());
    }

    private void submitReport() {
        // --- Reset errors ---
        tilItemName.setError(null);
        tilCategory.setError(null);
        tilLocation.setError(null);
        tilDate.setError(null);
        tvPhotoError.setVisibility(View.GONE);

        String itemName  = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";
        String category  = actvCategory.getText() != null ? actvCategory.getText().toString().trim() : "";
        String location  = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
        String date      = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String desc      = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        boolean hasError = false;

        // Validate photo (mandatory)
        if (photoPath == null || photoPath.isEmpty()) {
            tvPhotoError.setVisibility(View.VISIBLE);
            hasError = true;
        }

        if (TextUtils.isEmpty(itemName)) {
            tilItemName.setError("Item name is required");
            if (!hasError) tilItemName.requestFocus();
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

        // --- Build and save the found item ---
        Item foundItem = new Item();
        foundItem.setUserId(sessionManager.getUserId());
        foundItem.setItemType("Found");
        foundItem.setItemName(itemName);
        foundItem.setCategory(category);
        foundItem.setLocation(location);
        foundItem.setDate(date);
        foundItem.setDescription(desc);
        foundItem.setStatus("Active");
        foundItem.setPhotoPath(photoPath);

        long id = dbHelper.addFoundItem(foundItem);
        if (id <= 0) {
            Toast.makeText(this, "Failed to submit report. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Run smart matching ---
        foundItem.setItemId((int) id);
        List<Item> lostItems = dbHelper.getLostItems(null, null);
        List<Item> matches = MatchingEngine.findMatches(foundItem, lostItems);

        if (!matches.isEmpty()) {
            // Notify finder about match and offer to email owner
            showMatchDialog(foundItem, matches);
        } else {
            Toast.makeText(this, "Found item reported successfully!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // -----------------------------------------------------------------------
    // Match notification dialog
    // -----------------------------------------------------------------------

    private void showMatchDialog(Item foundItem, List<Item> matches) {
        Item bestMatch = matches.get(0); // most likely match

        String ownerEmail = bestMatch.getReporterEmail() != null ? bestMatch.getReporterEmail() : "";
        String ownerName  = bestMatch.getReporterName()  != null ? bestMatch.getReporterName()  : "the owner";

        new AlertDialog.Builder(this)
                .setTitle("🎯 Possible Match Found!")
                .setMessage("Your found item may match a lost report by " + ownerName +
                        " for \"" + bestMatch.getItemName() + "\" at " + bestMatch.getLocation() +
                        ".\n\nWould you like to send them an email notification?")
                .setPositiveButton("📧 Send Email", (dialog, which) -> {
                    sendMatchEmail(ownerEmail, ownerName, bestMatch, foundItem);
                })
                .setNegativeButton("Skip", (dialog, which) -> {
                    Toast.makeText(this, "Found item reported successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Opens Gmail (or any email app) with a pre-filled match notification email.
     * This is the standard Android approach — no backend/API key needed.
     */
    private void sendMatchEmail(String toEmail, String ownerName, Item lostItem, Item foundItem) {
        String subject = "CampusFind: Possible Match for Your Lost " + lostItem.getItemName();
        String body = "Hi " + ownerName + ",\n\n"
                + "Great news! Someone on campus has reported finding an item that may be yours.\n\n"
                + "🔍 Your Lost Item:\n"
                + "  Item: " + lostItem.getItemName() + "\n"
                + "  Category: " + lostItem.getCategory() + "\n"
                + "  Location Lost: " + lostItem.getLocation() + "\n"
                + "  Date: " + lostItem.getDate() + "\n\n"
                + "📦 Found Item Report:\n"
                + "  Item Found: " + foundItem.getItemName() + "\n"
                + "  Category: " + foundItem.getCategory() + "\n"
                + "  Location Found: " + foundItem.getLocation() + "\n"
                + "  Date Found: " + foundItem.getDate() + "\n\n"
                + "Please open the CampusFind app and check the Search section to view the\n"
                + "found item photo and contact the finder.\n\n"
                + "— CampusFind App\nFind Lost. Return Found.";

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{toEmail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send notification via..."));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found. Please notify the owner manually.", Toast.LENGTH_LONG).show();
        }

        Toast.makeText(this, "Found item reported successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
