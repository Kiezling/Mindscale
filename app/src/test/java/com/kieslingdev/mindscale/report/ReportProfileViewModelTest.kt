package com.kieslingdev.mindscale.report

import androidx.lifecycle.SavedStateHandle
import com.kieslingdev.mindscale.data.DataControlDao
import com.kieslingdev.mindscale.data.DataSnapshot
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.ExternalInstrument
import com.kieslingdev.mindscale.data.ExternalScore
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.ProfileDao
import com.kieslingdev.mindscale.data.ProfileStats
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.UserProfile
import com.kieslingdev.mindscale.insights.InsightRange
import com.kieslingdev.mindscale.insights.InsightsUiState
import com.kieslingdev.mindscale.insights.deriveInsights
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-08-04T12:00:00Z")

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun impossibleTotalsAreRejectedAndValidExternalTotalKeepsProvenance() = runTest {
        val profile = FakeProfileDao()
        val vm = viewModel(profile)
        dispatcher.scheduler.runCurrent()
        vm.updateScoreDate("2026-08-04")
        vm.updateScoreTotal("25")
        vm.saveScore()
        assertEquals("PHQ-8 total must be 0–24.", vm.uiState.value.scoreTotalError)
        assertEquals(null, vm.uiState.value.scoreDateError)
        assertTrue(profile.scores.value.isEmpty())

        vm.updateScoreTotal("24")
        vm.saveScore()
        dispatcher.scheduler.runCurrent()

        val stored = profile.scores.value.single()
        assertEquals(ExternalInstrument.PHQ_8, stored.instrument)
        assertEquals(24, stored.total)
        assertEquals("EXTERNALLY_OBTAINED_USER_ENTERED", stored.provenance.name)
        assertEquals("", vm.uiState.value.scoreTotalDraft)
    }

    @Test
    fun duplicateInstrumentDateIsRejectedWithoutOverwriting() = runTest {
        val profile = FakeProfileDao()
        profile.insertScore(
            ExternalScore(
                instrument = ExternalInstrument.GAD_7,
                total = 7,
                assessedEpochDay = java.time.LocalDate.of(2026, 8, 3).toEpochDay(),
                enteredAt = 1
            )
        )
        val vm = viewModel(profile)
        dispatcher.scheduler.runCurrent()
        vm.selectInstrument(ExternalInstrument.GAD_7)
        vm.updateScoreDate("2026-08-03")
        vm.updateScoreTotal("8")
        vm.saveScore()
        dispatcher.scheduler.runCurrent()

        assertEquals(listOf(7), profile.scores.value.map(ExternalScore::total))
        assertTrue(requireNotNull(vm.uiState.value.scoreDateError).contains("already stored"))
        assertEquals(null, vm.uiState.value.scoreTotalError)
        assertEquals("8", vm.uiState.value.scoreTotalDraft)
    }

    @Test
    fun draftsAndPendingDocumentRestoreFromPrimitiveSavedState() = runTest {
        val handle = SavedStateHandle()
        val profile = FakeProfileDao()
        var vm = viewModel(profile, handle)
        dispatcher.scheduler.runCurrent()
        vm.updateNameDraft("Ada")
        vm.selectInstrument(ExternalInstrument.GAD_7)
        vm.updateScoreDate("2026-08-03")
        vm.updateScoreTotal("11")
        vm.requestSaveDocument()
        val expectedDocument = vm.uiState.value.pendingDocument
        assertNotNull(expectedDocument)

        vm = viewModel(profile, handle)
        dispatcher.scheduler.runCurrent()

        assertEquals("Ada", vm.uiState.value.nameDraft)
        assertTrue(vm.uiState.value.nameDirty)
        assertEquals(ExternalInstrument.GAD_7, vm.uiState.value.scoreInstrument)
        assertEquals("2026-08-03", vm.uiState.value.scoreDateDraft)
        assertEquals("11", vm.uiState.value.scoreTotalDraft)
        assertEquals(expectedDocument?.text, vm.uiState.value.pendingDocument?.text)
    }

    @Test
    fun concurrentNameChangeRequiresExplicitReplacement() = runTest {
        val handle = SavedStateHandle()
        val profile = FakeProfileDao()
        val vm = viewModel(profile, handle)
        dispatcher.scheduler.runCurrent()
        vm.updateNameDraft("My draft")
        profile.profile.value = UserProfile(displayName = "Other writer")
        dispatcher.scheduler.runCurrent()

        vm.saveName()
        assertTrue(vm.uiState.value.nameConflict)
        assertEquals("Other writer", profile.profile.value.displayName)

        vm.saveName(forceReplace = true)
        dispatcher.scheduler.runCurrent()
        assertEquals("My draft", profile.profile.value.displayName)
        assertFalse(vm.uiState.value.nameDirty)
    }

    @Test
    fun atomicNameWriteDetectsChangeBetweenReadAndMutation() = runTest {
        val profile = FakeProfileDao()
        val vm = viewModel(profile)
        dispatcher.scheduler.runCurrent()
        vm.updateNameDraft("My draft")
        profile.beforeConditionalUpdate = {
            profile.profile.value = UserProfile(displayName = "Racing writer")
        }

        vm.saveName()
        dispatcher.scheduler.runCurrent()

        assertTrue(vm.uiState.value.nameConflict)
        assertEquals("Racing writer", profile.profile.value.displayName)
        assertTrue(vm.uiState.value.nameDirty)
    }

    @Test
    fun retainedSaveIsExplicitWhenReportChangesAndCanBeDiscarded() = runTest {
        val profile = FakeProfileDao()
        val vm = viewModel(profile)
        dispatcher.scheduler.runCurrent()
        vm.requestSaveDocument()
        val captured = requireNotNull(vm.uiState.value.pendingDocument).text
        vm.documentPickerReturned()
        vm.documentWriteFailed()

        profile.profile.value = UserProfile(displayName = "Ada")
        dispatcher.scheduler.runCurrent()
        assertTrue(captured != requireNotNull(vm.uiState.value.report).text)

        vm.requestSaveDocument()
        assertEquals(captured, vm.uiState.value.pendingDocument?.text)
        assertTrue(requireNotNull(vm.uiState.value.message).contains("previously captured"))

        vm.discardPendingDocument()
        vm.requestSaveDocument()
        assertEquals(vm.uiState.value.report?.text, vm.uiState.value.pendingDocument?.text)
    }

    @Test
    fun completedEraseClearsAllRestorableProfileAndReportState() = runTest {
        val handle = SavedStateHandle()
        val profile = FakeProfileDao()
        val vm = viewModel(profile, handle)
        dispatcher.scheduler.runCurrent()
        vm.updateNameDraft("Sensitive draft")
        vm.updateScoreDate("2026-08-03")
        vm.updateScoreTotal("11")
        vm.requestSaveDocument()

        vm.clearAfterErase()

        assertEquals("", vm.uiState.value.nameDraft)
        assertEquals("", vm.uiState.value.scoreTotalDraft)
        assertEquals(null, vm.uiState.value.pendingDocument)
        val recreated = viewModel(profile, handle)
        assertEquals("", recreated.uiState.value.nameDraft)
        assertEquals("", recreated.uiState.value.scoreTotalDraft)
        assertEquals(null, recreated.uiState.value.pendingDocument)
    }

    private fun viewModel(
        profile: FakeProfileDao,
        handle: SavedStateHandle = SavedStateHandle()
    ): ReportProfileViewModel {
        val snapshot = deriveInsights(
            rows = emptyList(),
            hold = TrackSettings().holdDuration,
            now = now,
            zoneId = ZoneOffset.UTC,
            range = InsightRange.THIRTY_DAYS
        )
        return ReportProfileViewModel(
            profileDao = profile,
            dataControlDao = FakeDataControlDao(profile),
            insightsState = MutableStateFlow(
                InsightsUiState(
                    loading = false,
                    range = InsightRange.THIRTY_DAYS,
                    snapshot = snapshot
                )
            ),
            savedStateHandle = handle,
            nowProvider = { now },
            zoneProvider = { ZoneOffset.UTC },
            computationDispatcher = dispatcher
        )
    }
}

