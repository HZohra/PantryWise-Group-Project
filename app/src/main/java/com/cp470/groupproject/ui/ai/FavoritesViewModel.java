package com.cp470.groupproject.ui.ai;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.repository.PantryRepository;

import java.util.List;

/**
 * State and logic for the Favourites screen (MVVM: no Views here).
 * The list follows the database, so removing or restoring a recipe updates it by itself.
 */
public class FavoritesViewModel extends AndroidViewModel {

    @Nullable private PantryRepository pantryRepository;
    @Nullable private LiveData<List<FavoriteRecipe>> favorites;

    public FavoritesViewModel(@NonNull Application application) {
        this(application, null);
    }

    /** @param pantryRepository the favourites storage, or null to create the real one when needed */
    FavoritesViewModel(@NonNull Application application, @Nullable PantryRepository pantryRepository) {
        super(application);
        this.pantryRepository = pantryRepository;
    }

    /** Saved Edamam and AI recipes together, most recently saved first, with ingredients and steps. */
    @NonNull
    public LiveData<List<FavoriteRecipe>> getFavorites() {
        if (favorites == null) {
            favorites = pantry().getFavoriteRecipes();
        }
        return favorites;
    }

    /** Main thread. Removes the recipe; keep the object to restore it with {@link #restore}. */
    public void remove(@NonNull FavoriteRecipe recipe) {
        pantry().removeFavoriteRecipe(recipe.recipeId);
    }

    /** Main thread. Undo: saves the removed recipe again with its ingredients, steps and save time. */
    public void restore(@NonNull FavoriteRecipe recipe) {
        pantry().saveFavoriteRecipe(recipe);
    }

    @NonNull
    private PantryRepository pantry() {
        if (pantryRepository == null) {
            pantryRepository = new PantryRepository(getApplication());
        }
        return pantryRepository;
    }
}