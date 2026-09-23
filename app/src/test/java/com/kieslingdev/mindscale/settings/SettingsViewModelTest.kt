package com.kieslingdev.mindscale.settings

import androidx.lifecycle.SavedStateHandle
import com.kieslingdev.mindscale.data.BreathingSession
import com.kieslingdev.mindscale.data.DataControlDao
import com.kieslingdev.mindscale.data.DataSnapshot
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.ExternalScore
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.SafetyPlanItem
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.UserProfile
import com.kieslingdev.mindscale.track.FakeTrackSettingsDao
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    /**
     * The paced-breathing toggle is on by default and is a plain targeted settings write.
     * Turning it off removes the Track link and nothing else — no stored session is
     * touched (`docs/specs/SPEC-paced-breathing.md`, D-8).
     */
    @Test
    fun pacedBreathingIsOnByDefaultAndTheToggleIsATargetedWrite() = runTest {
        val settings = FakeTrackSettingsDao()
        val data = FakeDataControlDao()
        data.breathingSessions += BreathingSession(id = 1, startedAt = 1_000, endedAt = 61_000)
        val vm = SettingsViewModel(settings, data, SavedStateHandle())
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.settings.breathingOn)

        vm.setBreathingOn(false)
        dispatcher.scheduler.runCurrent()
        assertFalse(vm.uiState.value.settings.breathingOn)
        assertEquals(1, data.breathingSessions.size)

        vm.setBreathingOn(true)
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.settings.breathingOn)
    }

    /** The erase dialog states the exact number of sessions before the user confirms. */
    @Test
    fun theEraseConfirmationDisclosesTheStoredBreathingSessionCount() = runTest {
        val data = FakeDataControlDao()
        data.breathingSessions += BreathingSession(id = 1, startedAt = 1_000, endedAt = 61_000)
        data.breathingSessions += BreathingSession(id = 2, startedAt = 70_000, endedAt = 70_000)
        val vm = SettingsViewModel(
            FakeTrackSettingsDao(),
            data,
            SavedStateHandle(),
            nowProvider = { Instant.parse("2026-08-05T12:00:00Z") },
            ioContext = dispatcher
        )
        dispatcher.scheduler.runCurrent()

        vm.requestExportThenErase()
        dispatcher.scheduler.runCurrent()
        assertEquals(2, vm.uiState.value.pendingDocument?.breathingSessionCount)
        vm.documentWriteSucceeded()
        assertEquals(2, vm.uiState.value.eraseConfirmation?.breathingSessionCount)
    }

    @Test
    fun eraseCannotBeConfirmedUntilBackupWriteSucceeds() = runTest {
        val data = FakeDataControlDao()
        var completedCallbacks = 0
        val vm = SettingsViewModel(
            FakeTrackSettingsDao(),
            data,
            SavedStateHandle(),
            nowProvider = { Instant.parse("2026-08-03T12:00:00Z") },
            onEraseCompleted = { completedCallbacks += 1 },
            ioContext = dispatcher
        )
        dispatcher.scheduler.runCurrent()

        vm.confirmErase()
        dispatcher.scheduler.runCurrent()
        assertEquals(0, data.eraseCalls)

        vm.requestExportThenErase()
        dispatcher.scheduler.runCurrent()
        assertNotNull(vm.uiState.value.pendingDocument)
        assertNull(vm.uiState.value.eraseConfirmation)

        vm.documentPickerCanceled()
        vm.confirmErase()
        dispatcher.scheduler.runCurrent()
        assertEquals(0, data.eraseCalls)

        vm.requestExportThenErase()
        dispatcher.scheduler.runCurrent()
        vm.documentWriteSucceeded()
        assertNotNull(vm.uiState.value.eraseConfirmation)

        vm.confirmErase()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, data.eraseCalls)
        assertEquals(1, completedCallbacks)
        assertEquals(Instant.parse("2026-08-03T12:00:00Z").toEpochMilli(), vm.uiState.value.eraseRevision)
        assertNull(vm.uiState.value.pendingDocument)
        assertNull(vm.uiState.value.retryDocument)
        assertNull(vm.uiState.value.eraseConfirmation)
    }

    /** R-1: export-first erase never offers confirmation for an unrestorable legacy snapshot. */
    @Test
    fun erasePreflightRefusesOversizedLegacyNoteButKeepsOrdinaryExportAvailable() = runTest {
        val data = FakeDataControlDao()
        data.entries[0] = data.entries[0].copy(note = "x".repeat(4_001))
        val vm = SettingsViewModel(
            FakeTrackSettingsDao(), data, nowProvider = { Instant.parse("2026-08-03T12:00:00Z") },
            ioContext = dispatcher
        )
        dispatcher.scheduler.runCurrent()

        vm.requestExportThenErase()
        dispatcher.scheduler.runCurrent()
        assertNull(vm.uiState.value.pendingDocument)
        assertTrue(vm.uiState.value.message!!.contains("restorable"))

        vm.requestBackup()
        dispatcher.scheduler.runCurrent()
        assertNotNull(vm.uiState.value.pendingDocument)
        assertTrue(vm.uiState.value.pendingDocument!!.warning!!.contains("cannot restore"))
    }

    /** R-1: the bounded collection limit also blocks erase confirmation. */
    @Test
    fun erasePreflightRefusesMoreThanTwentyThousandRecords() = runTest {
        val data = FakeDataControlDao()
        data.entries.clear()
        data.entries += (1..20_001).map { id ->
            Entry(id = id.toLong(), ts = id.toLong(), value = 4, note = "x")
        }
        val vm = SettingsViewModel(
            FakeTrackSettingsDao(), data, nowProvider = { Instant.parse("2026-08-03T12:00:00Z") },
            ioContext = dispatcher
        )
        dispatcher.scheduler.runCurrent()

        vm.requestExportThenErase()
        dispatcher.scheduler.runCurrent()
        assertNull(vm.uiState.value.pendingDocument)
        assertTrue(vm.uiState.value.message!!.contains("restorable"))
    }

    /** R-1: byte-size refusal is exercised independently of the collection and field limits. */
    @Test
    fun erasePreflightRefusesEncodedBackupOverEightMiB() = runTest {
        val data = FakeDataControlDao()
        data.entries.clear()
        data.entries += (1..3_000).map { id ->
            Entry(id = id.toLong(), ts = id.toLong(), value = 4, note = "x".repeat(3_000))
        }
        val exported = encodeBackup(
            DataSnapshot(data.entries, data.sleeps, data.markers, data.settings, data.profile),
            Instant.parse("2026-08-03T12:00:00Z")
        )
        assertTrue(exported.toByteArray(Charsets.UTF_8).size > MAX_IMPORT_BYTES)
        val vm = SettingsViewModel(
            FakeTrackSettingsDao(), data, nowProvider = { Instant.parse("2026-08-03T12:00:00Z") },
            ioContext = dispatcher
        )
        dispatcher.scheduler.runCurrent()
        vm.requestExportThenErase()
        dispatcher.scheduler.runCurrent()
        assertNull(vm.uiState.value.pendingDocument)
        assertNull(vm.uiState.value.eraseConfirmation)
        assertTrue(vm.uiState.value.message!!.contains("restorable"))
    }

    /** R-1: a failed erase-backup write retries the same protected snapshot. */
    @Test
    fun eraseBackupRetryRetainsSnapshotUntilConfirmation() = runTest {
        val data = FakeDataControlDao()
        val vm = SettingsViewModel(
            FakeTrackSettingsDao(), data, nowProvider = { Instant.parse("2026-08-03T12:00:00Z") },
            ioContext = dispatcher
        )
        dispatcher.scheduler.runCurrent()
        vm.requestExportThenErase()
        dispatcher.scheduler.runCurrent()
        val original = vm.uiState.value.pendingDocument
        vm.documentWriteFailed()
        assertEquals(original, vm.uiState.value.retryDocument)
        vm.retryDocumentWrite()
        assertEquals(original, vm.uiState.value.pendingDocument)
        vm.documentWriteSucceeded()
        assertNotNull(vm.uiState.value.eraseConfirmation)
        vm.confirmErase()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, data.eraseCalls)
    }

    /** R-1: a changed database and a duplicate confirmation cannot erase new data. */
    @Test
    fun staleAndDuplicateEraseConfirmationsAreSafe() = runTest {
        val data = FakeDataControlDao()
        val vm = SettingsViewModel(
            FakeTrackSettingsDao(), data, nowProvider = { Instant.parse("2026-08-03T12:00:00Z") },
            ioContext = dispatcher
        )
        dispatcher.scheduler.runCurrent()
        vm.requestExportThenErase()
        dispatcher.scheduler.runCurrent()
        vm.documentWriteSucceeded()
        data.entries += Entry(ts = 9_000, value = 5)
        vm.confirmErase()
        vm.confirmErase()
        dispatcher.scheduler.runCurrent()
        assertEquals(0, data.eraseCalls)
        assertTrue(vm.uiState.value.message!!.contains("changed"))
    }

    @Test
    fun anchorAndChipDraftsSurviveViewModelRecreation() = runTest {
        val handle = SavedStateHandle()
        val settings = FakeTrackSettingsDao()
        val data = FakeDataControlDao()
        var vm = SettingsViewModel(settings, data, handle)
        dispatcher.scheduler.runCurrent()
        vm.updateAnchor2("can't get moving")
        vm.updateChipDraft("work, poor sleep")

        vm = SettingsViewModel(settings, data, handle)
        dispatcher.scheduler.runCurrent()

        assertEquals("can't get moving", vm.uiState.value.anchorDraft.anchor2)
        assertEquals("work, poor sleep", vm.uiState.value.chipDraft)
    }

    @Test
    fun failedWriteRetainsEncodedDocumentForRetry() = runTest {
        val vm = SettingsViewModel(
            FakeTrackSettingsDao(),
            FakeDataControlDao(),
            nowProvider = { Instant.parse("2026-08-03T12:00:00Z") },
            ioContext = dispatcher
        )
        dispatcher.scheduler.runCurrent()
        vm.requestBackup()
        dispatcher.scheduler.runCurrent()
        val original = vm.uiState.value.pendingDocument

        vm.documentWriteFailed()
        assertNull(vm.uiState.value.pendingDocument)
        assertEquals(original, vm.uiState.value.retryDocument)

        vm.retryDocumentWrite()
        assertEquals(original, vm.uiState.value.pendingDocument)
        assertNull(vm.uiState.value.retryDocument)
    }

    @Test
    fun backupPickerRequestIsOneShotAndCancellationKeepsTheSnapshot() = runTest {
        val data = FakeDataControlDao()
        val vm = SettingsViewModel(
            FakeTrackSettingsDao(), data,
            nowProvider = { Instant.parse("2026-08-03T12:00:00Z") },
            ioContext = dispatcher
        )
        dispatcher.scheduler.runCurrent()
        val before = data.snapshot()

        vm.requestBackup()
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.documentLaunchPending)
        assertEquals(ExportKind.BACKUP, vm.uiState.value.pendingDocument?.kind)

        vm.documentLaunchHandled()
        assertFalse(vm.uiState.value.documentLaunchPending)
        assertNotNull(vm.uiState.value.pendingDocument)

        vm.documentPickerCanceled()
        assertNull(vm.uiState.value.pendingDocument)
        assertFalse(vm.uiState.value.documentLaunchPending)
        assertEquals(before, data.snapshot())
    }

    @Test
    fun holdDurationUsesTargetedWriteAndUpdatesObservedState() = runTest {
        val settings = FakeTrackSettingsDao()
        val vm = SettingsViewModel(settings, FakeDataControlDao())
        dispatcher.scheduler.runCurrent()

        vm.setHoldDuration(HoldDuration.TWENTY_FOUR)
        dispatcher.scheduler.runCurrent()

        assertEquals(HoldDuration.TWENTY_FOUR, vm.uiState.value.settings.holdDuration)
        assertEquals(HoldDuration.TWENTY_FOUR, settings.updateCalls.single().holdDuration)
    }
}