private class FakeProfileDao : ProfileDao {
    val profile = MutableStateFlow(UserProfile())
    val scores = MutableStateFlow<List<ExternalScore>>(emptyList())
    val stats = MutableStateFlow(ProfileStats(null, 0, 0, 0))
    private var nextId = 1L
    var beforeConditionalUpdate: (() -> Unit)? = null

    override fun observeProfile(): Flow<UserProfile> = profile
    override fun observeScores(): Flow<List<ExternalScore>> = scores
    override fun observeStats(): Flow<ProfileStats> = stats
    override suspend fun setDisplayName(displayName: String): Int {
        profile.value = UserProfile(displayName = displayName)
        return 1
    }
    override suspend fun setDisplayNameIfUnchanged(displayName: String, expectedDisplayName: String): Int {
        beforeConditionalUpdate?.also { beforeConditionalUpdate = null }?.invoke()
        if (profile.value.displayName != expectedDisplayName) return 0
        profile.value = UserProfile(displayName = displayName)
        return 1
    }
    override suspend fun scoreById(id: Long): ExternalScore? = scores.value.firstOrNull { it.id == id }
    override suspend fun scoreOnDate(instrument: ExternalInstrument, assessedEpochDay: Long): ExternalScore? =
        scores.value.firstOrNull { it.instrument == instrument && it.assessedEpochDay == assessedEpochDay }
    override suspend fun insertScore(score: ExternalScore): Long {
        val id = nextId++
        scores.value = (scores.value + score.copy(id = id))
            .sortedWith(compareByDescending<ExternalScore> { it.assessedEpochDay }.thenByDescending { it.id })
        return id
    }
    override suspend fun updateScore(score: ExternalScore): Int {
        if (scores.value.none { it.id == score.id }) return 0
        scores.value = scores.value.map { if (it.id == score.id) score else it }
        return 1
    }
    override suspend fun deleteScore(id: Long): Int {
        val old = scores.value
        scores.value = old.filterNot { it.id == id }
        return if (old.size == scores.value.size) 0 else 1
    }
}

