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

import com.cp470.groupproject.ui.ai.AiAssistantActivity;
import com.cp470.groupproject.ui.ai.RecipesActivity;
import com.cp470.groupproject.ui.ai.FavoritesActivity;
import com.cp470.groupproject.notification.NotificationPermissionHelper;
import com.cp470.groupproject.ui.mealplanner.MealPlannerActivity;
import com.cp470.groupproject.ui.shoppinglist.ShoppingListActivity;
import com.cp470.groupproject.ui.wastesummary.WasteSummaryActivity;

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

        findViewById(R.id.aiAssistantButton).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AiAssistantActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.recipesButton).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RecipesActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.favoritesButton).setOnClickListener(v ->
                startActivity(FavoritesActivity.newIntent(this)));

        findViewById(R.id.shoppingListButton).setOnClickListener(v ->
                startActivity(ShoppingListActivity.newIntent(this)));

        findViewById(R.id.mealPlannerButton).setOnClickListener(v ->
                startActivity(MealPlannerActivity.newIntent(this)));

        findViewById(R.id.wasteSummaryButton).setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, WasteSummaryActivity.class)));

        // the launcher must be registered while the activity is being created, so the helper is
        // built here; the expiration reminders need the permission on android 13 and later
        new NotificationPermissionHelper(this).ensurePermission();

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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.help_title)
                .setMessage(R.string.help_guide)
                .setPositiveButton(R.string.help_close, null)
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