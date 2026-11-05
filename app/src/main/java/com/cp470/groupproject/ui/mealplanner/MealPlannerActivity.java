package com.cp470.groupproject.ui.mealplanner;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.shopping.ShoppingListGenerator;
import com.cp470.groupproject.ui.shoppinglist.ShoppingListActivity;
import com.cp470.groupproject.util.Event;
import com.cp470.groupproject.util.WindowInsetsHelper;
import com.google.android.material.snackbar.Snackbar;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Weekly meal planner: seven day cards with four meal slots each. it only renders state and
 * forwards clicks to {@link MealPlannerViewModel}.
 */
public class MealPlannerActivity extends AppCompatActivity {

    private static final String STATE_WEEK_START = "state_week_start";

    private MealPlannerViewModel viewModel;
    private MealPlannerAdapter adapter;
    private TextView txtWeekRange;

    /** latest saved recipes, offered in the meal dialog */
    private List<FavoriteRecipe> favorites;

    /** latest names of the food in the inventory, suggested while typing an ingredient */
    private List<String> inventoryNames;

    /** @return an explicit intent that opens this screen */
    public static Intent newIntent(Context context) {
        return new Intent(context, MealPlannerActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_meal_planner);
        WindowInsetsHelper.applySystemBarPadding(findViewById(R.id.root));

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(MealPlannerViewModel.class);
        // the view model survives a rotation, the bundle also survives a process kill
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_WEEK_START)) {
            viewModel.restoreWeek(savedInstanceState.getLong(STATE_WEEK_START));
        }

        txtWeekRange = findViewById(R.id.txtWeekRange);
        RecyclerView recycler = findViewById(R.id.recycler);
        adapter = new MealPlannerAdapter(this::onSlotClicked, viewModel.getZone());
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        findViewById(R.id.btnPrevWeek).setOnClickListener(v -> viewModel.previousWeek());
        findViewById(R.id.btnNextWeek).setOnClickListener(v -> viewModel.nextWeek());
        findViewById(R.id.btnToday).setOnClickListener(v -> viewModel.goToToday());

        // a LiveData from room only has a value while somebody observes it, so keep the latest list
        viewModel.getFavorites().observe(this, list -> favorites = list);
        viewModel.getInventoryNames().observe(this, list -> inventoryNames = list);
        viewModel.getDays().observe(this, adapter::submitList);
        viewModel.getWeekStart().observe(this, this::showWeekRange);
        viewModel.getMessages().observe(this, this::showMessage);
        viewModel.getGenerated().observe(this, this::showGenerated);
        Button btnGenerate = findViewById(R.id.btnGenerate);
        btnGenerate.setOnClickListener(v -> viewModel.generateForDisplayedWeek());
        viewModel.getGenerating().observe(this,
                busy -> btnGenerate.setEnabled(!Boolean.TRUE.equals(busy)));
    }

    /**
     * result of "generate shopping list": a snackbar that offers to open the list if it got items.
     * with no meals to look at the message says so, instead of claiming everything is in stock.
     */
    private void showGenerated(Event<ShoppingListGenerator.Result> event) {
        ShoppingListGenerator.Result result = event == null ? null : event.getContentIfNotHandled();
        if (result == null) return;
        if (result.meals == 0) {
            Snackbar.make(findViewById(R.id.root), R.string.shopping_generated_no_meals,
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        if (result.added == 0) {
            Snackbar.make(findViewById(R.id.root), R.string.shopping_generated_none,
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        Snackbar.make(findViewById(R.id.root),
                        getResources().getQuantityString(R.plurals.shopping_generated,
                                result.added, result.added),
                        Snackbar.LENGTH_LONG)
                .setAction(R.string.planner_action_view_list,
                        v -> startActivity(ShoppingListActivity.newIntent(this)))
                .show();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Long weekStart = viewModel.getWeekStart().getValue();
        if (weekStart != null) {
            outState.putLong(STATE_WEEK_START, weekStart);
        }
    }

    private void showWeekRange(Long weekStartMs) {
        if (weekStartMs == null) return;
        DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault());
        ZonedDateTime first = Instant.ofEpochMilli(weekStartMs).atZone(viewModel.getZone());
        txtWeekRange.setText(getString(R.string.planner_week_range,
                format.format(first), format.format(first.plusDays(6))));
    }

    private void showMessage(Event<Integer> event) {
        Integer resId = event == null ? null : event.getContentIfNotHandled();
        if (resId != null) {
            Snackbar.make(findViewById(R.id.root), resId, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void onSlotClicked(long dayStartMs, String mealType, MealPlan existing) {
        if (existing == null) {
            showMealDialog(dayStartMs, mealType, null);
        } else {
            showSlotOptions(dayStartMs, mealType, existing);
        }
    }

    /** options of an occupied slot: change, mark as cooked (or not) and remove */
    private void showSlotOptions(long dayStartMs, String mealType, MealPlan meal) {
        String[] options = {
                getString(R.string.planner_action_change),
                getString(meal.completed ? R.string.planner_action_mark_not_cooked
                        : R.string.planner_action_mark_cooked),
                getString(R.string.planner_action_remove)
        };
        new AlertDialog.Builder(this)
                .setTitle(meal.recipeTitle)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showMealDialog(dayStartMs, mealType, meal);
                    } else if (which == 1) {
                        viewModel.toggleCooked(meal);
                    } else {
                        viewModel.clearMeal(meal);
                    }
                })
                .show();
    }

    /** custom dialog to plan a meal from a saved recipe or by typing it */
    private void showMealDialog(long dayStartMs, String mealType, MealPlan existing) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_add_meal, null);
        Spinner spinner = content.findViewById(R.id.spnRecipe);
        EditText title = content.findViewById(R.id.edtMealTitle);
        IngredientEditor editor = new IngredientEditor(this, content,
                inventoryNames == null ? new ArrayList<String>() : inventoryNames);

        // first entry is the custom recipe, then the saved ones (the list may still be null)
        final List<FavoriteRecipe> saved = favorites == null
                ? new ArrayList<>() : new ArrayList<>(favorites);
        List<String> names = new ArrayList<>();
        names.add(getString(R.string.planner_custom_recipe));
        for (FavoriteRecipe recipe : saved) {
            names.add(recipe.title);
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        if (existing != null) {
            title.setText(existing.recipeTitle);
            editor.setIngredients(existing.ingredients);
        } else {
            editor.setIngredients(new ArrayList<>());
        }
        // android reports the initial selection too: ignore it so it does not wipe the fields
        final boolean[] firstCall = {true};
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstCall[0]) {
                    firstCall[0] = false;
                    return;
                }
                if (position == 0) {
                    title.setText("");
                    editor.setIngredients(new ArrayList<>());
                } else {
                    FavoriteRecipe recipe = saved.get(position - 1);
                    title.setText(recipe.title);
                    editor.setIngredients(recipe.ingredients);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nothing to do
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? R.string.planner_dialog_add_title
                        : R.string.planner_dialog_change_title)
                .setView(content)
                .setPositiveButton(R.string.shopping_action_save, null)
                .setNegativeButton(R.string.shopping_action_cancel, null)
                .create();
        // wired after show() so an invalid ingredient or an empty title does not close the dialog
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    // what is typed but not added yet counts as one more ingredient
                    if (!editor.addTyped()) {
                        return;
                    }
                    int position = spinner.getSelectedItemPosition();
                    String recipeId = position > 0 ? saved.get(position - 1).recipeId : null;
                    boolean ok = viewModel.saveMeal(dayStartMs, mealType, recipeId,
                            title.getText().toString(), editor.getIngredients());
                    if (ok) {
                        dialog.dismiss();
                    } else {
                        title.setError(getString(R.string.planner_error_title));
                    }
                }));
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_meal_planner, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_shopping_list) {
            startActivity(ShoppingListActivity.newIntent(this));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** the parent declared in the manifest (InventoryActivity) may not exist yet, so just close */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
