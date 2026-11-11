package com.cp470.groupproject.ui.ai;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.repository.PantryRepository;

import java.util.List;

/**
 * A saved recipe read from the favourites (offline, no AI call). It follows the database, so if
 * the recipe is removed while the screen is open, the value becomes null.
 */
public class SavedRecipeViewModel extends AndroidViewModel {

    @Nullable private PantryRepository pantryRepository;
    @Nullable private LiveData<FavoriteRecipe> recipe;

    public SavedRecipeViewModel(@NonNull Application application) {
        super(application);
    }

    /** Main thread. The saved recipe with this id (with ingredients and steps), or null. */
    @NonNull
    public LiveData<FavoriteRecipe> getRecipe(@NonNull String recipeId) {
        if (recipe == null) {
            recipe = Transformations.map(pantry().getFavoriteRecipes(), list -> find(list, recipeId));
        }
        return recipe;
    }

    /** @return the recipe with this id in the list, or null */
    @Nullable
    static FavoriteRecipe find(@Nullable List<FavoriteRecipe> list, @NonNull String recipeId) {
        if (list == null) return null;
        for (FavoriteRecipe item : list) {
            if (recipeId.equals(item.recipeId)) return item;
        }
        return null;
    }

    @NonNull
    private PantryRepository pantry() {
        if (pantryRepository == null) {
            pantryRepository = new PantryRepository(getApplication());
        }
        return pantryRepository;
    }
}