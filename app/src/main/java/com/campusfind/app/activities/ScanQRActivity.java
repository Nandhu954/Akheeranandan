package com.campusfind.app.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.campusfind.app.R;
import com.campusfind.app.database.DatabaseHelper;
import com.campusfind.app.models.Claim;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

public class ScanQRActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    private MaterialToolbar toolbar;
    private DecoratedBarcodeView barcodeScannerView;
    private FloatingActionButton fabTorch;
    private TextInputEditText etManualPayload;
    private MaterialButton btnVerifyManual;

    private DatabaseHelper dbHelper;
    private boolean isTorchOn = false;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        dbHelper = DatabaseHelper.getInstance(this);

        initViews();
        setupToolbar();
        setupManualInput();
        checkCameraPermissionAndStart();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarScanQR);
        barcodeScannerView = findViewById(R.id.barcodeScannerView);
        fabTorch = findViewById(R.id.fabTorch);
        etManualPayload = findViewById(R.id.etManualPayload);
        btnVerifyManual = findViewById(R.id.btnVerifyManual);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupManualInput() {
        btnVerifyManual.setOnClickListener(v -> {
            String code = etManualPayload.getText() != null ? etManualPayload.getText().toString().trim() : "";
            if (TextUtils.isEmpty(code)) {
                Toast.makeText(ScanQRActivity.this, "Please enter a claim code", Toast.LENGTH_SHORT).show();
                return;
            }
            processScannedPayload(code);
        });

        fabTorch.setOnClickListener(v -> toggleTorch());
    }

    private void toggleTorch() {
        if (isTorchOn) {
            barcodeScannerView.setTorchOff();
            isTorchOn = false;
        } else {
            barcodeScannerView.setTorchOn();
            isTorchOn = true;
        }
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    private void startScanning() {
        barcodeScannerView.setStatusText("Scanning for CampusFind Claim QR...");
        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null && !isProcessing) {
                    runOnUiThread(() -> processScannedPayload(result.getText()));
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });
    }

    private void processScannedPayload(String payload) {
        if (isProcessing) return;
        isProcessing = true;

        Claim claim = dbHelper.getClaimByQrPayload(payload);

        if (claim == null) {
            showResultDialog("Invalid QR Code",
                    "No matching claim was found for payload:\n\n" + payload,
                    false);
            return;
        }

        if ("Returned".equalsIgnoreCase(claim.getClaimStatus())) {
            showResultDialog("Already Returned",
                    "This item has already been verified and returned to "
                            + (claim.getClaimerName() != null ? claim.getClaimerName() : "the claimant")
                            + ".\n\nItem: " + (claim.getItemName() != null ? claim.getItemName() : "N/A"),
                    false);
            return;
        }

        // Perform atomic update in Database
        boolean success = dbHelper.verifyAndCompleteHandover(payload);
        if (success) {
            String itemName = claim.getItemName() != null ? claim.getItemName() : "Campus Item";
            String studentName = claim.getClaimerName() != null ? claim.getClaimerName() : "Student";
            String studentRoll = claim.getClaimerRollNo() != null ? claim.getClaimerRollNo() : "--";
            String studentEmail = claim.getClaimerEmail() != null ? claim.getClaimerEmail() : "--";

            String message = "Item Handover Completed!\n\n"
                    + "• Item: " + itemName + " (" + claim.getItemType() + ")\n"
                    + "• Claimed by: " + studentName + "\n"
                    + "• Roll No: " + studentRoll + "\n"
                    + "• Email: " + studentEmail + "\n\n"
                    + "Status for both Claim and Item has been updated to 'Returned'.";

            showResultDialog("Handover Verified Successfully!", message, true);
        } else {
            showResultDialog("Handover Failed",
                    "Could not complete the handover transaction. Please check database logs.",
                    false);
        }
    }

    private void showResultDialog(String title, String message, boolean isSuccess) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(isSuccess ? "Finish" : "Scan Again", (dialog, which) -> {
                    dialog.dismiss();
                    if (isSuccess) {
                        finish();
                    } else {
                        isProcessing = false;
                        if (etManualPayload != null) etManualPayload.setText("");
                    }
                });

        if (isSuccess) {
            builder.setIcon(android.R.drawable.ic_dialog_info);
        } else {
            builder.setIcon(android.R.drawable.ic_dialog_alert);
        }

        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission denied. You can still use manual verification.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeScannerView != null) {
            barcodeScannerView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeScannerView != null) {
            barcodeScannerView.pause();
        }
    }
}
