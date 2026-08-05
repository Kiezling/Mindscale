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

/** Phase 4 additive-only settings expansion. Existing records and columns are untouched. */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val encodedDefaults = DEFAULT_ONSET_CHIPS.joinToString("\u001F").replace("'", "''")
        db.execSQL("ALTER TABLE track_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'SYSTEM'")
        db.execSQL("ALTER TABLE track_settings ADD COLUMN hourFormat TEXT NOT NULL DEFAULT 'TWELVE'")
        db.execSQL("ALTER TABLE track_settings ADD COLUMN anchor2 TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE track_settings ADD COLUMN anchor5 TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE track_settings ADD COLUMN anchor8 TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE track_settings ADD COLUMN onsetChips TEXT NOT NULL DEFAULT '$encodedDefaults'")
        db.execSQL("ALTER TABLE track_settings ADD COLUMN hideNotes INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE track_settings ADD COLUMN anchorPromptDone INTEGER NOT NULL DEFAULT 0")
    }
}

/** Phase 5 additive-only setting for the awake-time episode hold. */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE track_settings ADD COLUMN holdDuration TEXT NOT NULL DEFAULT 'SIXTEEN'"
        )
    }
}

/** Phase 11 additive-only Profile and externally obtained assessment totals. */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS user_profile (" +
                "id INTEGER PRIMARY KEY NOT NULL, " +
                "displayName TEXT NOT NULL)"
        )
        db.execSQL("INSERT INTO user_profile (id, displayName) VALUES (0, '')")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS external_scores (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "instrument TEXT NOT NULL, " +
                "total INTEGER NOT NULL, " +
                "assessedEpochDay INTEGER NOT NULL, " +
                "provenance TEXT NOT NULL, " +
                "enteredAt INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "index_external_scores_instrument_assessedEpochDay " +
                "ON external_scores (instrument, assessedEpochDay)"
        )
    }
}

/**
 * Phase 13 additive-only safety plan. One new table; no existing table, column, row, or
 * index is altered, so a 5→6 upgrade cannot lose anything. There is no seed row: an empty
 * plan is the correct and expected starting state, and MindScale never pressures the user
 * to fill one in (`docs/specs/SPEC-safety-card.md`, D-9).
 */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS safety_plan_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "step TEXT NOT NULL, " +
                "position INTEGER NOT NULL, " +
                "text TEXT NOT NULL, " +
                "phone TEXT)"
        )
    }
}

/**
 * Phase 14 additive-only paced breathing: one new table and one new settings column. No
 * existing table, column, row, or index is altered, so a 6→7 upgrade cannot lose anything.
 *
 * `breathingOn` defaults to 1 for every existing row, which is the same value a fresh
 * install gets, so an upgrading user and a new user see the identical starting state
 * (`docs/specs/SPEC-paced-breathing.md`, D-8, D-9).
 */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS breathing_sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "startedAt INTEGER NOT NULL, " +
                "endedAt INTEGER NOT NULL)"
        )
        db.execSQL("ALTER TABLE track_settings ADD COLUMN breathingOn INTEGER NOT NULL DEFAULT 1")
    }
}
