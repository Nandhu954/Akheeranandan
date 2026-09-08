package com.campusfind.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.campusfind.app.R;
import com.campusfind.app.adapters.ItemAdapter;
import com.campusfind.app.database.DatabaseHelper;
import com.campusfind.app.models.Item;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText etSearchQuery;
    private AutoCompleteTextView actvFilterCategory;
    private TabLayout tabLayoutItemType;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvSearchResults;
    private LinearLayout layoutEmptyState;

    private ItemAdapter adapter;
    private final List<Item> itemList = new ArrayList<>();
    private DatabaseHelper dbHelper;

    private boolean isLostTab = true; // true: Lost Items, false: Found Items
    private String selectedCategory = "All Categories";
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        dbHelper = DatabaseHelper.getInstance(this);

        initViews();
        setupToolbar();
        setupCategoryFilter();
        setupTabs();
        setupRecyclerView();
        setupSearchInput();
        setupSwipeRefresh();

        loadItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarSearch);
        etSearchQuery = findViewById(R.id.etSearchQuery);
        actvFilterCategory = findViewById(R.id.actvFilterCategory);
        tabLayoutItemType = findViewById(R.id.tabLayoutItemType);
        swipeRefresh = findViewById(R.id.swipeRefreshSearch);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupCategoryFilter() {
        String[] categories = getResources().getStringArray(R.array.filter_categories_array);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        actvFilterCategory.setAdapter(catAdapter);
        actvFilterCategory.setText(categories[0], false);

        actvFilterCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory = categories[position];
            loadItems();
        });
    }

    private void setupTabs() {
        tabLayoutItemType.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isLostTab = (tab.getPosition() == 0);
                loadItems();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
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
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                loadItems();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::loadItems);
    }

    private void loadItems() {
        List<Item> results;
        if (isLostTab) {
            results = dbHelper.getLostItems(currentQuery, selectedCategory);
        } else {
            results = dbHelper.getFoundItems(currentQuery, selectedCategory);
        }

        itemList.clear();
        if (results != null) {
            itemList.addAll(results);
        }
        adapter.updateList(itemList);

        if (itemList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.VISIBLE);
        }

        swipeRefresh.setRefreshing(false);
    }
}
