package com.cp470.groupproject.ui.ai;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.AiError;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Recipes screen (View in MVVM): pantry recommendations, keyword search, Zero-Waste Mode,
 * and the entry point to the AI recipe built from expiring food.
 * Only draws state from {@link RecipeViewModel} and forwards user actions to it.
 */
public class RecipesActivity extends AppCompatActivity {

    private RecipeViewModel viewModel;
    private RecipeAdapter adapter;

    private View root;
    private View appBar;
    private View content;
    private TextInputLayout searchLayout;
    private TextInputEditText searchInput;
    private LinearProgressIndicator progress;
    private TextView notice;
    private TextView emptyView;

    @Nullable private Snackbar errorSnackbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipes);

        root = findViewById(R.id.ai_recipes_root);
        appBar = findViewById(R.id.ai_recipes_app_bar);
        content = findViewById(R.id.ai_recipes_content);
        searchLayout = findViewById(R.id.ai_recipes_search_layout);
        searchInput = findViewById(R.id.ai_recipes_search);
        progress = findViewById(R.id.ai_recipes_progress);
        notice = findViewById(R.id.ai_recipes_notice);
        emptyView = findViewById(R.id.ai_recipes_empty);
        MaterialSwitch zeroWasteSwitch = findViewById(R.id.ai_recipes_zero_waste);
        RecyclerView list = findViewById(R.id.ai_recipes_list);

        MaterialToolbar toolbar = findViewById(R.id.ai_recipes_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        applyWindowInsets();

        // Tapping a card opens Recipe details (the recipe is passed along, no new backend call)
        adapter = new RecipeAdapter(recipe -> startActivity(RecipeDetailActivity.newIntent(this, recipe)));
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        // UC-2 / Zero-Waste "Ask AI": the AI recipe screen asks for consent itself
        findViewById(R.id.ai_recipes_generate).setOnClickListener(
                v -> startActivity(AiRecipeActivity.newIntent(this)));

        viewModel = new ViewModelProvider(this).get(RecipeViewModel.class);

        // Set the saved state BEFORE adding the listener, so it doesn't count as a user change
        zeroWasteSwitch.setChecked(viewModel.isZeroWaste());
        zeroWasteSwitch.setOnCheckedChangeListener((button, checked) -> viewModel.setZeroWaste(checked));

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                onSearch();
                return true;
            }
            return false;
        });
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                searchLayout.setError(null);
                if (s.toString().trim().isEmpty()) {
                    viewModel.clearSearch(); // ✕ or deleting the text brings back recommendations
                }
            }
        });

        viewModel.getRecipes().observe(this, adapter::submitList);
        viewModel.isLoading().observe(this, isLoading ->
                progress.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE));
        viewModel.getNotice().observe(this, this::showNotice);
        viewModel.isEmpty().observe(this, isEmpty ->
                emptyView.setVisibility(Boolean.TRUE.equals(isEmpty) ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(this, this::showError);

        viewModel.loadIfNeeded();
    }

    /**
     * The status-bar space is padding on the ROOT (not the app bar), so it stays in place and keeps
     * the background colour when the header scrolls away; nothing shows through behind the clock.
     * The list stays above the navigation bar and keyboard.
     */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            root.setPadding(0, bars.top, 0, 0);
            appBar.setPadding(bars.left, 0, bars.right, 0);
            content.setPadding(bars.left, 0, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void onSearch() {
        Editable text = searchInput.getText();
        if (viewModel.search(text == null ? null : text.toString())) {
            searchLayout.setError(null);
            WindowCompat.getInsetsController(getWindow(), searchInput).hide(WindowInsetsCompat.Type.ime());
            dismissError();
        } else {
            searchLayout.setError(getString(R.string.ai_recipes_search_empty)); // no API call for an empty search
        }
    }

    private void showNotice(@Nullable Integer resId) {
        if (resId == null || resId == 0) {
            notice.setVisibility(View.GONE);
        } else {
            notice.setText(resId);
            notice.setVisibility(View.VISIBLE);
        }
    }

    /** Network problems stay until Retry or the next action; "busy" disappears by itself. */
    private void showError(@Nullable AiError error) {
        if (error == null) {
            return;
        }
        dismissError();
        boolean retryable = RecipeErrorText.isRetryable(error);
        errorSnackbar = Snackbar.make(root, RecipeErrorText.message(this, error),
                retryable ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG);
        if (retryable) {
            errorSnackbar.setAction(R.string.ai_retry, v -> viewModel.retry());
        }
        errorSnackbar.show();
        viewModel.onErrorShown();
    }

    private void dismissError() {
        if (errorSnackbar != null) {
            errorSnackbar.dismiss();
            errorSnackbar = null;
        }
    }

    /** Tahmina's InventoryActivity doesn't exist yet, so Up behaves like Back for now. */
    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}