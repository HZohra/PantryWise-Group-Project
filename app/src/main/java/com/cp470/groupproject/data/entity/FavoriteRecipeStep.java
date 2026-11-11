package com.cp470.groupproject.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * One cooking step of a favorite recipe. the row is deleted together with its
 * {@link FavoriteRecipe} (cascade), like the ingredient rows.
 */
@Entity(tableName = "favorite_recipe_steps",
        foreignKeys = @ForeignKey(entity = FavoriteRecipe.class, parentColumns = "id",
                childColumns = "favoriteId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("favoriteId")})
public class FavoriteRecipeStep {
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** id of the favorite recipe this step belongs to */
    public long favoriteId;

    /** order of the step inside the recipe, starting at 0 */
    public int position;

    /** what to do, such as "Beat the eggs with the milk" */
    @NonNull
    public String text = "";

    /** empty constructor required by room */
    public FavoriteRecipeStep() { }

    /**
     * @param text the step as written, surrounding spaces are removed
     * @param position order inside the recipe
     */
    @Ignore
    public FavoriteRecipeStep(@NonNull String text, int position) {
        this.text = text.trim();
        this.position = position;
    }
}