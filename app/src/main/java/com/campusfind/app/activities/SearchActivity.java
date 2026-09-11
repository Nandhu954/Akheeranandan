package com.campusfind.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.campusfind.app.R;
import com.campusfind.app.adapters.ItemAdapter;
import com.campusfind.app.models.Item;
import com.campusfind.app.utils.FirestoreHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * SearchActivity — shows Lost and Found items fetched from Firebase Firestore.
 *
 * All users on all devices report into Firestore, so every student can see
 * every report in real-time regardless of which phone submitted it.
 *
 * Flow:
 *   fetchFromCloud()  — one Firestore read, fills cachedItems
 *   applyFilters()    — client-side text/category filter on cachedItems → shows in RecyclerView
 *
 * fetchFromCloud is called on: onCreate, onResume, tab change, swipe-to-refresh.
 * applyFilters  is called on: search query change, category change (no new Firestore read needed).
 */
public class SearchActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText etSearchQuery;
    private AutoCompleteTextView actvFilterCategory;
    private TabLayout tabLayoutItemType;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvSearchResults;
    private LinearLayout layoutEmptyState;

    private ItemAdapter adapter;

    /** All items currently shown in the list (after filtering). */
    private final List<Item> itemList = new ArrayList<>();

    /** Raw items fetched from Firestore — filtering is applied on top of this. */
    private final List<Item> cachedItems = new ArrayList<>();

    private boolean isLostTab = true;
    private String selectedCategory = "All Categories";
    private String currentQuery = "";

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupToolbar();
        setupCategoryFilter();
        setupTabs();
        setupRecyclerView();
        setupSearchInput();
        setupSwipeRefresh();

        fetchFromCloud();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh from cloud every time user comes back to this screen
        fetchFromCloud();
    }

    // ─── View init ─────────────────────────────────────────────────────────────

    private void initViews() {
        toolbar              = findViewById(R.id.toolbarSearch);
        etSearchQuery        = findViewById(R.id.etSearchQuery);
        actvFilterCategory   = findViewById(R.id.actvFilterCategory);
        tabLayoutItemType    = findViewById(R.id.tabLayoutItemType);
        swipeRefresh         = findViewById(R.id.swipeRefreshSearch);
        rvSearchResults      = findViewById(R.id.rvSearchResults);
        layoutEmptyState     = findViewById(R.id.layoutEmptyState);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategoryFilter() {
        String[] categories = getResources().getStringArray(R.array.filter_categories_array);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, categories);
        actvFilterCategory.setAdapter(catAdapter);
        actvFilterCategory.setText(categories[0], false);

        actvFilterCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = categories[position];
            // No new Firestore read needed — just re-filter the cached data
            applyFilters();
        });
    }

    private void setupTabs() {
        tabLayoutItemType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isLostTab = (tab.getPosition() == 0);
                // Different collection → need a fresh Firestore fetch
                cachedItems.clear();
                fetchFromCloud();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new ItemAdapter(this, itemList, item -> {
            Intent intent = new Intent(SearchActivity.this, ItemDetailActivity.class);
            intent.putExtra("ITEM_EXTRA", item);
            startActivity(intent);
        });
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(adapter);
    }

    private void setupSearchInput() {
        etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                // Filter already-cached data — no new network call
                applyFilters();
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::fetchFromCloud);
    }

    // ─── Data loading ──────────────────────────────────────────────────────────

    /**
     * Fetch all items for the current tab from Firebase Firestore.
     * On success the raw list is cached and then filtered + displayed.
     */
    private void fetchFromCloud() {
        swipeRefresh.setRefreshing(true);

        FirestoreHelper.ItemsCallback callback = new FirestoreHelper.ItemsCallback() {
            @Override
            public void onSuccess(List<Item> items) {
                cachedItems.clear();
                cachedItems.addAll(items);
                applyFilters();
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SearchActivity.this,
                        "Could not load items. Check your internet connection.",
                        Toast.LENGTH_SHORT).show();
                swipeRefresh.setRefreshing(false);
                // Show whatever we have cached (may be empty)
                applyFilters();
            }
        };

        if (isLostTab) {
            FirestoreHelper.getInstance().getLostItems(callback);
        } else {
            FirestoreHelper.getInstance().getFoundItems(callback);
        }
    }

    /**
     * Filter cachedItems by current search query and category.
     * No network call — runs instantly on cached data.
     */
    private void applyFilters() {
        List<Item> filtered = new ArrayList<>();
        for (Item item : cachedItems) {
            if (passesFilters(item)) {
                filtered.add(item);
            }
        }

        itemList.clear();
        itemList.addAll(filtered);
        adapter.updateList(itemList);
        updateEmptyState();
    }

    /** Returns true if the item matches the current search query AND category. */
    private boolean passesFilters(Item item) {
        // Category filter
        if (!selectedCategory.equals("All Categories")) {
            if (item.getCategory() == null || !item.getCategory().equals(selectedCategory)) {
                return false;
            }
        }

        // Text search across name, description, location
        if (!currentQuery.trim().isEmpty()) {
            String q = currentQuery.trim().toLowerCase();
            String name = item.getItemName()   != null ? item.getItemName().toLowerCase()   : "";
            String desc = item.getDescription() != null ? item.getDescription().toLowerCase() : "";
            String loc  = item.getLocation()    != null ? item.getLocation().toLowerCase()    : "";
            if (!name.contains(q) && !desc.contains(q) && !loc.contains(q)) {
                return false;
            }
        }

        return true;
    }

    private void updateEmptyState() {
        if (itemList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.VISIBLE);
        }
    }
}
