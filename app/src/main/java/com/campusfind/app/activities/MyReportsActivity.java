package com.campusfind.app.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.campusfind.app.R;
import com.campusfind.app.adapters.ItemAdapter;
import com.campusfind.app.database.DatabaseHelper;
import com.campusfind.app.models.Claim;
import com.campusfind.app.models.Item;
import com.campusfind.app.utils.QRCodeHelper;
import com.campusfind.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class MyReportsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvMyReports;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyTitle, tvEmptySubtitle;

    private ItemAdapter adapter;
    private final List<Item> displayedItems = new ArrayList<>();
    private final List<Claim> userClaims = new ArrayList<>();

    private DatabaseHelper dbHelper;
    private SessionManager sessionManager;
    private int currentTabPosition = 0; // 0: Lost, 1: Found, 2: Claims

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reports);

        dbHelper = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupSwipeRefresh();

        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarMyReports);
        tabLayout = findViewById(R.id.tabLayoutMyReports);
        swipeRefresh = findViewById(R.id.swipeRefreshMyReports);
        rvMyReports = findViewById(R.id.rvMyReports);
        layoutEmpty = findViewById(R.id.layoutEmptyMyReports);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                loadData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new ItemAdapter(this, displayedItems, item -> {
            if (currentTabPosition == 2) {
                // In claims tab, click directly displays the claim QR Code
                showQRCodeDialog(item.getDate(), item.getItemName());
            } else {
                // In lost/found tab, open details
                Intent intent = new Intent(MyReportsActivity.this, ItemDetailActivity.class);
                intent.putExtra("ITEM_EXTRA", item);
                startActivity(intent);
            }
        });
        rvMyReports.setLayoutManager(new LinearLayoutManager(this));
        rvMyReports.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void loadData() {
        int userId = sessionManager.getUserId();
        displayedItems.clear();

        if (currentTabPosition == 0) {
            List<Item> lostItems = dbHelper.getLostItemsByUser(userId);
            if (lostItems != null) displayedItems.addAll(lostItems);
            tvEmptyTitle.setText("No Lost Items Reported");
            tvEmptySubtitle.setText("You haven't reported any lost items yet.");
        } else if (currentTabPosition == 1) {
            List<Item> foundItems = dbHelper.getFoundItemsByUser(userId);
            if (foundItems != null) displayedItems.addAll(foundItems);
            tvEmptyTitle.setText("No Found Items Reported");
            tvEmptySubtitle.setText("You haven't reported any found items yet.");
        } else {
            userClaims.clear();
            List<Claim> claims = dbHelper.getClaimsByUser(userId);
            if (claims != null) {
                userClaims.addAll(claims);
                for (Claim c : claims) {
                    Item claimDisplayItem = new Item();
                    claimDisplayItem.setItemId(c.getItemId());
                    claimDisplayItem.setItemName(c.getItemName() != null ? c.getItemName() : "Claim #" + c.getClaimId());
                    claimDisplayItem.setCategory("Claim #" + c.getClaimId() + " (" + c.getItemType() + ")");
                    claimDisplayItem.setLocation("Tap to show verification QR");
                    claimDisplayItem.setDate(c.getQrPayload()); // Storing qr payload in date field for quick access
                    claimDisplayItem.setStatus(c.getClaimStatus());
                    claimDisplayItem.setItemType(c.getItemType());
                    displayedItems.add(claimDisplayItem);
                }
            }
            tvEmptyTitle.setText("No Claims Submitted");
            tvEmptySubtitle.setText("You have not claimed any campus items yet.");
        }

        adapter.updateList(displayedItems);

        if (displayedItems.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvMyReports.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvMyReports.setVisibility(View.VISIBLE);
        }

        swipeRefresh.setRefreshing(false);
    }

    private void showQRCodeDialog(String qrPayload, String itemName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_qr_code, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvDialogSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);
        ImageView ivQRCode = dialogView.findViewById(R.id.ivQRCode);
        TextView tvClaimCode = dialogView.findViewById(R.id.tvClaimCode);
        MaterialButton btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);

        tvDialogTitle.setText("Claim QR: " + itemName);
        tvDialogSubtitle.setText("Present this QR code at the campus handover desk to verify and complete item return.");
        tvClaimCode.setText(qrPayload);

        Bitmap qrBitmap = QRCodeHelper.generateQRCode(qrPayload, 500);
        if (qrBitmap != null) {
            ivQRCode.setImageBitmap(qrBitmap);
        } else {
            Toast.makeText(this, "Failed to render QR Code", Toast.LENGTH_SHORT).show();
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
