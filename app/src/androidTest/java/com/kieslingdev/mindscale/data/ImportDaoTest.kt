package com.kieslingdev.mindscale.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented coverage for the Phase 12 import transactions frozen in
 * `docs/specs/SPEC-import-restore.md`, D-1, D-3, D-4, and D-10.
 *
 * Coverage note: of the six post-mutation checks, the row-count, open-sleep, unique-index,
 * and conflict-recheck branches are exercised below. The two canonical-row checks
 * (`settingsRowCount()`/`profileRowCount()` equal to 1) are defensive only — the schema
 * seeds exactly one `id = 0` row in each table and nothing in the import path deletes it,
 * so there is no supported way to force those branches from a valid database. Do not read
 * their presence here as evidence that they are tested.
 */
@RunWith(AndroidJUnit4::class)
class ImportDaoTest {
    private lateinit var database: MindScaleDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindScaleDatabase::class.java
        ).addCallback(MindScaleDatabase.seedSettingsCallback).build()
    }

    @After
    fun closeDatabase() = database.close()

    private fun dao() = database.dataControlDao()

    /** Seeds a populated database with prior data unrelated to the incoming payload ids. */
    private suspend fun seedPriorData() {
        database.entryDao().insert(Entry(ts = 100, value = 3))
        database.entryDao().insert(Entry(ts = 200, value = 4))
        database.sleepDao().insert(SleepInterval(startTs = 300, endTs = 400))
        database.markerDao().insert(Marker(ts = 500, text = "prior event"))
        database.trackSettingsDao().setAnchors("prior low", "prior mid", "prior high")
        database.profileDao().setDisplayName("Prior")
        database.profileDao().insertScore(
            ExternalScore(
                instrument = ExternalInstrument.PHQ_8,
                total = 5,
                assessedEpochDay = 10,
                enteredAt = 111
            )
        )
    }

    private fun backupPayload(
        entries: List<Entry> = listOf(Entry(id = 101, ts = 1_000, value = 6)),
        sleeps: List<SleepInterval> = listOf(SleepInterval(id = 102, startTs = 2_000, endTs = 3_000)),
        markers: List<Marker> = listOf(Marker(id = 103, ts = 4_000, text = "restored event")),
        externalScores: List<ExternalScore> = listOf(
            ExternalScore(
                id = 104,
                instrument = ExternalInstrument.GAD_7,
                total = 9,
                assessedEpochDay = 20,
                enteredAt = 5_000
            )
        ),
        settings: TrackSettings = TrackSettings(id = 0, anchor2 = "restored anchor", themeMode = ThemeMode.DARK),
        profile: UserProfile = UserProfile(id = 0, displayName = "Restored Name"),
        breathingSessions: List<BreathingSession> = emptyList()
    ) = BackupPayload(
        version = 5,
        exportedAt = java.time.Instant.parse("2026-08-04T00:00:00Z"),
        entries = entries,
        sleeps = sleeps,
        markers = markers,
        settings = settings,
        profile = profile,
        externalScores = externalScores,
        breathingSessions = breathingSessions
    )

    // ---- 1: replaceEverything on a populated database ----

    @Test
    fun replaceEverything_onPopulatedDatabase_deletesPriorDataAndInsertsFileRowsVerbatim() = runBlocking {
        seedPriorData()
        val payload = backupPayload()

        val counts = dao().replaceEverything(payload)

        assertEquals(ImportCounts(entries = 1, sleeps = 1, markers = 1, externalScores = 1), counts)

        val snapshot = dao().snapshot()
        assertEquals(listOf(101L), snapshot.entries.map(Entry::id))
        assertEquals(listOf(102L), snapshot.sleeps.map(SleepInterval::id))
        assertEquals(listOf(103L), snapshot.markers.map(Marker::id))
        assertEquals(listOf(104L), snapshot.externalScores.map(ExternalScore::id))
        assertEquals(0, snapshot.settings.id)
        assertEquals("restored anchor", snapshot.settings.anchor2)
        assertEquals(ThemeMode.DARK, snapshot.settings.themeMode)
        assertEquals(0, snapshot.profile.id)
        assertEquals("Restored Name", snapshot.profile.displayName)
    }

    // ---- 2: replaceEverything on an empty database ----

    @Test
    fun replaceEverything_onEmptyDatabase_insertsFileRowsVerbatim() = runBlocking {
        val payload = backupPayload()

        val counts = dao().replaceEverything(payload)

        assertEquals(ImportCounts(entries = 1, sleeps = 1, markers = 1, externalScores = 1), counts)
        val snapshot = dao().snapshot()
        assertEquals(listOf(101L), snapshot.entries.map(Entry::id))
        assertEquals(listOf(102L), snapshot.sleeps.map(SleepInterval::id))
        assertEquals(listOf(103L), snapshot.markers.map(Marker::id))
        assertEquals(listOf(104L), snapshot.externalScores.map(ExternalScore::id))
        assertEquals("Restored Name", snapshot.profile.displayName)
    }

    // ---- 3: addRecords appends only ----

    @Test
    fun addRecords_appendsOnly_leavesPriorRowsSettingsProfileAndExternalScoresUntouched() = runBlocking {
        seedPriorData()
        val before = dao().snapshot()
        val payload = RecordsPayload(
            entries = listOf(Entry(ts = 9_000, value = 2)),
            sleeps = listOf(SleepInterval(startTs = 10_000, endTs = 11_000)),
            markers = listOf(Marker(ts = 12_000, text = "added event"))
        )

        val counts = dao().addRecords(payload, recheck = { true })

        assertEquals(ImportCounts(entries = 1, sleeps = 1, markers = 1, externalScores = 0), counts)
        val after = dao().snapshot()
        assertEquals(before.entries.size + 1, after.entries.size)
        assertEquals(before.sleeps.size + 1, after.sleeps.size)
        assertEquals(before.markers.size + 1, after.markers.size)
        assertTrue(after.entries.any { it.ts == 100L && it.value == 3 })
        assertTrue(after.entries.any { it.ts == 200L && it.value == 4 })
        val newEntry = after.entries.single { it.ts == 9_000L }
        assertTrue(newEntry.id > 0)
        assertEquals(before.settings, after.settings)
        assertEquals(before.profile, after.profile)
        assertEquals(before.externalScores, after.externalScores)
    }

    // ---- 4a: addRecords rollback on recheck failure ----

    @Test
    fun addRecords_recheckFails_throwsAndLeavesEveryTableByteIdentical() = runBlocking {
        seedPriorData()
        val before = dao().snapshot()
        val payload = RecordsPayload(
            entries = listOf(Entry(ts = 9_000, value = 2)),
            sleeps = emptyList(),
            markers = emptyList()
        )

        try {
            dao().addRecords(payload, recheck = { false })
            fail("expected ImportConflictException")
        } catch (expected: ImportConflictException) {
            // expected
        }

        assertEquals(before, dao().snapshot())
    }

    // ---- 4b: replaceEverything rollback on duplicate (instrument, assessedEpochDay) ----

    @Test
    fun replaceEverything_duplicateInstrumentAndDate_throwsAndRollsBack() = runBlocking {
        seedPriorData()
        val before = dao().snapshot()
        val payload = backupPayload(
            externalScores = listOf(
                ExternalScore(
                    id = 201,
                    instrument = ExternalInstrument.PHQ_8,
                    total = 4,
                    assessedEpochDay = 30,
                    enteredAt = 1_000
                ),
                ExternalScore(
                    id = 202,
                    instrument = ExternalInstrument.PHQ_8,
                    total = 8,
                    assessedEpochDay = 30,
                    enteredAt = 2_000
                )
            )
        )

        try {
            dao().replaceEverything(payload)
            fail("expected an exception for the unique index violation")
        } catch (expected: Throwable) {
            // expected: the unique index must be caught, not crash the process.
        }

        assertEquals(before, dao().snapshot())
    }

    // ---- 4c: addRecords rollback on two open sleep intervals ----

    @Test
    fun addRecords_wouldLeaveTwoOpenSleepIntervals_throwsAndRollsBack() = runBlocking {
        database.sleepDao().insert(SleepInterval(startTs = 600, endTs = null))
        val before = dao().snapshot()
        val payload = RecordsPayload(
            entries = emptyList(),
            sleeps = listOf(SleepInterval(startTs = 700, endTs = null)),
            markers = emptyList()
        )

        try {
            dao().addRecords(payload, recheck = { true })
            fail("expected an exception for a second open sleep interval")
        } catch (expected: Throwable) {
            // expected
        }

        assertEquals(before, dao().snapshot())
    }

    // ---- 5: post-replace invariants ----

    // ---- Phase 14: breathing sessions travel with both import paths ----

    /**
     * A restore replaces sessions with the file's, ids verbatim, exactly as it does every
     * other record (`docs/specs/SPEC-paced-breathing.md`, D-9, Invariant 8).
     */
    @Test
    fun replaceEverything_replacesBreathingSessionsWithTheFilesRowsVerbatim() = runBlocking {
        seedPriorData()
        database.breathingSessionDao().insert(BreathingSession(startedAt = 10, endedAt = 70))
        assertEquals(1, dao().breathingSessionCount())

        val counts = dao().replaceEverything(
            backupPayload(
                breathingSessions = listOf(
                    BreathingSession(id = 105, startedAt = 6_000, endedAt = 66_000),
                    BreathingSession(id = 106, startedAt = 70_000, endedAt = 70_000)
                )
            )
        )

        assertEquals(2, counts.breathingSessions)
        val snapshot = dao().snapshot()
        assertEquals(listOf(106L, 105L), snapshot.breathingSessions.map(BreathingSession::id))
        assertEquals(
            listOf(0L, 60_000L),
            snapshot.breathingSessions.map { it.endedAt - it.startedAt }
        )
    }

    /** A pre-version-7 backup carries none, so a restore leaves the device with none. */
    @Test
    fun replaceEverything_withNoBreathingSessions_clearsTheStoredOnes() = runBlocking {
        database.breathingSessionDao().insert(BreathingSession(startedAt = 10, endedAt = 70))

        val counts = dao().replaceEverything(backupPayload())

        assertEquals(0, counts.breathingSessions)
        assertEquals(0, dao().breathingSessionCount())
    }

    @Test
    fun addRecords_appendsBreathingSessionsAndLeavesEverythingElseAlone() = runBlocking {
        seedPriorData()
        database.breathingSessionDao().insert(BreathingSession(startedAt = 10, endedAt = 70))
        val before = dao().snapshot()

        val counts = dao().addRecords(
            RecordsPayload(
                entries = emptyList(),
                sleeps = emptyList(),
                markers = emptyList(),
                breathingSessions = listOf(BreathingSession(startedAt = 80_000, endedAt = 140_000))
            )
        ) { true }

        assertEquals(1, counts.breathingSessions)
        val after = dao().snapshot()
        assertEquals(2, after.breathingSessions.size)
        assertEquals(before.entries, after.entries)
        assertEquals(before.sleeps, after.sleeps)
        assertEquals(before.markers, after.markers)
        assertEquals(before.settings, after.settings)
        assertEquals(before.profile, after.profile)
        assertEquals(before.externalScores, after.externalScores)
        assertEquals(before.safetyPlan, after.safetyPlan)
    }

    @Test
    fun addRecords_recheckFails_leavesBreathingSessionsUntouched() = runBlocking {
        database.breathingSessionDao().insert(BreathingSession(startedAt = 10, endedAt = 70))

        runCatching {
            dao().addRecords(
                RecordsPayload(
                    entries = emptyList(),
                    sleeps = emptyList(),
                    markers = emptyList(),
                    breathingSessions = listOf(BreathingSession(startedAt = 80_000, endedAt = 140_000))
                )
            ) { false }
        }

        assertEquals(1, dao().breathingSessionCount())
    }

    @Test
    fun eraseEverything_deletesBreathingSessionsAndReportsTheCount() = runBlocking {
        seedPriorData()
        database.breathingSessionDao().insert(BreathingSession(startedAt = 10, endedAt = 70))
        database.breathingSessionDao().insert(BreathingSession(startedAt = 80, endedAt = 140))

        val counts = dao().eraseEverythingAndResetSettings()

        assertEquals(2, counts.breathingSessions)
        assertEquals(0, dao().breathingSessionCount())
        // The setting returns to its default rather than being left where the user had it.
        assertEquals(TrackSettings().breathingOn, dao().settings().breathingOn)
    }

    /** The record snapshot the CSV conflict re-check reads must include sessions. */
    @Test
    fun recordSnapshot_carriesTheStoredBreathingSessions() = runBlocking {
        database.breathingSessionDao().insert(BreathingSession(startedAt = 10, endedAt = 70))
        assertEquals(1, dao().recordSnapshot().breathingSessions.size)
    }

    @Test
    fun afterReplaceEverything_snapshotHasExactlyOneSettingsAndProfileRow_andAtMostOneOpenSleep() = runBlocking {
        seedPriorData()
        dao().replaceEverything(backupPayload())

        val snapshot = dao().snapshot()
        assertEquals(1, dao().settingsRowCount())
        assertEquals(1, dao().profileRowCount())
        assertTrue(dao().openSleepCount() <= 1)
        // snapshot() itself surfaces exactly one settings/profile row (non-list fields).
        assertEquals(0, snapshot.settings.id)
        assertEquals(0, snapshot.profile.id)
    }
}
