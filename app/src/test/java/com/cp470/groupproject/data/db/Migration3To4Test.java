package com.cp470.groupproject.data.db;

import static org.junit.Assert.assertEquals;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.io.IOException;

/**
 * Checks migration 3 to 4 (favorite details and the steps table). the helper builds a real
 * version 3 database from {@code 3.json}, runs the migration and asks room to validate the result
 * against {@code 4.json}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class Migration3To4Test {

    private static final String DB_NAME = "migration-3-4-test.db";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), PantryDatabase.class);

    private SupportSQLiteDatabase migrate(String... sqlOnVersion3) throws IOException {
        SupportSQLiteDatabase old = helper.createDatabase(DB_NAME, 3);
        for (String sql : sqlOnVersion3) {
            old.execSQL(sql);
        }
        old.close();
        return helper.runMigrationsAndValidate(DB_NAME, 4, true, Migrations.MIGRATION_3_4);
    }

    private static final String OMELETTE =
            "INSERT INTO favorite_recipes (id, recipeId, title, savedAt) "
                    + "VALUES (3, 'edamam:abc', 'Omelette', 99)";

    private static final String OMELETTE_EGGS =
            "INSERT INTO favorite_recipe_ingredients (favoriteId, position, text, normalizedName, "
                    + "quantity, unit, explicitQuantity) VALUES (3, 0, '2 eggs', 'eggs', 2, 'pcs', 1)";

    private static int count(SupportSQLiteDatabase db, String sql) {
        try (Cursor cursor = db.query(sql)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    private static String text(SupportSQLiteDatabase db, String sql) {
        try (Cursor cursor = db.query(sql)) {
            cursor.moveToFirst();
            return cursor.getString(0);
        }
    }

    @Test
    public void migrate3To4_existingFavorite_keepsItsData() throws IOException {
        SupportSQLiteDatabase db = migrate(OMELETTE);

        assertEquals("edamam:abc", text(db, "SELECT recipeId FROM favorite_recipes WHERE id = 3"));
        assertEquals("Omelette", text(db, "SELECT title FROM favorite_recipes WHERE id = 3"));
        assertEquals(99, count(db, "SELECT savedAt FROM favorite_recipes WHERE id = 3"));
    }

    @Test
    public void migrate3To4_existingFavorite_newColumnsAreNull() throws IOException {
        SupportSQLiteDatabase db = migrate(OMELETTE);

        assertEquals(1, count(db, "SELECT imageUrl IS NULL AND sourceName IS NULL "
                + "AND sourceUrl IS NULL AND servings IS NULL AND totalTimeMinutes IS NULL "
                + "AND tip IS NULL AND calories IS NULL AND protein IS NULL "
                + "AND carbs IS NULL AND fat IS NULL FROM favorite_recipes WHERE id = 3"));
    }

    @Test
    public void migrate3To4_existingIngredients_areKept() throws IOException {
        SupportSQLiteDatabase db = migrate(OMELETTE, OMELETTE_EGGS);

        assertEquals(1, count(db,
                "SELECT COUNT(*) FROM favorite_recipe_ingredients WHERE favoriteId = 3"));
        assertEquals("2 eggs", text(db,
                "SELECT text FROM favorite_recipe_ingredients WHERE favoriteId = 3"));
    }

    @Test
    public void migrate3To4_stepsTable_startsEmptyAndAcceptsSteps() throws IOException {
        SupportSQLiteDatabase db = migrate(OMELETTE);

        assertEquals(0, count(db, "SELECT COUNT(*) FROM favorite_recipe_steps"));
        db.execSQL("INSERT INTO favorite_recipe_steps (favoriteId, position, text) "
                + "VALUES (3, 0, 'Beat the eggs')");
        assertEquals("Beat the eggs", text(db,
                "SELECT text FROM favorite_recipe_steps WHERE favoriteId = 3"));
    }

    @Test
    public void migrate3To4_deletingTheFavorite_cascadesToStepsAndIngredients() throws IOException {
        SupportSQLiteDatabase db = migrate(OMELETTE, OMELETTE_EGGS);
        db.execSQL("INSERT INTO favorite_recipe_steps (favoriteId, position, text) "
                + "VALUES (3, 0, 'Beat the eggs')");

        // room turns foreign keys on when the app opens the database; this helper does not
        db.execSQL("PRAGMA foreign_keys = ON");
        db.execSQL("DELETE FROM favorite_recipes WHERE id = 3");

        assertEquals(0, count(db, "SELECT COUNT(*) FROM favorite_recipe_steps"));
        assertEquals(0, count(db, "SELECT COUNT(*) FROM favorite_recipe_ingredients"));
    }

    @Test
    public void migrate3To4_otherTables_areNotTouched() throws IOException {
        SupportSQLiteDatabase db = migrate(
                "INSERT INTO storage_locations (name) VALUES ('fridge')", OMELETTE);

        assertEquals(1, count(db, "SELECT COUNT(*) FROM storage_locations"));
    }

    @Test
    public void migrate2To4_allMigrationsInARow_reachVersion4() throws IOException {
        SupportSQLiteDatabase old = helper.createDatabase(DB_NAME, 2);
        old.execSQL("INSERT INTO favorite_recipes (id, recipeId, title, ingredients, savedAt) "
                + "VALUES (5, 'ai:xyz', 'Soup', 'water\nsalt', 7)");
        old.close();

        SupportSQLiteDatabase db = helper.runMigrationsAndValidate(DB_NAME, 4, true,
                Migrations.ALL);

        assertEquals("Soup", text(db, "SELECT title FROM favorite_recipes WHERE id = 5"));
        assertEquals(2, count(db,
                "SELECT COUNT(*) FROM favorite_recipe_ingredients WHERE favoriteId = 5"));
        assertEquals(1, count(db, "SELECT tip IS NULL FROM favorite_recipes WHERE id = 5"));
    }
}