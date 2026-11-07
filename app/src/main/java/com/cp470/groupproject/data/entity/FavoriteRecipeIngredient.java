package com.cp470.groupproject.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One ingredient of a favorite recipe. the row is deleted together with its
 * {@link FavoriteRecipe} (cascade).
 */
@Entity(tableName = "favorite_recipe_ingredients",
        foreignKeys = @ForeignKey(entity = FavoriteRecipe.class, parentColumns = "id",
                childColumns = "favoriteId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("favoriteId")})
public class FavoriteRecipeIngredient extends IngredientLine {
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** id of the favorite recipe this ingredient belongs to */
    public long favoriteId;

    /** empty constructor required by room */
    public FavoriteRecipeIngredient() { }

    /**
     * @param text the ingredient as typed, such as "2 eggs"
     * @param position order inside the recipe
     */
    @Ignore
    public FavoriteRecipeIngredient(@NonNull String text, int position) {
        fill(text, position);
    }
}
