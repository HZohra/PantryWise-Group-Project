package com.cp470.groupproject.data.db;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.cp470.groupproject.util.IngredientParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry of schema migrations. the schema json files are exported to {@code app/schemas/} and
 * must be committed to git, so every version can be inspected and tested.
 *
 * how to add a migration without losing user data
 *   - increase {@code version} in PantryDatabase.
 *   - add a {@code Migration} here, for example to add a "notes" column:
 *        static final Migration MIGRATION_1_2 = new Migration(1, 2) {
 *            public void migrate(SupportSQLiteDatabase db) {
 *                db.execSQL("ALTER TABLE food_items ADD COLUMN notes TEXT");
 *            }
 *        };
 *   - append it to {@link #ALL}.
 *
 * {@code fallbackToDestructiveMigration()} is intentionally not used, because it would erase the
 * household inventory and waste history on every app update.
 */

public final class Migrations {
    /** all migrations, passed to the database builder. empty while the schema is at version 1 */

    /** version 1 to 2: adds the meal_plans table. existing data is not touched. */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `meal_plans` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`planDate` INTEGER NOT NULL, "
                    + "`mealType` TEXT NOT NULL, "
                    + "`recipeId` TEXT NOT NULL, "
                    + "`recipeTitle` TEXT NOT NULL, "
                    + "`ingredients` TEXT NOT NULL, "
                    + "`completed` INTEGER NOT NULL, "
                    + "`createdAt` INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_plans_planDate` "
                    + "ON `meal_plans` (`planDate`)");
        }
    };

    /**
     * version 2 to 3: the ingredients of meals and favorite recipes move from one text column
     * (lines separated by "\n") to the tables meal_plan_ingredients and
     * favorite_recipe_ingredients. every old line is parsed once and becomes a row; blank lines
     * are dropped. the old column is removed by rebuilding the two parent tables, because sqlite
     * on api 30 cannot drop a column.
     *
     * the order matters: the old text is read first, the parents are rebuilt (keeping their ids),
     * and only then are the child tables created and filled, so no foreign key can be broken.
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            List<Object[]> meals = readOldIngredients(db, "meal_plans");
            List<Object[]> favorites = readOldIngredients(db, "favorite_recipes");

            db.execSQL("CREATE TABLE `meal_plans_new` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`planDate` INTEGER NOT NULL, "
                    + "`mealType` TEXT NOT NULL, "
                    + "`recipeId` TEXT NOT NULL, "
                    + "`recipeTitle` TEXT NOT NULL, "
                    + "`completed` INTEGER NOT NULL, "
                    + "`createdAt` INTEGER NOT NULL)");
            db.execSQL("INSERT INTO `meal_plans_new` (`id`, `planDate`, `mealType`, `recipeId`, "
                    + "`recipeTitle`, `completed`, `createdAt`) SELECT `id`, `planDate`, `mealType`, "
                    + "`recipeId`, `recipeTitle`, `completed`, `createdAt` FROM `meal_plans`");
            db.execSQL("DROP TABLE `meal_plans`");
            db.execSQL("ALTER TABLE `meal_plans_new` RENAME TO `meal_plans`");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_plans_planDate` "
                    + "ON `meal_plans` (`planDate`)");

            db.execSQL("CREATE TABLE `favorite_recipes_new` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`recipeId` TEXT NOT NULL, "
                    + "`title` TEXT NOT NULL, "
                    + "`savedAt` INTEGER NOT NULL)");
            db.execSQL("INSERT INTO `favorite_recipes_new` (`id`, `recipeId`, `title`, `savedAt`) "
                    + "SELECT `id`, `recipeId`, `title`, `savedAt` FROM `favorite_recipes`");
            db.execSQL("DROP TABLE `favorite_recipes`");
            db.execSQL("ALTER TABLE `favorite_recipes_new` RENAME TO `favorite_recipes`");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorite_recipes_recipeId` "
                    + "ON `favorite_recipes` (`recipeId`)");

            db.execSQL("CREATE TABLE IF NOT EXISTS `meal_plan_ingredients` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`mealPlanId` INTEGER NOT NULL, "
                    + "`position` INTEGER NOT NULL, "
                    + "`text` TEXT NOT NULL, "
                    + "`normalizedName` TEXT NOT NULL, "
                    + "`quantity` REAL NOT NULL, "
                    + "`unit` TEXT NOT NULL, "
                    + "`explicitQuantity` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`mealPlanId`) REFERENCES `meal_plans`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_plan_ingredients_mealPlanId` "
                    + "ON `meal_plan_ingredients` (`mealPlanId`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `favorite_recipe_ingredients` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`favoriteId` INTEGER NOT NULL, "
                    + "`position` INTEGER NOT NULL, "
                    + "`text` TEXT NOT NULL, "
                    + "`normalizedName` TEXT NOT NULL, "
                    + "`quantity` REAL NOT NULL, "
                    + "`unit` TEXT NOT NULL, "
                    + "`explicitQuantity` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`favoriteId`) REFERENCES `favorite_recipes`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS "
                    + "`index_favorite_recipe_ingredients_favoriteId` "
                    + "ON `favorite_recipe_ingredients` (`favoriteId`)");

            for (Object[] meal : meals) {
                insertLines(db, "meal_plan_ingredients", "mealPlanId", (Long) meal[0],
                        (String) meal[1]);
            }
            for (Object[] favorite : favorites) {
                insertLines(db, "favorite_recipe_ingredients", "favoriteId", (Long) favorite[0],
                        (String) favorite[1]);
            }
        }
    };

    /**
     * version 3 to 4: favorite recipes keep more details so they can be opened offline.
     * favorite_recipes gets optional columns (image, source, servings, time, ai tip and
     * nutrition per serving) and the new table favorite_recipe_steps holds the cooking steps.
     * existing favorites keep all their data; the new columns start empty (null), so no table
     * has to be rebuilt.
     */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            String[] newColumns = {
                    "`imageUrl` TEXT", "`sourceName` TEXT", "`sourceUrl` TEXT",
                    "`servings` INTEGER", "`totalTimeMinutes` INTEGER", "`tip` TEXT",
                    "`calories` REAL", "`protein` REAL", "`carbs` REAL", "`fat` REAL"
            };
            for (String column : newColumns) {
                db.execSQL("ALTER TABLE `favorite_recipes` ADD COLUMN " + column);
            }

            db.execSQL("CREATE TABLE IF NOT EXISTS `favorite_recipe_steps` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`favoriteId` INTEGER NOT NULL, "
                    + "`position` INTEGER NOT NULL, "
                    + "`text` TEXT NOT NULL, "
                    + "FOREIGN KEY(`favoriteId`) REFERENCES `favorite_recipes`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_recipe_steps_favoriteId` "
                    + "ON `favorite_recipe_steps` (`favoriteId`)");
        }
    };

    /** @return {id, old ingredients text} of every row of a table that still has the old column */
    private static List<Object[]> readOldIngredients(SupportSQLiteDatabase db, String table) {
        List<Object[]> rows = new ArrayList<>();
        try (Cursor cursor = db.query("SELECT `id`, `ingredients` FROM `" + table + "`")) {
            while (cursor.moveToNext()) {
                String text = cursor.isNull(1) ? "" : cursor.getString(1);
                rows.add(new Object[]{cursor.getLong(0), text});
            }
        }
        return rows;
    }

    /** splits the old text into lines and inserts one parsed row per non-blank line */
    private static void insertLines(SupportSQLiteDatabase db, String table, String ownerColumn,
                                    long ownerId, String oldText) {
        int position = 0;
        for (String raw : oldText.split("\n")) {
            String text = raw.trim();
            if (text.isEmpty()) {
                continue;
            }
            IngredientParser.Parsed parsed = IngredientParser.parse(text);
            Object[] values = parsed == null
                    ? new Object[]{ownerId, position, text, "", 0.0, "", 0}
                    : new Object[]{ownerId, position, text, parsed.normalizedName,
                    parsed.quantity, parsed.unit, parsed.explicitQuantity ? 1 : 0};
            db.execSQL("INSERT INTO `" + table + "` (`" + ownerColumn + "`, `position`, `text`, "
                    + "`normalizedName`, `quantity`, `unit`, `explicitQuantity`) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)", values);
            position++;
        }
    }

    public static final Migration[] ALL = new Migration[] {
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4 };
    private Migrations() { }
}