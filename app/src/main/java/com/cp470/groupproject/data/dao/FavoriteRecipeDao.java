package com.cp470.groupproject.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.cp470.groupproject.data.entity.FavoriteRecipe;

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

    /** favorites, most recently saved first */
    @Query("SELECT * FROM favorite_recipes ORDER BY savedAt DESC")
    LiveData<List<FavoriteRecipe>> observeAll();

    /** snapshot of the favorites. background thread only */
    @Query("SELECT * FROM favorite_recipes ORDER BY savedAt DESC")
    List<FavoriteRecipe> getAllSync();

    /** emits 1 if the recipe is saved, otherwise 0 */
    @Query("SELECT COUNT(*) FROM favorite_recipes WHERE recipeId = :recipeId")
    LiveData<Integer> observeIsFavorite(String recipeId);
}