package com.kieslingdev.mindscale.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Additive-only: `entries.kind` (nullable), `sleeps`/`markers`/`track_settings` tables,
 * and an explicit seed INSERT into `track_settings`. No existing row is dropped, dropped
 * and recreated, or rewritten. `RoomDatabase.Callback.onCreate` (see
 * [MindScaleDatabase.seedSettingsCallback]) only fires for a brand-new database file, not
 * for an upgrade, so this migration seeds the canonical `id = 0` settings row itself for
 * anyone upgrading from a Phase 1 (`version = 1`) database. See
 * `docs/specs/SPEC-track-phase2-completeness.md`, "Rollout, migration, and rollback" and D-8.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE entries ADD COLUMN kind TEXT DEFAULT NULL")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS sleeps (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "startTs INTEGER NOT NULL, " +
                "endTs INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS markers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "ts INTEGER NOT NULL, " +
                "text TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS track_settings (" +
                "id INTEGER PRIMARY KEY NOT NULL, " +
                "sleepOn INTEGER NOT NULL, " +
                "askChips INTEGER NOT NULL, " +
                "paused INTEGER NOT NULL, " +
                "checkinAt INTEGER NOT NULL, " +
                "sleepIntroShown INTEGER NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO track_settings (id, sleepOn, askChips, paused, checkinAt, sleepIntroShown) " +
                "VALUES (0, 1, 0, 0, 0, 0)"
        )
    }
}