private class FakeDataControlDao(private val profileDao: FakeProfileDao) : DataControlDao {
    override suspend fun allEntries(): List<Entry> = emptyList()
    override suspend fun allSleeps(): List<SleepInterval> = emptyList()
    override suspend fun allMarkers(): List<Marker> = emptyList()
    override suspend fun settings(): TrackSettings = TrackSettings()
    override suspend fun profile(): UserProfile = profileDao.profile.value
    override suspend fun allExternalScores(): List<ExternalScore> = profileDao.scores.value
    override suspend fun deleteEntries(): Int = 0
    override suspend fun deleteSleeps(): Int = 0
    override suspend fun deleteMarkers(): Int = 0
    override suspend fun deleteExternalScores(): Int = 0
    override suspend fun resetSettings(defaults: TrackSettings): Int = 1
    override suspend fun resetProfile(defaults: UserProfile): Int = 1
    override suspend fun insertEntries(entries: List<Entry>): List<Long> = emptyList()
    override suspend fun insertSleeps(sleeps: List<SleepInterval>): List<Long> = emptyList()
    override suspend fun insertMarkers(markers: List<Marker>): List<Long> = emptyList()
    override suspend fun insertExternalScores(scores: List<ExternalScore>): List<Long> = emptyList()
    override suspend fun openSleepCount(): Int = 0
    override suspend fun settingsRowCount(): Int = 1
    override suspend fun profileRowCount(): Int = 1
}
