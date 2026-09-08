package com.campusfind.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.campusfind.app.R;
import com.campusfind.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcomeUser, tvUserRollNo;
    private MaterialButton btnQuickLogout;
    private MaterialCardView cardReportLost, cardReportFound, cardSearchItems,
            cardMyReports, cardScanQR, cardLogout;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        initViews();
        setupUserData();
        setupClickListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
        }
    }

    private void initViews() {
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        tvUserRollNo = findViewById(R.id.tvUserRollNo);
        btnQuickLogout = findViewById(R.id.btnQuickLogout);

        cardReportLost = findViewById(R.id.cardReportLost);
        cardReportFound = findViewById(R.id.cardReportFound);
        cardSearchItems = findViewById(R.id.cardSearchItems);
        cardMyReports = findViewById(R.id.cardMyReports);
        cardScanQR = findViewById(R.id.cardScanQR);
        cardLogout = findViewById(R.id.cardLogout);
    }

    private void setupUserData() {
        String name = sessionManager.getUserName();
        String rollNo = sessionManager.getUserRollNo();

        tvWelcomeUser.setText("Hello, " + name + "!");
        if (rollNo != null && !rollNo.isEmpty()) {
            tvUserRollNo.setText("Roll No: " + rollNo);
        } else {
            tvUserRollNo.setText("Campus Member");
        }
    }

    private void setupClickListeners() {
        cardReportLost.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ReportLostActivity.class)));

        cardReportFound.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ReportFoundActivity.class)));

        cardSearchItems.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SearchActivity.class)));

        cardMyReports.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MyReportsActivity.class)));

        cardScanQR.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ScanQRActivity.class)));

        cardLogout.setOnClickListener(v -> showLogoutConfirmation());
        btnQuickLogout.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to sign out of CampusFind?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    sessionManager.logout();
                    Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    redirectToLogin();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