/**
 * In-memory stand-in for the record tables. The `@Transaction` default methods on
 * [DataControlDao] are inherited unchanged, so the import transaction body and all of its
 * post-mutation checks are exercised here exactly as production runs them.
 */
internal class FakeDataControlDao : DataControlDao {
    val entries = mutableListOf(Entry(id = 1, ts = 1_000, value = 4))
    val sleeps = mutableListOf(SleepInterval(id = 2, startTs = 2_000, endTs = 3_000))
    val markers = mutableListOf(Marker(id = 3, ts = 4_000, text = "event"))
    var settings = TrackSettings()
    var profile = UserProfile()
    val externalScores = mutableListOf<ExternalScore>()
    val safetyPlan = mutableListOf<SafetyPlanItem>()
    val breathingSessions = mutableListOf<BreathingSession>()
    var eraseCalls = 0
    var failInsert = false
    private var nextId = 100L

    /** Mirrors Room: a zero id is generated, a non-zero id is inserted verbatim. */
    private fun generated(id: Long): Long = if (id == 0L) ++nextId else id

    override suspend fun insertEntries(entries: List<Entry>): List<Long> =
        insert(entries, this.entries) { it.copy(id = generated(it.id)) }

    override suspend fun insertSleeps(sleeps: List<SleepInterval>): List<Long> =
        insert(sleeps, this.sleeps) { it.copy(id = generated(it.id)) }

