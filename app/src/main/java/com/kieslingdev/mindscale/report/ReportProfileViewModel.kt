package com.kieslingdev.mindscale.report

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieslingdev.mindscale.data.DataControlDao
import com.kieslingdev.mindscale.data.ExternalInstrument
import com.kieslingdev.mindscale.data.ExternalScore
import com.kieslingdev.mindscale.data.ExternalScoreProvenance
import com.kieslingdev.mindscale.data.ProfileDao
import com.kieslingdev.mindscale.data.ProfileStats
import com.kieslingdev.mindscale.data.UserProfile
import com.kieslingdev.mindscale.insights.InsightsUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val NAME_DRAFT_KEY = "profile.nameDraft"
private const val NAME_BASELINE_KEY = "profile.nameBaseline"
private const val NAME_DIRTY_KEY = "profile.nameDirty"
private const val SCORE_INSTRUMENT_KEY = "profile.scoreInstrument"
private const val SCORE_DATE_KEY = "profile.scoreDate"
private const val SCORE_TOTAL_KEY = "profile.scoreTotal"
private const val SCORE_EDITING_ID_KEY = "profile.scoreEditingId"
private const val SCORE_DELETE_ID_KEY = "profile.scoreDeleteId"
private const val DOCUMENT_TEXT_KEY = "report.documentText"
private const val DOCUMENT_FILENAME_KEY = "report.documentFilename"
private const val DOCUMENT_TOKEN_KEY = "report.documentToken"

data class PendingReportDocument(
    val text: String,
    val filename: String,
    val launchToken: Long?
)

data class ReportProfileUiState(
    val loading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val stats: ProfileStats = ProfileStats(null, 0, 0, 0),
    val scores: List<ExternalScore> = emptyList(),
    val nameDraft: String = "",
    val nameDirty: Boolean = false,
    val nameConflict: Boolean = false,
    val scoreInstrument: ExternalInstrument = ExternalInstrument.PHQ_8,
    val scoreDateDraft: String = "",
    val scoreTotalDraft: String = "",
    val editingScoreId: Long? = null,
    val pendingDeleteScoreId: Long? = null,
    val scoreDateError: String? = null,
    val scoreTotalError: String? = null,
    val scoreFormError: String? = null,
    val report: ClinicianReport? = null,
    val pendingDocument: PendingReportDocument? = null,
    val message: String? = null,
    val error: String? = null
)

