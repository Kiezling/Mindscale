package com.kieslingdev.mindscale.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val TEST_DB = "migration-test"

class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MindScaleDatabase::class.java
    )

    /**
     * Migration(1, 2) is additive-only (no fallbackToDestructiveMigration; see
     * SPEC-track-phase2-completeness.md, "Rollout, migration, and rollback"). This
     * confirms a real user's existing v1 data survives the upgrade unmodified, the new
     * tables exist and are empty, and the canonical track_settings row is seeded via the
     * migration's own INSERT — since RoomDatabase.Callback.onCreate never runs on an
     * upgrade, only on a brand-new database file (D-8).
     */
    @Test
    fun migrate1To2_preservesExistingEntries_createsNewTables_andSeedsSettings() {
        var db = helper.createDatabase(TEST_DB, 1)
        db.execSQL(
            "INSERT INTO entries (id, ts, value, chips, note) VALUES (1, 1000, 5, '', 'existing note')"
        )
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT id, ts, value, chips, note, kind FROM entries WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1L, c.getLong(0))
            assertEquals(1000L, c.getLong(1))
            assertEquals(5, c.getInt(2))
            assertEquals("", c.getString(3))
            assertEquals("existing note", c.getString(4))
            assertTrue("kind must read as NULL for a pre-existing row", c.isNull(5))
        }

        db.query("SELECT COUNT(*) FROM sleeps").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM markers").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }

        db.query(
            "SELECT id, sleepOn, askChips, paused, checkinAt, sleepIntroShown FROM track_settings"
        ).use { c ->
            assertTrue("Migration(1, 2) must seed the canonical settings row itself", c.moveToFirst())
            assertEquals(0, c.getInt(0))
            assertEquals(1, c.getInt(1)) // sleepOn = true
            assertEquals(0, c.getInt(2)) // askChips = false
            assertEquals(0, c.getInt(3)) // paused = false
            assertEquals(0L, c.getLong(4)) // checkinAt = 0
            assertEquals(0, c.getInt(5)) // sleepIntroShown = false
            assertTrue("exactly one settings row after migration", !c.moveToNext())
        }
    }

    @Test
    fun migrate2To3_preservesRecordsAndAddsFrozenSettingsDefaults() {
        var db = helper.createDatabase(TEST_DB, 2)
        db.execSQL("INSERT INTO entries (id, ts, value, chips, note, kind) VALUES (7, 1000, 8, 'work', 'keep me', 'SLEEP')")
        db.execSQL("INSERT INTO sleeps (id, startTs, endTs) VALUES (8, 900, 1200)")
        db.execSQL("INSERT INTO markers (id, ts, text) VALUES (9, 1100, 'dose change')")
        db.execSQL("INSERT INTO track_settings (id, sleepOn, askChips, paused, checkinAt, sleepIntroShown) VALUES (0, 0, 1, 1, 77, 1)")
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        db.query("SELECT ts, value, chips, note, kind FROM entries WHERE id = 7").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1000L, c.getLong(0))
            assertEquals(8, c.getInt(1))
            assertEquals("work", c.getString(2))
            assertEquals("keep me", c.getString(3))
            assertEquals("SLEEP", c.getString(4))
        }
        db.query("SELECT themeMode, hourFormat, anchor2, anchor5, anchor8, onsetChips, hideNotes, anchorPromptDone, sleepOn, askChips, paused FROM track_settings WHERE id = 0").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("SYSTEM", c.getString(0))
            assertEquals("TWELVE", c.getString(1))
            assertEquals("", c.getString(2))
            assertEquals("", c.getString(3))
            assertEquals("", c.getString(4))
            assertEquals(DEFAULT_ONSET_CHIPS, ChipsConverter().toChips(c.getString(5)))
            assertEquals(0, c.getInt(6))
            assertEquals(0, c.getInt(7))
            assertEquals(0, c.getInt(8))
            assertEquals(1, c.getInt(9))
            assertEquals(1, c.getInt(10))
        }
    }

    @Test
    fun migrate3To4_preservesRecordsAndSettings_andAddsSixteenHourHold() {
        var db = helper.createDatabase(TEST_DB, 3)
        db.execSQL("INSERT INTO entries (id, ts, value, chips, note, kind) VALUES (17, 1000, 6, 'work', 'keep', NULL)")
        db.execSQL("INSERT INTO sleeps (id, startTs, endTs) VALUES (18, 1100, 1200)")
        db.execSQL("INSERT INTO markers (id, ts, text) VALUES (19, 1300, 'marker')")
        db.execSQL(
            "INSERT INTO track_settings " +
                "(id, sleepOn, askChips, paused, checkinAt, sleepIntroShown, themeMode, hourFormat, " +
                "anchor2, anchor5, anchor8, onsetChips, hideNotes, anchorPromptDone) " +
                "VALUES (0, 0, 1, 1, 77, 1, 'DARK', 'TWENTY_FOUR', 'low', 'mid', 'high', 'work', 1, 1)"
        )
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        db.query("SELECT ts, value, chips, note, kind FROM entries WHERE id = 17").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1000L, c.getLong(0))
            assertEquals(6, c.getInt(1))
            assertEquals("work", c.getString(2))
            assertEquals("keep", c.getString(3))
            assertTrue(c.isNull(4))
        }
        db.query("SELECT startTs, endTs FROM sleeps WHERE id = 18").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1100L, c.getLong(0))
            assertEquals(1200L, c.getLong(1))
        }
        db.query("SELECT text FROM markers WHERE id = 19").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("marker", c.getString(0))
        }
        db.query(
            "SELECT themeMode, hourFormat, anchor2, anchor5, anchor8, onsetChips, hideNotes, " +
                "anchorPromptDone, sleepOn, askChips, paused, holdDuration FROM track_settings WHERE id = 0"
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("DARK", c.getString(0))
            assertEquals("TWENTY_FOUR", c.getString(1))
            assertEquals("low", c.getString(2))
            assertEquals("mid", c.getString(3))
            assertEquals("high", c.getString(4))
            assertEquals("work", c.getString(5))
            assertEquals(1, c.getInt(6))
            assertEquals(1, c.getInt(7))
            assertEquals(0, c.getInt(8))
            assertEquals(1, c.getInt(9))
            assertEquals(1, c.getInt(10))
            assertEquals("SIXTEEN", c.getString(11))
        }
    }

    @Test
    fun migrate1To4_runsEveryAdditiveStep() {
        var db = helper.createDatabase(TEST_DB, 1)
        db.execSQL("INSERT INTO entries (id, ts, value, chips, note) VALUES (3, 44, 5, '', NULL)")
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        db.query("SELECT value, kind FROM entries WHERE id = 3").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(5, c.getInt(0))
            assertTrue(c.isNull(1))
        }
        db.query("SELECT themeMode, onsetChips, holdDuration FROM track_settings WHERE id = 0").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("SYSTEM", c.getString(0))
            assertEquals(DEFAULT_ONSET_CHIPS, ChipsConverter().toChips(c.getString(1)))
            assertEquals("SIXTEEN", c.getString(2))
        }
    }

    @Test
    fun migrate2To4_runsRemainingAdditiveSteps() {
        var db = helper.createDatabase(TEST_DB, 2)
        db.execSQL("INSERT INTO entries (id, ts, value, chips, note, kind) VALUES (4, 55, 7, '', NULL, NULL)")
        db.execSQL("INSERT INTO track_settings (id, sleepOn, askChips, paused, checkinAt, sleepIntroShown) VALUES (0, 1, 0, 0, 0, 0)")
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_2_3, MIGRATION_3_4)
        db.query("SELECT value FROM entries WHERE id = 4").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(7, c.getInt(0))
        }
        db.query("SELECT themeMode, holdDuration FROM track_settings WHERE id = 0").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("SYSTEM", c.getString(0))
            assertEquals("SIXTEEN", c.getString(1))
        }
    }
}