    override suspend fun insertMarkers(markers: List<Marker>): List<Long> =
        insert(markers, this.markers) { it.copy(id = generated(it.id)) }

    override suspend fun insertExternalScores(scores: List<ExternalScore>): List<Long> =
        insert(scores, this.externalScores) { it.copy(id = generated(it.id)) }

    override suspend fun insertSafetyPlanItems(items: List<SafetyPlanItem>): List<Long> =
        insert(items, this.safetyPlan) { it.copy(id = generated(it.id)) }

    override suspend fun insertBreathingSessions(sessions: List<BreathingSession>): List<Long> =
        insert(sessions, this.breathingSessions) { it.copy(id = generated(it.id)) }

    private fun <T> insert(incoming: List<T>, into: MutableList<T>, assign: (T) -> T): List<Long> {
        if (failInsert && incoming.isNotEmpty()) error("insert failed")
        return incoming.map { row ->
            val stored = assign(row)
            into += stored
            when (stored) {
                is Entry -> stored.id
                is SleepInterval -> stored.id
                is Marker -> stored.id
                is ExternalScore -> stored.id
                is SafetyPlanItem -> stored.id
                is BreathingSession -> stored.id
                else -> 0L
            }
        }
    }

    override suspend fun openSleepCount(): Int = sleeps.count { it.endTs == null }
    override suspend fun settingsRowCount(): Int = 1
    override suspend fun profileRowCount(): Int = 1

