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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.dto.AiRecipeDto;
import com.cp470.groupproject.data.remote.dto.AiRecipeIngredientDto;
import com.cp470.groupproject.data.remote.dto.AiRecipeResponse;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * AI recipe from expiring food (View in MVVM, US-2 / UC-2).
 * Asks for consent first; Cancel closes the screen and sends nothing (US-6).
 */
public class AiRecipeActivity extends AppCompatActivity {

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, AiRecipeActivity.class);
    }

    private AiRecipeViewModel viewModel;
    private View root;
    private View loading;
    private View content;
    private TextView message;
    private MaterialButton againButton;
    @Nullable private AlertDialog consentDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_recipe);

        root = findViewById(R.id.ai_gen_root);
        loading = findViewById(R.id.ai_gen_loading);
        content = findViewById(R.id.ai_gen_content);
        message = findViewById(R.id.ai_gen_message);
        againButton = findViewById(R.id.ai_gen_again);

        MaterialToolbar toolbar = findViewById(R.id.ai_gen_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        applyWindowInsets();

        viewModel = new ViewModelProvider(this).get(AiRecipeViewModel.class);
        viewModel.getState().observe(this, this::render);
        againButton.setOnClickListener(v -> viewModel.generate());

        if (viewModel.isIdle()) {
            if (viewModel.needsConsent()) {
                showConsentDialog();
            } else {
                viewModel.generate();
            }
        }
    }

    private void applyWindowInsets() {
        View appBar = findViewById(R.id.ai_gen_app_bar);
        View scroll = findViewById(R.id.ai_gen_scroll);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            appBar.setPadding(bars.left, bars.top, bars.right, 0);
            scroll.setPadding(bars.left, 0, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** US-6: Continue generates; Cancel (or Back) closes the screen without sending anything. */
    private void showConsentDialog() {
        consentDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ai_consent_title)
                .setMessage(R.string.ai_consent_recipe_message)
                .setPositiveButton(R.string.ai_consent_continue, (dialog, which) -> {
                    viewModel.giveConsent();
                    viewModel.generate();
                })
                .setNegativeButton(R.string.ai_consent_cancel, (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    // ---------------------------------------------------------------- rendering

    private void render(@Nullable AiRecipeViewModel.UiState state) {
        if (state == null) return;
        loading.setVisibility(state.kind == AiRecipeViewModel.UiState.Kind.LOADING ? View.VISIBLE : View.GONE);
        content.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        againButton.setVisibility(View.GONE);

        if (state.kind == AiRecipeViewModel.UiState.Kind.SUCCESS && state.response != null) {
            bindRecipe(state.response);
            content.setVisibility(View.VISIBLE);
            againButton.setText(R.string.ai_gen_try_another);
            againButton.setVisibility(View.VISIBLE);
        } else if (state.kind == AiRecipeViewModel.UiState.Kind.ERROR && state.error != null) {
            message.setText(AiRecipeErrorText.message(this, state.error));
            message.setVisibility(View.VISIBLE);
            againButton.setText(R.string.ai_gen_try_again);
            againButton.setVisibility(View.VISIBLE);
        }
    }

    private void bindRecipe(@NonNull AiRecipeResponse response) {
        AiRecipeDto recipe = response.recipe;

        ((TextView) findViewById(R.id.ai_gen_name)).setText(recipe.title);

        String servings = null;
        if (recipe.servings != null && recipe.servings > 0) {
            servings = getResources().getQuantityString(R.plurals.ai_detail_servings, recipe.servings, recipe.servings);
        }
        String minutes = recipe.totalTimeMinutes != null && recipe.totalTimeMinutes > 0
                ? getString(R.string.ai_recipes_minutes, recipe.totalTimeMinutes) : null;
        TextView meta = findViewById(R.id.ai_gen_meta);
        String metaText = RecipeCardFormatter.joinMeta(servings, minutes);
        meta.setText(metaText);
        meta.setVisibility(metaText == null ? View.GONE : View.VISIBLE);

        TextView madeFor = findViewById(R.id.ai_gen_made_for);
        if (response.usedExpiring && response.expiringItems != null && !response.expiringItems.isEmpty()) {
            madeFor.setText(getString(R.string.ai_gen_made_for, String.join(", ", response.expiringItems)));
        } else {
            madeFor.setText(R.string.ai_gen_nothing_expiring); // UC-2 1a
        }

        bindIngredients(recipe);
        bindSteps(recipe);

        TextView tip = findViewById(R.id.ai_gen_tip);
        if (recipe.tip != null && !recipe.tip.trim().isEmpty()) {
            tip.setText(getString(R.string.ai_gen_tip, recipe.tip.trim()));
            tip.setVisibility(View.VISIBLE);
        } else {
            tip.setVisibility(View.GONE);
        }
    }

    /** Expiring soon (tertiary) > In pantry (primary) > Missing (error colour); staples get no label. */
    private void bindIngredients(@NonNull AiRecipeDto recipe) {
        LinearLayout container = findViewById(R.id.ai_gen_ingredients);
        container.removeAllViews();
        if (recipe.ingredients == null) return;
        LayoutInflater inflater = getLayoutInflater();

        for (AiRecipeIngredientDto ingredient : recipe.ingredients) {
            View row = inflater.inflate(R.layout.item_recipe_ingredient, container, false);
            String amount = ingredient.amount == null ? "" : ingredient.amount;
            ((TextView) row.findViewById(R.id.ai_ingredient_text)).setText((amount + " " + ingredient.name).trim());

            TextView status = row.findViewById(R.id.ai_ingredient_status);
            if (ingredient.expiring) {
                status.setText(R.string.ai_gen_expiring);
                status.setTextColor(MaterialColors.getColor(status, com.google.android.material.R.attr.colorTertiary));
                status.setVisibility(View.VISIBLE);
            } else if (ingredient.inPantry) {
                status.setText(R.string.ai_detail_in_pantry);
                status.setTextColor(MaterialColors.getColor(status, androidx.appcompat.R.attr.colorPrimary));
                status.setVisibility(View.VISIBLE);
            } else if (!ingredient.staple) {
                status.setText(R.string.ai_detail_missing); // colorError comes from the layout
                status.setVisibility(View.VISIBLE);
            }
            container.addView(row);
        }
    }

    private void bindSteps(@NonNull AiRecipeDto recipe) {
        LinearLayout container = findViewById(R.id.ai_gen_steps);
        container.removeAllViews();
        if (recipe.steps == null) return;
        LayoutInflater inflater = getLayoutInflater();

        for (int i = 0; i < recipe.steps.size(); i++) {
            View row = inflater.inflate(R.layout.item_ai_step, container, false);
            ((TextView) row.findViewById(R.id.ai_step_number)).setText(getString(R.string.ai_gen_step_number, i + 1));
            ((TextView) row.findViewById(R.id.ai_step_text)).setText(recipe.steps.get(i));
            container.addView(row);
        }
    }

    @Override
    protected void onDestroy() {
        if (consentDialog != null && consentDialog.isShowing()) {
            consentDialog.setOnCancelListener(null); // rotation: don't close the screen, the dialog comes back
            consentDialog.dismiss();
        }
        super.onDestroy();
    }

    /** Back to the Recipes list. */
    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}