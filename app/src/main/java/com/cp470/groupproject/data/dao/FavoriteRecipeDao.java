package com.cp470.groupproject.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.entity.FavoriteRecipeIngredient;
import com.cp470.groupproject.data.entity.FavoriteRecipeStep;
import com.cp470.groupproject.data.model.FavoriteRecipeWithIngredients;

import java.util.List;

/** Data access for saved favorite recipes. */
@Dao
public interface FavoriteRecipeDao {
    /**
     * saves a favorite, saving the same recipeId again replaces the old row.
     *
     * @return the row id
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(FavoriteRecipe recipe);

    /**
     * removes a favorite by the id from the recipe source.
     *
     * @return rows deleted
     */
    @Query("DELETE FROM favorite_recipes WHERE recipeId = :recipeId")
    int deleteByRecipeId(String recipeId);

    /**
     * adds the ingredient rows of a favorite (their {@code favoriteId} must already be set).
     * background thread only
     */
    @Insert
    void insertIngredients(List<FavoriteRecipeIngredient> ingredients);

    /**
     * adds the cooking steps of a favorite (their {@code favoriteId} must already be set).
     * background thread only
     */
    @Insert
    void insertSteps(List<FavoriteRecipeStep> steps);

    /** favorites with their ingredients, most recently saved first */
    @Transaction
    @Query("SELECT * FROM favorite_recipes ORDER BY savedAt DESC")
    LiveData<List<FavoriteRecipeWithIngredients>> observeAll();

    /** snapshot of the favorites with their ingredients. background thread only */
    @Transaction
    @Query("SELECT * FROM favorite_recipes ORDER BY savedAt DESC")
    List<FavoriteRecipeWithIngredients> getAllSync();

    /**
     * one favorite with its ingredients and steps, or null if it is not saved.
     * background thread only
     */
    @Transaction
    @Query("SELECT * FROM favorite_recipes WHERE recipeId = :recipeId")
    FavoriteRecipeWithIngredients getByRecipeIdSync(String recipeId);

    /** emits 1 if the recipe is saved, otherwise 0 */
    @Query("SELECT COUNT(*) FROM favorite_recipes WHERE recipeId = :recipeId")
    LiveData<Integer> observeIsFavorite(String recipeId);
}