    override suspend fun allEntries(): List<Entry> = entries.toList()
    override suspend fun allSleeps(): List<SleepInterval> = sleeps.toList()
    override suspend fun allMarkers(): List<Marker> = markers.toList()
    override suspend fun settings(): TrackSettings = settings
    override suspend fun profile(): UserProfile = profile
    override suspend fun allExternalScores(): List<ExternalScore> = externalScores.toList()
    override suspend fun allSafetyPlanItems(): List<SafetyPlanItem> =
        safetyPlan.sortedWith(compareBy({ it.position }, { it.id }))
    override suspend fun safetyPlanItemCount(): Int = safetyPlan.size
    override suspend fun allBreathingSessions(): List<BreathingSession> =
        breathingSessions.sortedWith(compareByDescending<BreathingSession> { it.startedAt }.thenByDescending { it.id })
    override suspend fun breathingSessionCount(): Int = breathingSessions.size
    override suspend fun deleteEntries(): Int = entries.size.also { entries.clear() }
    override suspend fun deleteSleeps(): Int = sleeps.size.also { sleeps.clear() }
    override suspend fun deleteMarkers(): Int = markers.size.also { markers.clear() }
    override suspend fun deleteExternalScores(): Int =
        externalScores.size.also { externalScores.clear() }
    override suspend fun deleteSafetyPlanItems(): Int =
        safetyPlan.size.also { safetyPlan.clear() }
    override suspend fun deleteBreathingSessions(): Int =
        breathingSessions.size.also { breathingSessions.clear() }
    override suspend fun resetSettings(defaults: TrackSettings): Int {
        eraseCalls += 1
        settings = defaults
        return 1
    }

    override suspend fun resetProfile(defaults: UserProfile): Int {
        profile = defaults
        return 1
    }
}
