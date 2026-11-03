package com.cp470.groupproject.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Food categorization label (dairy, produce, meat, ...).
 *
 * {@link #name} stores a stable, language-independent key such as "DAIRY".
 * the ui translates it through string resources (category_dairy), so localization never touches
 * stored data.
 */

@Entity(tableName = "categories", indices = {@Index(value = "name", unique = true)})
public class Category {
    /** ids assigned by the seed order in PantryDatabase */
    public static final long DAIRY_ID = 1, PRODUCE_ID = 2, MEAT_ID = 3, PANTRY_ID = 4,
            BAKERY_ID = 5, BEVERAGES_ID = 6, FROZEN_ID = 7, OTHER_ID = 8;

    /** default category keys, in seed (= id) order */
    public static final String[] DEFAULT_KEYS = {"DAIRY", "PRODUCE", "MEAT", "PANTRY",
            "BAKERY", "BEVERAGES", "FROZEN", "OTHER"};

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    /** empty constructor required by room */
    public Category() { }

    @Ignore
    public Category(@NonNull String name) {
        this.name = name;
    }
}