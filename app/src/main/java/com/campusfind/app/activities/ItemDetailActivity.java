package com.campusfind.app.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.campusfind.app.R;
import com.campusfind.app.database.DatabaseHelper;
import com.campusfind.app.models.Claim;
import com.campusfind.app.models.Item;
import com.campusfind.app.utils.QRCodeHelper;
import com.campusfind.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class ItemDetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvDetailTypeBadge, tvDetailStatusBadge, tvDetailItemName, tvDetailCategory;
    private TextView tvDetailLocationLabel, tvDetailLocation, tvDetailDateLabel, tvDetailDate, tvDetailDescription;
    private TextView tvReporterName, tvReporterDetails, tvOwnershipNotice;
    private MaterialButton btnClaimItem, btnViewClaimQR;
    private com.google.android.material.card.MaterialCardView cardItemPhoto;
    private ImageView ivItemPhoto;

    private Item currentItem;
    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private Claim userExistingClaim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        currentItem = (Item) getIntent().getSerializableExtra("ITEM_EXTRA");
        if (currentItem == null) {
            Toast.makeText(this, "Item details not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();
        populateItemData();
        checkClaimStatusAndPermissions();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarDetail);
        tvDetailTypeBadge = findViewById(R.id.tvDetailTypeBadge);
        tvDetailStatusBadge = findViewById(R.id.tvDetailStatusBadge);
        tvDetailItemName = findViewById(R.id.tvDetailItemName);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);

        tvDetailLocationLabel = findViewById(R.id.tvDetailLocationLabel);
        tvDetailLocation = findViewById(R.id.tvDetailLocation);
        tvDetailDateLabel = findViewById(R.id.tvDetailDateLabel);
        tvDetailDate = findViewById(R.id.tvDetailDate);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);

        tvReporterName = findViewById(R.id.tvReporterName);
        tvReporterDetails = findViewById(R.id.tvReporterDetails);
        tvOwnershipNotice = findViewById(R.id.tvOwnershipNotice);

        btnClaimItem = findViewById(R.id.btnClaimItem);
        btnViewClaimQR = findViewById(R.id.btnViewClaimQR);
        cardItemPhoto = findViewById(R.id.cardItemPhoto);
        ivItemPhoto = findViewById(R.id.ivItemPhoto);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void populateItemData() {
        tvDetailItemName.setText(currentItem.getItemName());
        tvDetailCategory.setText("Category: " + currentItem.getCategory());
        tvDetailLocation.setText(currentItem.getLocation());
        tvDetailDate.setText(currentItem.getDate());

        String desc = currentItem.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            tvDetailDescription.setText(desc.trim());
        } else {
            tvDetailDescription.setText("No description provided.");
        }

        // Item Type Badge
        String itemType = currentItem.getItemType() != null ? currentItem.getItemType().toUpperCase() : "ITEM";
        tvDetailTypeBadge.setText(itemType + " ITEM");
        if ("FOUND".equalsIgnoreCase(itemType)) {
            tvDetailLocationLabel.setText("Location Found");
            tvDetailDateLabel.setText("Date Found");
            tvDetailTypeBadge.setTextColor(ContextCompat.getColor(this, R.color.primary));
            tvDetailTypeBadge.setBackgroundResource(R.drawable.badge_matched);

            // Show photo if available
            String photoPath = currentItem.getPhotoPath();
            if (photoPath != null && !photoPath.isEmpty()) {
                cardItemPhoto.setVisibility(View.VISIBLE);
                try {
                    android.net.Uri photoUri = android.net.Uri.parse(photoPath);
                    ivItemPhoto.setImageURI(photoUri);
                } catch (Exception e) {
                    cardItemPhoto.setVisibility(View.GONE);
                }
            }
        } else {
            tvDetailLocationLabel.setText("Location Lost");
            tvDetailDateLabel.setText("Date Lost");
            tvDetailTypeBadge.setTextColor(ContextCompat.getColor(this, R.color.accent));
            tvDetailTypeBadge.setBackgroundResource(R.drawable.badge_active);
        }

        // Status Badge
        String status = currentItem.getStatus() != null ? currentItem.getStatus() : "Active";
        tvDetailStatusBadge.setText(status);
        if ("returned".equalsIgnoreCase(status)) {
            tvDetailStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_returned));
            tvDetailStatusBadge.setBackgroundResource(R.drawable.badge_returned);
        } else if ("matched".equalsIgnoreCase(status)) {
            tvDetailStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_matched));
            tvDetailStatusBadge.setBackgroundResource(R.drawable.badge_matched);
        } else {
            tvDetailStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.status_active));
            tvDetailStatusBadge.setBackgroundResource(R.drawable.badge_active);
        }

        // Reporter Information
        String repName = currentItem.getReporterName();
        String repRoll = currentItem.getReporterRollNo();
        String repEmail = currentItem.getReporterEmail();

        if (repName != null && !repName.isEmpty()) {
            tvReporterName.setText(repName);
            StringBuilder sb = new StringBuilder();
            if (repRoll != null && !repRoll.isEmpty()) sb.append("Roll No: ").append(repRoll).append(" • ");
            if (repEmail != null && !repEmail.isEmpty()) sb.append(repEmail);
            tvReporterDetails.setText(sb.toString());
        } else {
            tvReporterName.setText("Campus Member (ID #" + currentItem.getUserId() + ")");
            tvReporterDetails.setText("Verified Campus Student/Staff");
        }
    }

    private void checkClaimStatusAndPermissions() {
        int currentUserId = sessionManager.getUserId();
        boolean isOwner = (currentUserId == currentItem.getUserId());
        String status = currentItem.getStatus() != null ? currentItem.getStatus() : "Active";

        if (isOwner) {
            btnClaimItem.setVisibility(View.GONE);
            btnViewClaimQR.setVisibility(View.GONE);
            tvOwnershipNotice.setVisibility(View.VISIBLE);
            tvOwnershipNotice.setText("You reported this item.");
            return;
        }

        if ("returned".equalsIgnoreCase(status)) {
            btnClaimItem.setVisibility(View.GONE);
            btnViewClaimQR.setVisibility(View.GONE);
            tvOwnershipNotice.setVisibility(View.VISIBLE);
            tvOwnershipNotice.setText("This item has already been returned to its owner.");
            return;
        }

        // Check if current user already submitted a claim
        userExistingClaim = dbHelper.getClaimForUserAndItem(currentUserId, currentItem.getItemId(), currentItem.getItemType());
        if (userExistingClaim != null) {
            btnClaimItem.setVisibility(View.GONE);
            btnViewClaimQR.setVisibility(View.VISIBLE);
            tvOwnershipNotice.setVisibility(View.VISIBLE);
            tvOwnershipNotice.setText("You have an active claim for this item.");

            btnViewClaimQR.setOnClickListener(v -> showQRCodeDialog(userExistingClaim.getQrPayload()));
        } else {
            btnClaimItem.setVisibility(View.VISIBLE);
            btnViewClaimQR.setVisibility(View.GONE);
            tvOwnershipNotice.setVisibility(View.GONE);

            btnClaimItem.setOnClickListener(v -> confirmAndCreateClaim());
        }
    }

    private void confirmAndCreateClaim() {
        new AlertDialog.Builder(this)
                .setTitle("Claim Item")
                .setMessage("Are you sure you want to claim this item? A unique QR code will be generated for you to present at the handover desk for verification.")
                .setPositiveButton("Claim & Generate QR", (dialog, which) -> generateAndSaveClaim())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void generateAndSaveClaim() {
        int currentUserId = sessionManager.getUserId();
        long timestamp = System.currentTimeMillis();
        String qrPayload = "CAMPUSFIND_CLAIM_" + currentItem.getItemId() + "_" + timestamp;

        Claim claim = new Claim();
        claim.setUserId(currentUserId);
        claim.setItemId(currentItem.getItemId());
        claim.setItemType(currentItem.getItemType());
        claim.setQrPayload(qrPayload);
        claim.setClaimStatus("Pending");

        long claimId = dbHelper.addClaim(claim);
        if (claimId > 0) {
            // Update status to Matched if it was Active
            dbHelper.updateItemStatus(currentItem.getItemId(), currentItem.getItemType(), "Matched");
            currentItem.setStatus("Matched");
            populateItemData();

            Toast.makeText(this, "Claim submitted successfully!", Toast.LENGTH_SHORT).show();
            userExistingClaim = claim;
            checkClaimStatusAndPermissions();
            showQRCodeDialog(qrPayload);
        } else {
            Toast.makeText(this, "Failed to submit claim. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showQRCodeDialog(String qrPayload) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_qr_code, null);
        ImageView ivQRCode = dialogView.findViewById(R.id.ivQRCode);
        TextView tvClaimCode = dialogView.findViewById(R.id.tvClaimCode);
        MaterialButton btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);

        tvClaimCode.setText(qrPayload);

        Bitmap qrBitmap = QRCodeHelper.generateQRCode(qrPayload, 500);
        if (qrBitmap != null) {
            ivQRCode.setImageBitmap(qrBitmap);
        } else {
            Toast.makeText(this, "Error generating QR Code Bitmap", Toast.LENGTH_SHORT).show();
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
