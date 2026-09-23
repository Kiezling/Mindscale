package com.kieslingdev.mindscale.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kieslingdev.mindscale.data.*
import com.kieslingdev.mindscale.log.*
import com.kieslingdev.mindscale.safety.*
import com.kieslingdev.mindscale.settings.*
import com.kieslingdev.mindscale.track.*
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

/** Review-only reproductions. Assertions describe desired behavior and fail at 937ab49.
 * Copy into app/src/test/java/com/kieslingdev/mindscale/review/ to execute; keep outside
 * the normal source set until a frozen repair decision authorizes regression tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewRegressionProbeTest {
    private val now = Instant.parse("2026-09-04T12:00:00Z")
    private fun snapshot(entries: List<Entry>) = DataSnapshot(
        entries, emptyList(), emptyList(), TrackSettings()
    )

    @Test fun uiSavedLongNoteMustRestoreFromOwnBackup() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val entries = FakeEntryDao()
        val vm = LogViewModel(entries, FakeSleepDao(), FakeMarkerDao(), SavedStateHandle(),
            { ZoneOffset.UTC }, { now.toEpochMilli() })
        try {
            val id = entries.insert(Entry(ts = now.toEpochMilli() - 1000, value = 4))
            runCurrent()
            vm.onEvent(LogEvent.NoteToggled(id))
            vm.onEvent(LogEvent.NoteTextChanged("x".repeat(4001)))
            vm.onEvent(LogEvent.NoteSaved)
            runCurrent()
            val saved = entries.observeRecent().first().single()
            assertEquals(4001, saved.note!!.length)
            val result = parseBackup(encodeBackup(snapshot(listOf(saved)), now), now, ZoneOffset.UTC)
            assertTrue("Own exported backup was rejected: $result", result is ParseResult.Ok)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }

    @Test fun customChipMustRoundTripThroughCsv() {
        val words = (normalizeOnsetWords("a|b") as ValidationResult.Valid).value
        val entry = Entry(id = 1, ts = now.toEpochMilli() - 1000, value = 4, chips = words)
        val result = parseRecordsCsv(encodeRecordsCsv(snapshot(listOf(entry))), now) as ParseResult.Ok
        assertEquals(words, result.value.entries.single().chips)
    }

    @Test fun differentRecordTypesMustNotCollide() {
        val payload = RecordsPayload(emptyList(), listOf(SleepInterval(startTs = 1000, endTs = 2000)),
            emptyList(), listOf(BreathingSession(startedAt = 1000, endedAt = 2000)))
        val result = checkRecordConflicts(payload, RecordSnapshot(emptyList(), emptyList(), emptyList()))
        assertTrue("Distinct record types rejected: $result", result is ParseResult.Ok)
    }

    @Test fun ratingEditMustNotPersistInvalidFutureTimestamp() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val entries = FakeEntryDao()
        val vm = LogViewModel(entries, FakeSleepDao(), FakeMarkerDao(), SavedStateHandle(),
            { ZoneOffset.UTC }, { now.toEpochMilli() })
        try {
            val id = entries.insert(Entry(ts = now.toEpochMilli() - 1000, value = 4))
            runCurrent()
            vm.onEvent(LogEvent.EditToggled(id))
            vm.onEvent(LogEvent.EditTimestampTextChanged("2027-01-01 12:00"))
            assertNotNull(vm.uiState.value.editDraft!!.error)
            vm.onEvent(LogEvent.EditValueSelected(7))
            runCurrent()
            assertTrue("Future timestamp persisted", entries.observeRecent().first().single().ts <= now.toEpochMilli())
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }

    @Test fun safetyRetryMustResumeObservation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dao = FakeSafetyPlanDao().apply { failReads = true }
        val vm = SafetyViewModel(dao, SavedStateHandle())
        try {
            runCurrent()
            assertNotNull(vm.uiState.value.readError)
            dao.failReads = false
            dao.insert(SafetyPlanItem(step = SafetyPlanStep.WARNING_SIGNS, position = 0, text = "My line"))
            vm.retry()
            runCurrent()
            assertEquals(1, vm.uiState.value.plan.values.flatten().size)
        } finally { vm.viewModelScope.cancel(); Dispatchers.resetMain() }
    }
}
