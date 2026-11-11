package com.cp470.groupproject.ui.ai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

/**
 * Favourites (View in MVVM, UC-11): saved Edamam and AI recipes together, newest first.
 * The heart or a swipe removes a recipe, and the Snackbar's Undo brings it back.
 */
public class FavoritesActivity extends AppCompatActivity {

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, FavoritesActivity.class);
    }

    private FavoritesViewModel viewModel;
    private FavoriteAdapter adapter;
    private View root;
    private View emptyView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favorites);

        root = findViewById(R.id.ai_fav_root);
        emptyView = findViewById(R.id.ai_fav_empty);
        RecyclerView list = findViewById(R.id.ai_fav_list);

        MaterialToolbar toolbar = findViewById(R.id.ai_fav_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        applyWindowInsets();

        viewModel = new ViewModelProvider(this).get(FavoritesViewModel.class);

        adapter = new FavoriteAdapter(new FavoriteAdapter.Listener() {
            @Override
            public void onOpen(@NonNull FavoriteRecipe recipe) {
                if (recipe.isAi()) {
                    // AI recipes have their steps saved: read-only view, no AI call
                    startActivity(SavedRecipeActivity.newIntent(FavoritesActivity.this, recipe.recipeId));
                } else {
                    // Edamam recipes reuse Recipe details, rebuilt from the saved data (offline)
                    startActivity(RecipeDetailActivity.newIntent(FavoritesActivity.this,
                            FavoriteRecipeMapper.toRecipeDto(recipe)));
                }
            }

            @Override
            public void onRemove(@NonNull FavoriteRecipe recipe) {
                removeWithUndo(recipe);
            }
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        attachSwipeToRemove(list);

        viewModel.getFavorites().observe(this, recipes -> {
            adapter.submitList(recipes);
            boolean empty = recipes == null || recipes.isEmpty();
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        });
    }

    private void applyWindowInsets() {
        View appBar = findViewById(R.id.ai_fav_app_bar);
        View content = findViewById(R.id.ai_fav_content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            appBar.setPadding(bars.left, bars.top, bars.right, 0);
            content.setPadding(bars.left, 0, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Swiping a card left or right removes it (same as its heart). */
    private void attachSwipeToRemove(@NonNull RecyclerView list) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    removeWithUndo(adapter.getItemAt(position));
                }
            }
        }).attachToRecyclerView(list);
    }

    /** Removes the recipe; Undo saves the same object again (ingredients, steps and save time). */
    private void removeWithUndo(@NonNull FavoriteRecipe recipe) {
        viewModel.remove(recipe);
        Snackbar.make(root, R.string.ai_fav_removed, 6000)
                .setAction(R.string.ai_fav_undo, v -> viewModel.restore(recipe))
                .show();
    }

    /** Back to the previous screen. */
    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}