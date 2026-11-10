package com.cp470.groupproject.ui.ai;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
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

import com.bumptech.glide.Glide;
import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.dto.NutritionDto;
import com.cp470.groupproject.data.remote.dto.RecipeDto;
import com.cp470.groupproject.data.remote.dto.SubstituteDto;
import com.cp470.groupproject.data.remote.dto.SubstituteResponse;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * Recipe details (View in MVVM): photo, ingredients marked In pantry / Missing, nutrition,
 * "View original recipe" (implicit Intent), "Find substitute" (bottom sheet) and
 * "Ask assistant how to make this" (opens the chat with a draft).
 */
public class RecipeDetailActivity extends AppCompatActivity {

    private static final String EXTRA_RECIPE_JSON = "com.cp470.groupproject.ui.ai.EXTRA_RECIPE_JSON";
    private static final Gson GSON = new Gson();

    /** Opens the details for a recipe from the list (no new backend call needed). */
    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull RecipeDto recipe) {
        return new Intent(context, RecipeDetailActivity.class).putExtra(EXTRA_RECIPE_JSON, GSON.toJson(recipe));
    }

    private RecipeDetailViewModel viewModel;
    private View root;
    @Nullable private BottomSheetDialog sheet;
    @Nullable private View sheetView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipe_detail);
        root = findViewById(R.id.ai_detail_root);

        viewModel = new ViewModelProvider(this).get(RecipeDetailViewModel.class);
        if (!viewModel.setRecipeIfAbsent(readRecipe(getIntent()))) {
            finish(); // opened without a usable recipe: nothing to show
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.ai_detail_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        applyWindowInsets();

        RecipeDto recipe = viewModel.getRecipe();
        bindRecipe(recipe);
        bindIngredients();
        bindNutrition(recipe.nutrition);

        viewModel.getSubstitute().observe(this, this::renderSubstitutes);
    }

    @Nullable
    private static RecipeDto readRecipe(@NonNull Intent intent) {
        String json = intent.getStringExtra(EXTRA_RECIPE_JSON);
        if (json == null) return null;
        try {
            return GSON.fromJson(json, RecipeDto.class);
        } catch (JsonParseException e) {
            return null;
        }
    }

    private void applyWindowInsets() {
        View appBar = findViewById(R.id.ai_detail_app_bar);
        View scroll = findViewById(R.id.ai_detail_scroll);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            appBar.setPadding(bars.left, bars.top, bars.right, 0);
            scroll.setPadding(bars.left, 0, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // ---------------------------------------------------------------- recipe header

    private void bindRecipe(@NonNull RecipeDto recipe) {
        ImageView image = findViewById(R.id.ai_detail_image);
        image.setContentDescription(getString(R.string.ai_recipes_image_description, recipe.title));
        if (recipe.image != null && !recipe.image.isEmpty()) {
            Glide.with(this).load(recipe.image).centerCrop().into(image);
        }

        ((TextView) findViewById(R.id.ai_detail_name)).setText(recipe.title);

        String servings = null;
        if (recipe.servings != null && recipe.servings > 0) {
            int count = (int) Math.round(recipe.servings);
            servings = getResources().getQuantityString(R.plurals.ai_detail_servings, count, count);
        }
        Integer minutes = RecipeCardFormatter.minutes(recipe.totalTimeMinutes);
        showOrHide(R.id.ai_detail_meta, RecipeCardFormatter.joinMeta(servings,
                minutes == null ? null : getString(R.string.ai_recipes_minutes, minutes)));

        showOrHide(R.id.ai_detail_source, recipe.sourceName == null || recipe.sourceName.trim().isEmpty()
                ? null : getString(R.string.ai_detail_source, recipe.sourceName));

        showOrHide(R.id.ai_detail_have, RecipeCardFormatter.hasMatch(recipe.match)
                ? getResources().getQuantityString(R.plurals.ai_recipes_have_count,
                recipe.match.totalCount, recipe.match.haveCount, recipe.match.totalCount)
                : null);

        String expiring = RecipeCardFormatter.expiringNames(recipe.match);
        showOrHide(R.id.ai_detail_expiring,
                expiring == null ? null : getString(R.string.ai_recipes_uses_expiring, expiring));

        MaterialButton viewOriginal = findViewById(R.id.ai_detail_view_original);
        if (isWebLink(recipe.sourceUrl)) {
            viewOriginal.setOnClickListener(v -> openOriginal(recipe.sourceUrl));
        } else {
            viewOriginal.setVisibility(View.GONE);
        }

        findViewById(R.id.ai_detail_ask).setOnClickListener(v -> askAssistant(recipe));
    }

    /** Sets the text, or hides the view when there is nothing to show. */
    private void showOrHide(int viewId, @Nullable String text) {
        TextView view = findViewById(viewId);
        if (text == null) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        }
    }

    private static boolean isWebLink(@Nullable String url) {
        if (url == null) return false;
        String scheme = Uri.parse(url).getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    /** Implicit Intent: the user's browser opens the source page. */
    private void openOriginal(@NonNull String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(root, R.string.ai_detail_no_browser, Snackbar.LENGTH_LONG).show();
        }
    }

    /** Opens the chat with a ready-made question (not sent: the user reviews it first). */
    private void askAssistant(@NonNull RecipeDto recipe) {
        int maxLength = getResources().getInteger(R.integer.ai_message_max_length);
        int roomForList = maxLength - getString(R.string.ai_detail_ask_prompt, recipe.title, "").length();
        String list = RecipeDetailViewModel.buildIngredientList(recipe.ingredientLines, Math.max(0, roomForList));
        String question = getString(R.string.ai_detail_ask_prompt, recipe.title, list);
        startActivity(ChatLauncher.openWithDraft(this, question));
    }

    // ---------------------------------------------------------------- ingredients and nutrition

    private void bindIngredients() {
        LinearLayout container = findViewById(R.id.ai_detail_ingredients);
        container.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();

        for (RecipeDetailViewModel.IngredientRow row : viewModel.getRows()) {
            View item = inflater.inflate(R.layout.item_recipe_ingredient, container, false);
            ((TextView) item.findViewById(R.id.ai_ingredient_text)).setText(row.line);
            TextView status = item.findViewById(R.id.ai_ingredient_status);
            MaterialButton substitute = item.findViewById(R.id.ai_ingredient_substitute);

            if (row.status == IngredientMatcher.Status.IN_PANTRY) {
                status.setText(R.string.ai_detail_in_pantry);
                status.setTextColor(MaterialColors.getColor(status, androidx.appcompat.R.attr.colorPrimary));
                status.setVisibility(View.VISIBLE);
            } else if (row.status == IngredientMatcher.Status.MISSING && row.name != null) {
                status.setText(R.string.ai_detail_missing); // colorError comes from the layout
                status.setVisibility(View.VISIBLE);
                substitute.setVisibility(View.VISIBLE);
                substitute.setOnClickListener(v -> viewModel.findSubstitute(row.name));
            }
            container.addView(item);
        }
    }

    private void bindNutrition(@Nullable NutritionDto nutrition) {
        ((TextView) findViewById(R.id.ai_detail_nutrition_title)).setText(
                nutrition != null && !nutrition.perServing
                        ? R.string.ai_detail_nutrition_whole
                        : R.string.ai_detail_nutrition);
        ((TextView) findViewById(R.id.ai_detail_calories)).setText(
                valueOrDash(nutrition == null ? null : nutrition.calories, R.string.ai_detail_kcal));
        ((TextView) findViewById(R.id.ai_detail_protein)).setText(
                valueOrDash(nutrition == null ? null : nutrition.protein, R.string.ai_detail_grams));
        ((TextView) findViewById(R.id.ai_detail_carbs)).setText(
                valueOrDash(nutrition == null ? null : nutrition.carbs, R.string.ai_detail_grams));
        ((TextView) findViewById(R.id.ai_detail_fat)).setText(
                valueOrDash(nutrition == null ? null : nutrition.fat, R.string.ai_detail_grams));
    }

    @NonNull
    private String valueOrDash(@Nullable Integer value, int formatRes) {
        return value == null ? getString(R.string.ai_detail_unknown) : getString(formatRes, value);
    }

    // ---------------------------------------------------------------- substitutes sheet

    private void renderSubstitutes(@Nullable RecipeDetailViewModel.SubstituteState state) {
        if (state == null) {
            closeSheetQuietly();
            return;
        }
        View view = ensureSheet();

        ((TextView) view.findViewById(R.id.ai_sub_title)).setText(getString(R.string.ai_sub_title, state.ingredient));
        view.findViewById(R.id.ai_sub_progress).setVisibility(state.loading ? View.VISIBLE : View.GONE);
        TextView source = view.findViewById(R.id.ai_sub_source);
        TextView message = view.findViewById(R.id.ai_sub_message);
        TextView tip = view.findViewById(R.id.ai_sub_tip);
        LinearLayout list = view.findViewById(R.id.ai_sub_list);
        list.removeAllViews();
        source.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        tip.setVisibility(View.GONE);

        if (state.loading) return;

        if (state.error != null) {
            message.setText(RecipeErrorText.message(this, state.error));
            message.setVisibility(View.VISIBLE);
            return;
        }

        SubstituteResponse response = state.response;
        if (response == null) return;
        source.setText(SubstituteResponse.SOURCE_AI.equals(response.source)
                ? R.string.ai_sub_source_ai : R.string.ai_sub_source_local);
        source.setVisibility(View.VISIBLE);

        if (response.substitutes == null || response.substitutes.isEmpty()) {
            message.setText(R.string.ai_sub_none);
            message.setVisibility(View.VISIBLE);
        } else {
            LayoutInflater inflater = getLayoutInflater();
            for (SubstituteDto item : response.substitutes) {
                View row = inflater.inflate(R.layout.item_substitute, list, false);
                ((TextView) row.findViewById(R.id.ai_sub_name)).setText(item.name);
                ((TextView) row.findViewById(R.id.ai_sub_amount)).setText(item.amount);
                TextView notes = row.findViewById(R.id.ai_sub_notes);
                notes.setText(item.notes);
                notes.setVisibility(item.notes == null || item.notes.isEmpty() ? View.GONE : View.VISIBLE);
                row.findViewById(R.id.ai_sub_owned).setVisibility(item.inPantry ? View.VISIBLE : View.GONE);
                list.addView(row);
            }
        }
        if (response.tip != null && !response.tip.trim().isEmpty()) {
            tip.setText(response.tip);
            tip.setVisibility(View.VISIBLE);
        }
    }

    /** Creates and shows the sheet if it isn't open yet; returns its content view. */
    @NonNull
    private View ensureSheet() {
        if (sheet == null || sheetView == null) {
            sheetView = getLayoutInflater().inflate(R.layout.sheet_substitutes, null);
            BottomSheetDialog dialog = new BottomSheetDialog(this);
            dialog.setContentView(sheetView);
            // Closed by the user (swipe, Back, tap outside) → tell the ViewModel
            dialog.setOnDismissListener(d -> {
                if (sheet == d) {
                    sheet = null;
                    viewModel.closeSubstitutes();
                }
            });
            sheet = dialog;
            dialog.show();
        }
        return sheetView;
    }

    /** Closes the sheet without telling the ViewModel (used for rotation and when the state is already null). */
    private void closeSheetQuietly() {
        if (sheet != null) {
            BottomSheetDialog dialog = sheet;
            sheet = null; // so the dismiss listener ignores this
            dialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        closeSheetQuietly(); // on rotation the ViewModel keeps the state and the new screen re-opens it
        super.onDestroy();
    }

    /** Back to the Recipes list. */
    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}