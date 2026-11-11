package com.cp470.groupproject.ui.ai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.entity.FavoriteRecipe;

import java.util.List;

/**
 * A saved AI recipe, read from the favourites (offline, no AI call): title, servings and time,
 * ingredients, numbered steps and the tip.
 */
public class SavedRecipeActivity extends AppCompatActivity {

    private static final String EXTRA_RECIPE_ID = "com.cp470.groupproject.ui.ai.EXTRA_RECIPE_ID";

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull String recipeId) {
        return new Intent(context, SavedRecipeActivity.class).putExtra(EXTRA_RECIPE_ID, recipeId);
    }

    private View root;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_saved_recipe);
        root = findViewById(R.id.ai_saved_root);

        String recipeId = getIntent().getStringExtra(EXTRA_RECIPE_ID);
        if (recipeId == null) {
            finish(); // nothing to show
            return;
        }

        setSupportActionBar(findViewById(R.id.ai_saved_toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        applyWindowInsets();

        SavedRecipeViewModel viewModel = new ViewModelProvider(this).get(SavedRecipeViewModel.class);
        viewModel.getRecipe(recipeId).observe(this, this::render);
    }

    private void applyWindowInsets() {
        View appBar = findViewById(R.id.ai_saved_app_bar);
        View scroll = findViewById(R.id.ai_saved_scroll);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            appBar.setPadding(bars.left, bars.top, bars.right, 0);
            scroll.setPadding(bars.left, 0, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void render(@Nullable FavoriteRecipe recipe) {
        View content = findViewById(R.id.ai_saved_content);
        View message = findViewById(R.id.ai_saved_message);
        if (recipe == null) {
            content.setVisibility(View.GONE);
            message.setVisibility(View.VISIBLE);
            return;
        }
        message.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);

        ((TextView) findViewById(R.id.ai_saved_name)).setText(recipe.title);

        String servings = recipe.servings == null ? null : getResources()
                .getQuantityString(R.plurals.ai_detail_servings, recipe.servings, recipe.servings);
        String minutes = recipe.totalTimeMinutes == null ? null
                : getString(R.string.ai_recipes_minutes, recipe.totalTimeMinutes);
        TextView meta = findViewById(R.id.ai_saved_meta);
        String metaText = RecipeCardFormatter.joinMeta(servings, minutes);
        meta.setText(metaText);
        meta.setVisibility(metaText == null ? View.GONE : View.VISIBLE);

        LayoutInflater inflater = getLayoutInflater();

        LinearLayout ingredients = findViewById(R.id.ai_saved_ingredients);
        ingredients.removeAllViews();
        for (String line : recipe.ingredientTexts()) {
            View row = inflater.inflate(R.layout.item_recipe_ingredient, ingredients, false);
            ((TextView) row.findViewById(R.id.ai_ingredient_text)).setText(line);
            ingredients.addView(row); // no pantry labels: the pantry may have changed since saving
        }

        LinearLayout steps = findViewById(R.id.ai_saved_steps);
        steps.removeAllViews();
        List<String> stepTexts = recipe.stepTexts();
        for (int i = 0; i < stepTexts.size(); i++) {
            View row = inflater.inflate(R.layout.item_ai_step, steps, false);
            ((TextView) row.findViewById(R.id.ai_step_number)).setText(getString(R.string.ai_gen_step_number, i + 1));
            ((TextView) row.findViewById(R.id.ai_step_text)).setText(stepTexts.get(i));
            steps.addView(row);
        }

        TextView tip = findViewById(R.id.ai_saved_tip);
        if (recipe.tip != null) {
            tip.setText(getString(R.string.ai_gen_tip, recipe.tip));
            tip.setVisibility(View.VISIBLE);
        } else {
            tip.setVisibility(View.GONE);
        }
    }

    /** Back to the Favourites list. */
    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}