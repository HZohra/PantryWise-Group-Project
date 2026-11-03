package com.cp470.groupproject.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.cp470.groupproject.data.dao.CategoryDao;
import com.cp470.groupproject.data.dao.ConsumptionHistoryDao;
import com.cp470.groupproject.data.dao.FavoriteRecipeDao;
import com.cp470.groupproject.data.dao.FoodItemDao;
import com.cp470.groupproject.data.dao.ShoppingItemDao;
import com.cp470.groupproject.data.dao.StorageLocationDao;
import com.cp470.groupproject.data.entity.Category;
import com.cp470.groupproject.data.entity.ConsumptionHistory;
import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.data.entity.StorageLocation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Central room database of pantrywise (sqlite underneath), exposed as a singleton.
 * The first time the database file is created, it is seeded with the default
 * storage locations and categories.
 */

@Database(entities = {FoodItem.class, Category.class, StorageLocation.class,
        ShoppingItem.class, FavoriteRecipe.class, ConsumptionHistory.class},
        version = 1, exportSchema = true)
@TypeConverters(Converters.class)
public abstract class PantryDatabase extends RoomDatabase {

    /** file name of the sqlite database */
    public static final String DATABASE_NAME = "pantrywise.db";

    /**
     * single background thread for all writes. it keeps the ui thread free and
     * guarantees that writes are applied in the order they were requested
     */
    public static final ExecutorService WRITE_EXECUTOR = Executors.newSingleThreadExecutor();

    private static volatile PantryDatabase instance;

    public abstract FoodItemDao foodItemDao();

    public abstract CategoryDao categoryDao();

    public abstract StorageLocationDao storageLocationDao();

    public abstract ShoppingItemDao shoppingItemDao();

    public abstract FavoriteRecipeDao favoriteRecipeDao();

    public abstract ConsumptionHistoryDao consumptionHistoryDao();

    /** @return the process-wide database, created on first use */
    public static PantryDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (PantryDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    PantryDatabase.class, DATABASE_NAME)
                            .addMigrations(Migrations.ALL)
                            .addCallback(seedCallback())
                            .build();
                }
            }
        }
        return instance;
    }

    /** callback that seeds the default rows the first time the database file is created */
    public static RoomDatabase.Callback seedCallback() {
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                seed(db);
            }
        };
    }

    /** inserts the default storage locations and categories (ids follow the array order) */
    public static void seed(@NonNull SupportSQLiteDatabase db) {
        for (String key : StorageLocation.DEFAULT_KEYS) {
            db.execSQL("INSERT INTO storage_locations (name) VALUES (?)", new Object[]{key});
        }
        for (String key : Category.DEFAULT_KEYS) {
            db.execSQL("INSERT INTO categories (name) VALUES (?)", new Object[]{key});
        }
    }
}