private data class ProfileRead(
    val profile: UserProfile,
    val scores: List<ExternalScore>,
    val stats: ProfileStats,
    val insights: InsightsUiState
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportProfileViewModel(
    private val profileDao: ProfileDao,
    private val dataControlDao: DataControlDao,
    insightsState: StateFlow<InsightsUiState>,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val nowProvider: () -> Instant = Instant::now,
    private val zoneProvider: () -> ZoneId = ZoneId::systemDefault,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    private val retryVersion = MutableStateFlow(0)
    private val mutationMutex = Mutex()
    private val _uiState = MutableStateFlow(
        ReportProfileUiState(
            nameDraft = savedStateHandle[NAME_DRAFT_KEY] ?: "",
            nameDirty = savedStateHandle[NAME_DIRTY_KEY] ?: false,
            scoreInstrument = savedStateHandle.get<String>(SCORE_INSTRUMENT_KEY)
                ?.let { runCatching { ExternalInstrument.valueOf(it) }.getOrNull() }
                ?: ExternalInstrument.PHQ_8,
            scoreDateDraft = savedStateHandle[SCORE_DATE_KEY]
                ?: nowProvider().atZone(zoneProvider()).toLocalDate().toString(),
            scoreTotalDraft = savedStateHandle[SCORE_TOTAL_KEY] ?: "",
            editingScoreId = savedStateHandle.get<Long>(SCORE_EDITING_ID_KEY)?.takeIf { it > 0 },
            pendingDeleteScoreId = savedStateHandle.get<Long>(SCORE_DELETE_ID_KEY)?.takeIf { it > 0 },
            pendingDocument = restoredDocument()
        )
    )
    val uiState: StateFlow<ReportProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            retryVersion.flatMapLatest {
                combine(
                    profileDao.observeProfile(),
                    profileDao.observeScores(),
                    profileDao.observeStats(),
                    insightsState
                ) { profile, scores, stats, insights ->
                    ProfileRead(profile, scores, stats, insights)
                }.catch { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = "Could not read Profile. Your records are still on this device."
                        )
                    }
                }
            }.collect(::publishRead)
        }
    }

    private suspend fun publishRead(read: ProfileRead) {
        val previous = _uiState.value
        val baseline = savedStateHandle.get<String>(NAME_BASELINE_KEY)
        val nameDraft = if (!previous.nameDirty) read.profile.displayName else previous.nameDraft
        val nameConflict = previous.nameDirty && baseline != null && baseline != read.profile.displayName
        if (!previous.nameDirty) {
            savedStateHandle[NAME_DRAFT_KEY] = nameDraft
            savedStateHandle[NAME_BASELINE_KEY] = read.profile.displayName
        }
        var editingId = previous.editingScoreId
        var deleteId = previous.pendingDeleteScoreId
        var staleMessage: String? = null
        if (editingId != null && read.scores.none { it.id == editingId }) {
            editingId = null
            savedStateHandle.remove<Long>(SCORE_EDITING_ID_KEY)
            staleMessage = "That stored total no longer exists. Your draft is ready to add as a new total."
        }
        if (deleteId != null && read.scores.none { it.id == deleteId }) {
            deleteId = null
            savedStateHandle.remove<Long>(SCORE_DELETE_ID_KEY)
            staleMessage = "That stored total no longer exists."
        }

        val report = read.insights.snapshot?.let { snapshot ->
            try {
                val source = dataControlDao.snapshot()
                withContext(computationDispatcher) {
                    buildClinicianReport(
                        source = source,
                        range = read.insights.range,
                        generatedAt = Instant.ofEpochMilli(snapshot.nowMillis),
                        zoneId = zoneProvider()
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                null
            }
        }
        _uiState.update {
            it.copy(
                loading = read.insights.loading,
                profile = read.profile,
                stats = read.stats,
                scores = read.scores,
                nameDraft = nameDraft,
                nameConflict = nameConflict,
                editingScoreId = editingId,
                pendingDeleteScoreId = deleteId,
                report = report,
                message = staleMessage ?: it.message,
                error = when {
                    read.insights.error != null -> "Could not build the clinician summary. Your records are still on this device."
                    read.insights.snapshot != null && report == null ->
                        "Could not build the clinician summary. Your records are still on this device."
                    else -> null
                }
            )
        }
    }

    fun retry() {
        _uiState.update { it.copy(loading = true, error = null) }
        retryVersion.update { it + 1 }
    }

    fun updateNameDraft(value: String) {
        if (value.codePointCount(0, value.length) > 80 || value.any { it == '\n' || it == '\r' || it.isISOControl() }) {
            _uiState.update { it.copy(message = "Name must be one line with at most 80 characters.") }
            return
        }
        if (!_uiState.value.nameDirty) savedStateHandle[NAME_BASELINE_KEY] = _uiState.value.profile.displayName
        savedStateHandle[NAME_DRAFT_KEY] = value
        savedStateHandle[NAME_DIRTY_KEY] = true
        _uiState.update { it.copy(nameDraft = value, nameDirty = true, nameConflict = false, message = null) }
    }

    fun saveName(forceReplace: Boolean = false) {
        val state = _uiState.value
        val normalized = state.nameDraft.trim()
        if (normalized.codePointCount(0, normalized.length) > 80 ||
            normalized.any { it == '\n' || it == '\r' || it.isISOControl() }
        ) {
            _uiState.update { it.copy(message = "Name must be one line with at most 80 characters.") }
            return
        }
        val baseline = savedStateHandle.get<String>(NAME_BASELINE_KEY) ?: state.profile.displayName
        viewModelScope.launch {
            mutationMutex.withLock {
                runCatching {
                    if (forceReplace) profileDao.setDisplayName(normalized)
                    else profileDao.setDisplayNameIfUnchanged(normalized, baseline)
                }
                    .onSuccess { count ->
                        if (count != 1) {
                            _uiState.update {
                                it.copy(
                                    nameConflict = !forceReplace,
                                    message = if (forceReplace) {
                                        "Could not save the name. Your draft is still here."
                                    } else {
                                        "The saved name changed. Choose Replace saved name to use this draft."
                                    }
                                )
                            }
                        } else {
                            savedStateHandle[NAME_DRAFT_KEY] = normalized
                            savedStateHandle[NAME_BASELINE_KEY] = normalized
                            savedStateHandle[NAME_DIRTY_KEY] = false
                            _uiState.update {
                                it.copy(
                                    nameDraft = normalized,
                                    nameDirty = false,
                                    nameConflict = false,
                                    message = "Name saved."
                                )
                            }
                        }
                    }
                    .onFailure {
                        _uiState.update { state -> state.copy(message = "Could not save the name. Your draft is still here.") }
                    }
            }
        }
    }

    fun selectInstrument(instrument: ExternalInstrument) {
        savedStateHandle[SCORE_INSTRUMENT_KEY] = instrument.name
        _uiState.update {
            it.copy(scoreInstrument = instrument, scoreDateError = null, scoreTotalError = null, scoreFormError = null)
        }
    }

    fun updateScoreDate(value: String) {
        if (value.length > 10) return
        savedStateHandle[SCORE_DATE_KEY] = value
        _uiState.update { it.copy(scoreDateDraft = value, scoreDateError = null, scoreFormError = null) }
    }

    fun updateScoreTotal(value: String) {
        if (value.length > 2 || value.any { !it.isDigit() }) return
        savedStateHandle[SCORE_TOTAL_KEY] = value
        _uiState.update { it.copy(scoreTotalDraft = value, scoreTotalError = null, scoreFormError = null) }
    }

    fun editScore(id: Long) {
        val score = _uiState.value.scores.firstOrNull { it.id == id } ?: run {
            _uiState.update { it.copy(message = "That stored total no longer exists.") }
            return
        }
        savedStateHandle[SCORE_INSTRUMENT_KEY] = score.instrument.name
        savedStateHandle[SCORE_DATE_KEY] = LocalDate.ofEpochDay(score.assessedEpochDay).toString()
        savedStateHandle[SCORE_TOTAL_KEY] = score.total.toString()
        savedStateHandle[SCORE_EDITING_ID_KEY] = score.id
        _uiState.update {
            it.copy(
                scoreInstrument = score.instrument,
                scoreDateDraft = LocalDate.ofEpochDay(score.assessedEpochDay).toString(),
                scoreTotalDraft = score.total.toString(),
                editingScoreId = score.id,
                scoreDateError = null,
                scoreTotalError = null,
                scoreFormError = null,
                message = null
            )
        }
    }

    fun cancelScoreEdit() {
        clearScoreDraft()
        _uiState.update { it.copy(message = null) }
    }

    fun saveScore() {
        val state = _uiState.value
        val date = runCatching { LocalDate.parse(state.scoreDateDraft) }.getOrNull()
        val today = nowProvider().atZone(zoneProvider()).toLocalDate()
        val total = state.scoreTotalDraft.toIntOrNull()
        val dateError = when {
            date == null -> "Enter a real assessment date as YYYY-MM-DD."
            date > today -> "Assessment date cannot be in the future."
            else -> null
        }
        val totalError = when {
            total == null -> "Enter the total shown on the external result."
            total !in 0..state.scoreInstrument.maxTotal ->
                "${state.scoreInstrument.visibleLabel} total must be 0–${state.scoreInstrument.maxTotal}."
            else -> null
        }
        if (dateError != null || totalError != null) {
            _uiState.update {
                it.copy(scoreDateError = dateError, scoreTotalError = totalError, scoreFormError = null)
            }
            return
        }
        val assessedDate = requireNotNull(date)
        val validatedTotal = requireNotNull(total)
        viewModelScope.launch {
            mutationMutex.withLock {
                try {
                    val duplicate = profileDao.scoreOnDate(state.scoreInstrument, assessedDate.toEpochDay())
                    if (duplicate != null && duplicate.id != state.editingScoreId) {
                        _uiState.update {
                            it.copy(
                                scoreDateError =
                                    "A ${state.scoreInstrument.visibleLabel} total is already stored for $assessedDate. Edit that row instead.",
                                scoreTotalError = null,
                                scoreFormError = null
                            )
                        }
                        return@withLock
                    }
                    val existing = state.editingScoreId?.let { profileDao.scoreById(it) }
                    if (state.editingScoreId != null && existing == null) {
                        _uiState.update {
                            it.copy(
                                editingScoreId = null,
                                scoreDateError = null,
                                scoreTotalError = null,
                                scoreFormError = "That stored total no longer exists. Your draft is ready to add."
                            )
                        }
                        savedStateHandle.remove<Long>(SCORE_EDITING_ID_KEY)
                        return@withLock
                    }
                    val score = ExternalScore(
                        id = existing?.id ?: 0,
                        instrument = state.scoreInstrument,
                        total = validatedTotal,
                        assessedEpochDay = assessedDate.toEpochDay(),
                        provenance = ExternalScoreProvenance.EXTERNALLY_OBTAINED_USER_ENTERED,
                        enteredAt = existing?.enteredAt ?: nowProvider().toEpochMilli()
                    )
                    if (existing == null) {
                        check(profileDao.insertScore(score) > 0) { "Score insert did not return an id" }
                    } else {
                        check(profileDao.updateScore(score) == 1) { "Score row is stale" }
                    }
                    clearScoreDraft()
                    _uiState.update {
                        it.copy(message = if (existing == null) "External total added." else "External total updated.")
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    val message = if (error is SQLiteConstraintException) {
                        "A total for that instrument and date already exists. Your draft is still here."
                    } else {
                        "Could not save the external total. Your draft is still here."
                    }
                    _uiState.update {
                        if (error is SQLiteConstraintException) {
                            it.copy(scoreDateError = message, scoreTotalError = null, scoreFormError = null)
                        } else {
                            it.copy(scoreDateError = null, scoreTotalError = null, scoreFormError = message)
                        }
                    }
                }
            }
        }
    }

    fun requestDeleteScore(id: Long) {
        if (_uiState.value.scores.none { it.id == id }) {
            _uiState.update { it.copy(message = "That stored total no longer exists.") }
            return
        }
        savedStateHandle[SCORE_DELETE_ID_KEY] = id
        _uiState.update { it.copy(pendingDeleteScoreId = id) }
    }

    fun cancelDeleteScore() {
        savedStateHandle.remove<Long>(SCORE_DELETE_ID_KEY)
        _uiState.update { it.copy(pendingDeleteScoreId = null) }
    }

    fun confirmDeleteScore() {
        val id = _uiState.value.pendingDeleteScoreId ?: return
        viewModelScope.launch {
            mutationMutex.withLock {
                runCatching { profileDao.deleteScore(id) }
                    .onSuccess { count ->
                        savedStateHandle.remove<Long>(SCORE_DELETE_ID_KEY)
                        _uiState.update {
                            it.copy(
                                pendingDeleteScoreId = null,
                                message = if (count == 1) "External total deleted." else "That stored total no longer exists."
                            )
                        }
                    }
                    .onFailure {
                        _uiState.update { state -> state.copy(message = "Could not delete the external total. Try again.") }
                    }
            }
        }
    }

    fun requestSaveDocument() {
        val report = _uiState.value.report ?: return
        val retained = _uiState.value.pendingDocument
        val token = nowProvider().toEpochMilli()
        val text = retained?.text ?: report.text
        val filename = retained?.filename ?: clinicianReportFilename(report.generatedAt)
        savedStateHandle[DOCUMENT_TEXT_KEY] = text
        savedStateHandle[DOCUMENT_FILENAME_KEY] = filename
        savedStateHandle[DOCUMENT_TOKEN_KEY] = token
        _uiState.update {
            it.copy(
                pendingDocument = PendingReportDocument(text, filename, token),
                message = if (retained != null && retained.text != report.text) {
                    "Retrying the previously captured summary. Its text may differ from the summary now shown."
                } else {
                    null
                }
            )
        }
    }

    fun documentPickerReturned() {
        savedStateHandle.remove<Long>(DOCUMENT_TOKEN_KEY)
        _uiState.update {
            it.copy(pendingDocument = it.pendingDocument?.copy(launchToken = null))
        }
    }

    fun documentWriteSucceeded() {
        savedStateHandle.remove<String>(DOCUMENT_TEXT_KEY)
        savedStateHandle.remove<String>(DOCUMENT_FILENAME_KEY)
        savedStateHandle.remove<Long>(DOCUMENT_TOKEN_KEY)
        _uiState.update { it.copy(pendingDocument = null, message = "Clinician summary saved.") }
    }

    fun documentWriteFailed() {
        _uiState.update {
            it.copy(
                message = "Could not save the clinician summary. Save again retries the same captured text; " +
                    "it may differ if the summary window or records changed."
            )
        }
    }

    fun discardPendingDocument() {
        savedStateHandle.remove<String>(DOCUMENT_TEXT_KEY)
        savedStateHandle.remove<String>(DOCUMENT_FILENAME_KEY)
        savedStateHandle.remove<Long>(DOCUMENT_TOKEN_KEY)
        _uiState.update { it.copy(pendingDocument = null, message = "Retained save discarded. Save captures the summary now shown.") }
    }

    fun reportActionFailed(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun reportActionSucceeded(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private fun clearScoreDraft() {
        val today = nowProvider().atZone(zoneProvider()).toLocalDate().toString()
        savedStateHandle[SCORE_DATE_KEY] = today
        savedStateHandle[SCORE_TOTAL_KEY] = ""
        savedStateHandle.remove<Long>(SCORE_EDITING_ID_KEY)
        _uiState.update {
            it.copy(
                scoreDateDraft = today,
                scoreTotalDraft = "",
                editingScoreId = null,
                scoreDateError = null,
                scoreTotalError = null,
                scoreFormError = null
            )
        }
    }

    fun clearAfterErase() {
        listOf(
            NAME_DRAFT_KEY,
            NAME_BASELINE_KEY,
            NAME_DIRTY_KEY,
            SCORE_INSTRUMENT_KEY,
            SCORE_DATE_KEY,
            SCORE_TOTAL_KEY,
            SCORE_EDITING_ID_KEY,
            SCORE_DELETE_ID_KEY,
            DOCUMENT_TEXT_KEY,
            DOCUMENT_FILENAME_KEY,
            DOCUMENT_TOKEN_KEY
        ).forEach { key -> savedStateHandle.remove<Any?>(key) }
        val today = nowProvider().atZone(zoneProvider()).toLocalDate().toString()
        _uiState.value = ReportProfileUiState(
            loading = true,
            nameDraft = "",
            scoreDateDraft = today,
            message = "Everything on this device was erased"
        )
        retryVersion.update { it + 1 }
    }

    private fun restoredDocument(): PendingReportDocument? {
        val text = savedStateHandle.get<String>(DOCUMENT_TEXT_KEY) ?: return null
        val filename = savedStateHandle.get<String>(DOCUMENT_FILENAME_KEY) ?: return null
        return PendingReportDocument(text, filename, savedStateHandle[DOCUMENT_TOKEN_KEY])
    }
}
