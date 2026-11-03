package com.cp470.groupproject.data.db;

import androidx.room.migration.Migration;

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
    public static final Migration[] ALL = new Migration[] { };

    private Migrations() { }
}