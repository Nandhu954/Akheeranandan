package com.campusfind.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusfind.app.R;
import com.campusfind.app.database.DatabaseHelper;
import com.campusfind.app.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilRollNo, tilRegEmail, tilRegPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etRollNo, etRegEmail, etRegPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvLoginLink;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = DatabaseHelper.getInstance(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilFullName = findViewById(R.id.tilFullName);
        tilRollNo = findViewById(R.id.tilRollNo);
        tilRegEmail = findViewById(R.id.tilRegEmail);
        tilRegPassword = findViewById(R.id.tilRegPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etFullName = findViewById(R.id.etFullName);
        etRollNo = findViewById(R.id.etRollNo);
        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegistration());
        tvLoginLink.setOnClickListener(v -> finish());
    }

    private void attemptRegistration() {
        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String rollNo = etRollNo.getText() != null ? etRollNo.getText().toString().trim() : "";
        String email = etRegEmail.getText() != null ? etRegEmail.getText().toString().trim() : "";
        String password = etRegPassword.getText() != null ? etRegPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        // Reset errors
        tilFullName.setError(null);
        tilRollNo.setError(null);
        tilRegEmail.setError(null);
        tilRegPassword.setError(null);
        tilConfirmPassword.setError(null);

        boolean cancel = false;

        // Confirm Password validation
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm your password");
            tilConfirmPassword.requestFocus();
            cancel = true;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError("Passwords do not match");
            tilConfirmPassword.requestFocus();
            cancel = true;
        }

        // Password validation
        if (TextUtils.isEmpty(password)) {
            tilRegPassword.setError("Password is required");
            tilRegPassword.requestFocus();
            cancel = true;
        } else if (password.length() < 6) {
            tilRegPassword.setError("Password must be at least 6 characters");
            tilRegPassword.requestFocus();
            cancel = true;
        }

        // Email validation
        if (TextUtils.isEmpty(email)) {
            tilRegEmail.setError("Email is required");
            tilRegEmail.requestFocus();
            cancel = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilRegEmail.setError("Enter a valid email address");
            tilRegEmail.requestFocus();
            cancel = true;
        } else if (dbHelper.isEmailTaken(email)) {
            tilRegEmail.setError("This email is already registered");
            tilRegEmail.requestFocus();
            cancel = true;
        }

        // Roll Number validation
        if (TextUtils.isEmpty(rollNo)) {
            tilRollNo.setError("Roll number is required");
            tilRollNo.requestFocus();
            cancel = true;
        } else if (dbHelper.isRollNoTaken(rollNo)) {
            tilRollNo.setError("This roll number is already registered");
            tilRollNo.requestFocus();
            cancel = true;
        }

        // Name validation
        if (TextUtils.isEmpty(name)) {
            tilFullName.setError("Full name is required");
            tilFullName.requestFocus();
            cancel = true;
        }

        if (cancel) return;

        User newUser = new User(name, rollNo, email, password);
        long result = dbHelper.registerUser(newUser);

        if (result > 0) {
            Toast.makeText(this, "Registration successful! Please sign in.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
