package com.kieslingdev.mindscale.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Entry::class, SleepInterval::class, Marker::class, TrackSettings::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(ChipsConverter::class, EntryKindConverter::class, SettingsConverters::class)
abstract class MindScaleDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun sleepDao(): SleepDao
    abstract fun markerDao(): MarkerDao
    abstract fun trackSettingsDao(): TrackSettingsDao
    abstract fun dataControlDao(): DataControlDao
    abstract fun episodeSourceDao(): EpisodeSourceDao

    companion object {
        const val NAME = "mindscale.db"

        /**
         * Seeds the canonical `id = 0` [TrackSettings] row for a brand-new database file.
         * Does not run on a version upgrade — [RoomDatabase.Callback.onCreate] only fires
         * for a database created for the first time; [MIGRATION_1_2] seeds that path
         * itself. Shared between production and in-memory test builders (see
         * `docs/specs/SPEC-track-phase2-completeness.md`, D-8 and Android compatibility).
         */
        val seedSettingsCallback: RoomDatabase.Callback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    "INSERT INTO track_settings (id, sleepOn, askChips, paused, checkinAt, sleepIntroShown, " +
                        "themeMode, hourFormat, anchor2, anchor5, anchor8, onsetChips, hideNotes, anchorPromptDone, holdDuration) " +
                        "VALUES (0, 1, 0, 0, 0, 0, 'SYSTEM', 'TWELVE', '', '', '', " +
                        "'${DEFAULT_ONSET_CHIPS.joinToString("\u001F").replace("'", "''")}', 0, 0, 'SIXTEEN')"
                )
            }
        }

        /** Manual singleton construction (no DI framework, per Invariant 11 / D-3). */
        fun build(context: Context): MindScaleDatabase =
            Room.databaseBuilder(context.applicationContext, MindScaleDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(seedSettingsCallback)
                .build()
    }
}
