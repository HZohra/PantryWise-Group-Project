package com.cp470.groupproject.data.db;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

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

    public static final Migration[] ALL = new Migration[] { MIGRATION_1_2 };
    private Migrations() { }
}