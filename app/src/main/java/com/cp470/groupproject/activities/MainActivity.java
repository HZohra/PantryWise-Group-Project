package com.cp470.groupproject.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.entity.StorageLocation;
import com.cp470.groupproject.ui.addfood.AddFoodActivity;
import com.cp470.groupproject.ui.inventory.FoodAdapter;
import com.cp470.groupproject.ui.inventory.InventoryViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.cp470.groupproject.ui.settings.SettingsActivity;
import com.cp470.groupproject.ui.settings.DisplayPreferences;

public class MainActivity extends AppCompatActivity {

    private InventoryViewModel viewModel;
    private FoodAdapter adapter;
    private TextView emptyInventory;
    private EditText searchFood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = getSharedPreferences(
                "PantryPreferences", MODE_PRIVATE);

        String theme = preferences.getString("theme", "system");

        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO);
                break;

            case "dark":
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES);
                break;

            default:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }

        setContentView(R.layout.activity_main);

        TextView inventoryTitle = findViewById(R.id.inventoryTitle);
        inventoryTitle.setTextSize(DisplayPreferences.getTextSize(this));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top,
                    systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView recyclerView = findViewById(R.id.foodRecyclerView);
        emptyInventory = findViewById(R.id.emptyInventory);
        searchFood = findViewById(R.id.searchFood);

        findViewById(R.id.addFoodButton).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddFoodActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.helpButton).setOnClickListener(v -> showHelpGuide());

        findViewById(R.id.helpButton).setOnClickListener(v -> showHelpGuide());

        findViewById(R.id.settingsButton).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FoodAdapter();
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        viewModel.getFoodItems().observe(this, items -> {
            adapter.setFoodItems(items);
            updateEmptyMessage();
        });

        searchFood.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
                updateEmptyMessage();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        MaterialButtonToggleGroup locationGroup =
                findViewById(R.id.locationToggleGroup);

        locationGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.fridgeButton) {
                viewModel.selectLocation(StorageLocation.FRIDGE_ID);
            } else if (checkedId == R.id.freezerButton) {
                viewModel.selectLocation(StorageLocation.FREEZER_ID);
            } else if (checkedId == R.id.pantryButton) {
                viewModel.selectLocation(StorageLocation.PANTRY_ID);
            }
        });

        if (savedInstanceState != null) {
            searchFood.setText(savedInstanceState.getString("search_query", ""));

            int selectedButton = savedInstanceState.getInt(
                    "selected_location_button", R.id.fridgeButton);

            locationGroup.check(selectedButton);
        } else {
            locationGroup.check(R.id.fridgeButton);
        }
    }

    private void showHelpGuide() {
        String guide =
                "WELCOME TO PANTRYWISE\n\n" +
                        "1. ADD FOOD\n" +
                        "Tap Add Food to enter a food name, quantity, unit, storage location, and expiration date.\n\n" +
                        "2. STORAGE LOCATIONS\n" +
                        "Use Fridge, Freezer, and Pantry to view food stored in each location.\n\n" +
                        "3. SEARCH FOOD\n" +
                        "Use the search bar to find food items in the selected location.\n\n" +
                        "4. EXPIRATION STATUS\n" +
                        "Fresh: Food is not close to expiring.\n" +
                        "Use Soon: Food expires within 3 days.\n" +
                        "Expired: The expiration date has passed.\n\n" +
                        "5. EDIT FOOD\n" +
                        "Tap a food item to edit its details.\n\n" +
                        "6. FOOD ACTIONS\n" +
                        "Use Consumed, Wasted, Expired, or Donated to record what happened to an item.";

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("PantryWise Help Guide")
                .setMessage(guide)
                .setPositiveButton("Close", null)
                .create();

        dialog.setOnShowListener(d -> {
            int size = DisplayPreferences.getTextSize(this);

            TextView message = dialog.findViewById(android.R.id.message);
            if (message != null) {
                message.setTextSize(size);
            }

            int titleId = getResources().getIdentifier(
                    "alertTitle", "id", "android");

            TextView title = dialog.findViewById(titleId);
            if (title != null) {
                title.setTextSize(size);
            }
        });

        dialog.show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("search_query", searchFood.getText().toString());

        MaterialButtonToggleGroup locationGroup =
                findViewById(R.id.locationToggleGroup);

        outState.putInt("selected_location_button",
                locationGroup.getCheckedButtonId());
    }

    private void updateEmptyMessage() {
        boolean isEmpty = adapter.getItemCount() == 0;

        emptyInventory.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        if (isEmpty) {
            if (searchFood.getText().toString().trim().isEmpty()) {
                emptyInventory.setText(R.string.inventory_empty);
            } else {
                emptyInventory.setText(R.string.inventory_no_search_results);
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        DisplayPreferences.applyTextSize(this);
    }
}