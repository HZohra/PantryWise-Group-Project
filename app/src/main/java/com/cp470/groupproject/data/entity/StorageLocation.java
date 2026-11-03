package com.cp470.groupproject.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Place where an item is stored (fridge, freezer, pantry).
 * the name is a stable key, same idea as in {@link Category}.
 */

@Entity(tableName = "storage_locations", indices = {@Index(value = "name", unique = true)})
public class StorageLocation {
    /** ids assigned by the seed order in PantryDatabase */
    public static final long FRIDGE_ID = 1, FREEZER_ID = 2, PANTRY_ID = 3;

    /** default location keys, in seed (= id) order */
    public static final String[] DEFAULT_KEYS = {"FRIDGE", "FREEZER", "PANTRY"};

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    /** empty constructor required by room */
    public StorageLocation() { }

    @Ignore
    public StorageLocation(@NonNull String name) {
        this.name = name;
    }
}