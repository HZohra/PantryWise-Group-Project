package com.cp470.groupproject.data.db;

import static org.junit.Assert.assertEquals;

import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.testing.MigrationTestHelper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.io.IOException;

/**
 * Checks migration 2 to 3 (ingredients move from a text column to tables). the helper builds a
 * real version 2 database from the exported schema {@code 2.json}, runs the migration and asks room
 * to validate the result against {@code 3.json}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class MigrationsTest {

    private static final String DB_NAME = "migration-test.db";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), PantryDatabase.class);

    private SupportSQLiteDatabase migrate(String... sqlOnVersion2) throws IOException {
        SupportSQLiteDatabase old = helper.createDatabase(DB_NAME, 2);
        for (String sql : sqlOnVersion2) {
            old.execSQL(sql);
        }
        old.close();
        return helper.runMigrationsAndValidate(DB_NAME, 3, true, Migrations.MIGRATION_2_3);
    }

    private static String meal(int id, String ingredients) {
        return "INSERT INTO meal_plans (id, planDate, mealType, recipeId, recipeTitle, "
                + "ingredients, completed, createdAt) VALUES (" + id + ", 1000, 'DINNER', 'r" + id
                + "', 'Meal " + id + "', '" + ingredients + "', 0, 5)";
    }

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
    public void migrate2To3_mealWithThreeLines_createsThreeParsedRowsInOrder() throws IOException {
        SupportSQLiteDatabase db = migrate(meal(1, "2 eggs\n200 g flour\nsoy sauce"));

        assertEquals(3, count(db, "SELECT COUNT(*) FROM meal_plan_ingredients WHERE mealPlanId = 1"));
        assertEquals("2 eggs", text(db,
                "SELECT text FROM meal_plan_ingredients WHERE position = 0"));
        assertEquals("eggs", text(db,
                "SELECT normalizedName FROM meal_plan_ingredients WHERE position = 0"));
        assertEquals("flour", text(db,
                "SELECT normalizedName FROM meal_plan_ingredients WHERE position = 1"));
        assertEquals("200.0", text(db,
                "SELECT quantity FROM meal_plan_ingredients WHERE position = 1"));
        assertEquals("g", text(db, "SELECT unit FROM meal_plan_ingredients WHERE position = 1"));
        assertEquals(0, count(db,
                "SELECT explicitQuantity FROM meal_plan_ingredients WHERE position = 2"));
    }

    @Test
    public void migrate2To3_mealWithoutIngredients_createsNoRows() throws IOException {
        SupportSQLiteDatabase db = migrate(meal(1, ""));

        assertEquals(1, count(db, "SELECT COUNT(*) FROM meal_plans"));
        assertEquals(0, count(db, "SELECT COUNT(*) FROM meal_plan_ingredients"));
    }

    @Test
    public void migrate2To3_blankLines_areDropped() throws IOException {
        SupportSQLiteDatabase db = migrate(meal(1, "milk\n\n   \nrice"));

        assertEquals(2, count(db, "SELECT COUNT(*) FROM meal_plan_ingredients"));
        assertEquals("rice", text(db,
                "SELECT text FROM meal_plan_ingredients WHERE position = 1"));
    }

    @Test
    public void migrate2To3_mealData_isKept() throws IOException {
        SupportSQLiteDatabase db = migrate(meal(7, "milk"));

        assertEquals("Meal 7", text(db, "SELECT recipeTitle FROM meal_plans WHERE id = 7"));
        assertEquals(1000, count(db, "SELECT planDate FROM meal_plans WHERE id = 7"));
        assertEquals(5, count(db, "SELECT createdAt FROM meal_plans WHERE id = 7"));
    }

    @Test
    public void migrate2To3_ingredientsOfTwoMeals_stayWithTheirOwnMeal() throws IOException {
        SupportSQLiteDatabase db = migrate(meal(1, "milk"), meal(2, "rice\nbeans"));

        assertEquals(1, count(db, "SELECT COUNT(*) FROM meal_plan_ingredients WHERE mealPlanId = 1"));
        assertEquals(2, count(db, "SELECT COUNT(*) FROM meal_plan_ingredients WHERE mealPlanId = 2"));
    }

    @Test
    public void migrate2To3_favoriteRecipe_createsRowsAndKeepsItsData() throws IOException {
        SupportSQLiteDatabase db = migrate(
                "INSERT INTO favorite_recipes (id, recipeId, title, ingredients, savedAt) "
                        + "VALUES (3, 'fav-1', 'Omelette', 'eggs\nmilk', 99)");

        assertEquals("Omelette", text(db, "SELECT title FROM favorite_recipes WHERE id = 3"));
        assertEquals(2, count(db,
                "SELECT COUNT(*) FROM favorite_recipe_ingredients WHERE favoriteId = 3"));
        assertEquals("milk", text(db,
                "SELECT normalizedName FROM favorite_recipe_ingredients WHERE position = 1"));
    }

    @Test
    public void migrate2To3_deletingTheMealAfterwards_cascadesToItsIngredients() throws IOException {
        SupportSQLiteDatabase db = migrate(meal(1, "milk\nrice"));

        // room turns foreign keys on when the app opens the database; this helper does not
        db.execSQL("PRAGMA foreign_keys = ON");
        db.execSQL("DELETE FROM meal_plans WHERE id = 1");

        assertEquals(0, count(db, "SELECT COUNT(*) FROM meal_plan_ingredients"));
    }

    @Test
    public void migrate2To3_otherTables_areNotTouched() throws IOException {
        SupportSQLiteDatabase db = migrate(
                "INSERT INTO storage_locations (name) VALUES ('fridge')", meal(1, "milk"));

        assertEquals(1, count(db, "SELECT COUNT(*) FROM storage_locations"));
    }
}
