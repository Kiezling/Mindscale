package com.kieslingdev.mindscale.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryDao
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.MarkerDao
import com.kieslingdev.mindscale.data.SleepCaptureOutcome
import com.kieslingdev.mindscale.data.SleepDao
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.TrackSettingsDao
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * How long a transient tap readout (e.g. "7 - severe") stays visible before it
 * self-clears. Not specified precisely by the spec ("a few seconds"); 3000ms
 * is a reasonable implementer's call.
 */
private const val READOUT_DURATION_MILLIS = 3_000L

/** Toast auto-dismiss duration, frozen by Invariant 25 of SPEC-track-phase2-completeness.md. */
private const val TOAST_DURATION_MILLIS = 2_200L

/** Frozen thresholds, Invariant 22: at least 40 entries and a 60-day cooldown. */
private const val CHECKIN_MIN_ENTRIES = 40
private const val CHECKIN_COOLDOWN_MILLIS = 60L * 24 * 60 * 60 * 1000

/** Minimum sleep-interval duration, Invariant 20. */
private const val MIN_SLEEP_DURATION_MILLIS = 60_000L

private const val FUTURE_TIMESTAMP_ERROR = "Timestamp cannot be in the future."
private const val SLEEP_INTRO_TOAST = "Marks time asleep — nothing is counted while you sleep"
private const val SLEEP_ARMED_TOAST = "Now tap how you felt going to sleep"
private const val WAKE_ARMED_TOAST = "Now tap how you feel waking up"
private const val NO_SLEEP_OPEN_TOAST = "No sleep was open"
private const val MARKER_SAVED_TOAST = "Event marked"
private const val MARKER_OPEN_KEY = "track.markerOpen"
private const val MARKER_DRAFT_KEY = "track.markerDraft"

private val ToastTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

class TrackViewModel(
    private val entryDao: EntryDao,
    private val sleepDao: SleepDao,
    private val markerDao: MarkerDao,
    private val settingsDao: TrackSettingsDao,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val nowProvider: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TrackUiState(
            markerOpen = savedStateHandle[MARKER_OPEN_KEY] ?: false,
            markerDraft = savedStateHandle[MARKER_DRAFT_KEY] ?: ""
        )
    )
    val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

    private var readoutClearJob: Job? = null
    private var toastClearJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                entryDao.observeRecent(),
                entryDao.observeCount(),
                settingsDao.observe()
            ) { recent, count, settings -> Triple(recent, count, settings) }
                .collect { (recent, count, settings) ->
                    _uiState.update {
                        it.copy(
                            recentEntries = recent,
                            isEmpty = count == 0,
                            sleepOn = settings.sleepOn,
                            isPaused = settings.paused,
                            showCheckin = computeShowCheckin(settings, count)
                        )
                    }
                }
        }
        viewModelScope.launch { refreshOpenSleepInterval() }
    }

    private fun computeShowCheckin(settings: TrackSettings, totalEntryCount: Int): Boolean =
        !settings.paused &&
            totalEntryCount >= CHECKIN_MIN_ENTRIES &&
            (nowProvider() - settings.checkinAt) > CHECKIN_COOLDOWN_MILLIS

    fun onEvent(event: TrackEvent) {
        when (event) {
            is TrackEvent.KeyTapped -> handleKeyTapped(event.value)
            is TrackEvent.KeyLongPressed -> handleKeyLongPressed(event.value)
            is TrackEvent.BackdateTimestampChanged -> handleBackdateTimestampChanged(event.timestampMillis)
            TrackEvent.BackdateSaveConfirmed -> handleBackdateSaveConfirmed()
            TrackEvent.BackdateCancelled -> _uiState.update { it.copy(backdateDialog = null) }
            is TrackEvent.EditRequested -> handleEditRequested(event.entry)
            is TrackEvent.EditValueChanged -> handleEditValueChanged(event.value)
            is TrackEvent.EditTimestampChanged -> handleEditTimestampChanged(event.timestampMillis)
            TrackEvent.EditSaveConfirmed -> handleEditSaveConfirmed()
            TrackEvent.EditCancelled -> _uiState.update { it.copy(editDialog = null) }
            is TrackEvent.NoteRequested -> handleNoteRequested(event.entry)
            is TrackEvent.NoteTextChanged -> handleNoteTextChanged(event.text)
            TrackEvent.NoteSaveConfirmed -> handleNoteSaveConfirmed()
            TrackEvent.NoteCancelled -> _uiState.update { it.copy(noteDialog = null) }
            is TrackEvent.DeleteRequested -> _uiState.update { it.copy(pendingDelete = event.entry) }
            TrackEvent.DeleteConfirmed -> handleDeleteConfirmed()
            TrackEvent.DeleteCancelled -> _uiState.update { it.copy(pendingDelete = null) }
            TrackEvent.ReadoutDismissed -> handleReadoutDismissed()
            is TrackEvent.EditChipToggled -> handleEditChipToggled(event.chip)
            TrackEvent.ToggleHelp -> _uiState.update { it.copy(helpOpen = !it.helpOpen) }
            TrackEvent.ArmSleep -> handleArm(EntryKind.SLEEP, SLEEP_ARMED_TOAST)
            TrackEvent.ArmWake -> handleArm(EntryKind.WAKE, WAKE_ARMED_TOAST)
            is TrackEvent.OnsetChipToggled -> handleOnsetChipToggled(event.chip)
            TrackEvent.OnsetChipsSubmitted -> handleOnsetChipsSubmitted()
            TrackEvent.OnsetChipsSkipped -> handleOnsetChipsSkipped()
            TrackEvent.MarkerToggled -> handleMarkerToggled()
            is TrackEvent.MarkerDraftChanged -> updateMarkerState(draft = event.text)
            TrackEvent.MarkerSaveConfirmed -> handleMarkerSaveConfirmed()
            TrackEvent.MarkerCancelled -> updateMarkerState(open = false, draft = "")
            TrackEvent.CheckinStillUseful -> handleCheckinStillUseful()
            TrackEvent.CheckinPauseRequested -> handleCheckinPauseRequested()
            TrackEvent.ResumeTracking -> handleResumeTracking()
            TrackEvent.ToastDismissed -> handleToastDismissed()
        }
    }

    private fun handleKeyTapped(value: Int) {
        require(value in 0..10) { "value out of range 0..10: $value" }
        val now = nowProvider()
        val armed = _uiState.value.armedCapture

        val expiresAt = now + READOUT_DURATION_MILLIS
        _uiState.update {
            it.copy(
                transientReadout = ReadoutState(value, band(value), expiresAt),
                helpOpen = false,
                armedCapture = null
            )
        }
        readoutClearJob?.cancel()
        readoutClearJob = viewModelScope.launch {
            delay(READOUT_DURATION_MILLIS)
            _uiState.update { state ->
                if (state.transientReadout?.expiresAtMillis == expiresAt) {
                    state.copy(transientReadout = null)
                } else {
                    state
                }
            }
        }

        viewModelScope.launch { performCapture(value = value, ts = now, armed = armed) }
    }

    private fun handleKeyLongPressed(value: Int) {
        require(value in 0..10) { "value out of range 0..10: $value" }
        _uiState.update {
            it.copy(backdateDialog = BackdateDialogState(value = value, timestampMillis = nowProvider()))
        }
    }

    private fun handleBackdateTimestampChanged(timestampMillis: Long) {
        _uiState.update { state ->
            val dialog = state.backdateDialog ?: return@update state
            val error = if (timestampMillis > nowProvider()) FUTURE_TIMESTAMP_ERROR else null
            state.copy(backdateDialog = dialog.copy(timestampMillis = timestampMillis, error = error))
        }
    }

    private fun handleBackdateSaveConfirmed() {
        val dialog = _uiState.value.backdateDialog ?: return
        if (dialog.error != null) return
        val armed = _uiState.value.armedCapture

        _uiState.update { it.copy(backdateDialog = null, helpOpen = false, armedCapture = null) }
        viewModelScope.launch {
            performCapture(value = dialog.value, ts = dialog.timestampMillis, armed = armed)
        }
    }

    /**
     * Shared by [handleKeyTapped] and [handleBackdateSaveConfirmed] — the single capture
     * path Invariants 15-20 describe. Onset detection (against [ts], not wall-clock now)
     * runs before the insert so it never sees the entry it is about to create; the
     * Sleep/Wake side effect goes through [SleepDao]'s transactional methods, which own
     * the "at most one open interval" guarantee (Invariant 19) - [armed] here is only
     * ever a UI-captured convenience value, never the correctness mechanism.
     */
    private suspend fun performCapture(value: Int, ts: Long, armed: EntryKind?) {
        val priorEntry = if (armed == null) entryDao.mostRecentAtOrBefore(ts) else null
        val isOnset = armed == null && value > 0 && (priorEntry == null || priorEntry.value == 0)

        val insertedId = entryDao.insert(Entry(ts = ts, value = value, kind = armed))

        when (armed) {
            EntryKind.SLEEP -> {
                when (val outcome = sleepDao.captureSleep(ts)) {
                    is SleepCaptureOutcome.Opened -> setToast("Asleep at $value")
                    is SleepCaptureOutcome.AlreadyOpen ->
                        setToast("Already asleep since ${formatTime(outcome.since)}")
                    else -> Unit // captureSleep never returns Closed/NothingOpen
                }
                refreshOpenSleepInterval()
            }

            EntryKind.WAKE -> {
                when (val outcome = sleepDao.captureWake(ts)) {
                    is SleepCaptureOutcome.Closed ->
                        setToast("Slept ${formatDuration(outcome.until - outcome.since)}")
                    is SleepCaptureOutcome.NothingOpen -> setToast(NO_SLEEP_OPEN_TOAST)
                    else -> Unit // captureWake never returns Opened/AlreadyOpen
                }
                refreshOpenSleepInterval()
            }

            null -> {
                val settings = settingsDao.observe().first()
                if (settings.askChips && isOnset) {
                    _uiState.update { it.copy(onsetChipPrompt = OnsetChipPromptState(entryId = insertedId)) }
                }
            }
        }
    }

    private suspend fun refreshOpenSleepInterval() {
        _uiState.update { it.copy(openSleepInterval = sleepDao.openInterval()) }
    }

    private fun handleArm(kind: EntryKind, armedToast: String) {
        val current = _uiState.value.armedCapture
        if (current == kind) {
            _uiState.update { it.copy(armedCapture = null) }
            return
        }
        _uiState.update { it.copy(armedCapture = kind) }
        viewModelScope.launch {
            val settings = settingsDao.observe().first()
            if (!settings.sleepIntroShown) {
                settingsDao.update(settings.copy(sleepIntroShown = true))
                setToast(SLEEP_INTRO_TOAST)
            } else {
                setToast(armedToast)
            }
        }
    }

    private fun handleEditRequested(entry: Entry) {
        _uiState.update {
            it.copy(
                editDialog = EditEntryState(
                    originalEntry = entry,
                    value = entry.value,
                    timestampMillis = entry.ts,
                    chips = entry.chips.toSet()
                )
            )
        }
    }

    private fun handleEditValueChanged(value: Int) {
        require(value in 0..10) { "value out of range 0..10: $value" }
        _uiState.update { state ->
            val dialog = state.editDialog ?: return@update state
            state.copy(editDialog = dialog.copy(value = value))
        }
    }

    private fun handleEditTimestampChanged(timestampMillis: Long) {
        _uiState.update { state ->
            val dialog = state.editDialog ?: return@update state
            val error = if (timestampMillis > nowProvider()) FUTURE_TIMESTAMP_ERROR else null
            state.copy(editDialog = dialog.copy(timestampMillis = timestampMillis, error = error))
        }
    }

    private fun handleEditChipToggled(chip: String) {
        _uiState.update { state ->
            val dialog = state.editDialog ?: return@update state
            val chips = if (chip in dialog.chips) dialog.chips - chip else dialog.chips + chip
            state.copy(editDialog = dialog.copy(chips = chips))
        }
    }

    private fun handleEditSaveConfirmed() {
        val dialog = _uiState.value.editDialog ?: return
        if (dialog.error != null) return
        _uiState.update { it.copy(editDialog = null) }
        viewModelScope.launch {
            val changed = entryDao.updateEditableFields(
                id = dialog.entryId,
                ts = dialog.timestampMillis,
                value = dialog.value,
                chips = dialog.chips.toList()
            )
            if (changed == 0) setToast("That record no longer exists")
        }
    }

    private fun handleNoteRequested(entry: Entry) {
        _uiState.update {
            it.copy(noteDialog = NoteEditState(originalEntry = entry, text = entry.note.orEmpty()))
        }
    }

    private fun handleNoteTextChanged(text: String) {
        _uiState.update { state ->
            val dialog = state.noteDialog ?: return@update state
            state.copy(noteDialog = dialog.copy(text = text))
        }
    }

    private fun handleNoteSaveConfirmed() {
        val dialog = _uiState.value.noteDialog ?: return
        _uiState.update { it.copy(noteDialog = null) }
        viewModelScope.launch {
            if (entryDao.updateNote(dialog.entryId, dialog.text.ifBlank { null }) == 0) {
                setToast("That record no longer exists")
            }
        }
    }

    private fun handleDeleteConfirmed() {
        val entry = _uiState.value.pendingDelete
        if (entry != null) {
            viewModelScope.launch {
                if (entryDao.deleteById(entry.id) == 0) setToast("That record no longer exists")
            }
        }
        _uiState.update { it.copy(pendingDelete = null) }
    }

    private fun handleReadoutDismissed() {
        readoutClearJob?.cancel()
        _uiState.update { it.copy(transientReadout = null) }
    }

    private fun handleOnsetChipToggled(chip: String) {
        _uiState.update { state ->
            val prompt = state.onsetChipPrompt ?: return@update state
            val selected = if (chip in prompt.selected) prompt.selected - chip else prompt.selected + chip
            state.copy(onsetChipPrompt = prompt.copy(selected = selected))
        }
    }

    private fun handleOnsetChipsSubmitted() {
        val prompt = _uiState.value.onsetChipPrompt ?: return
        _uiState.update { it.copy(onsetChipPrompt = null) }
        viewModelScope.launch { entryDao.updateChips(prompt.entryId, prompt.selected.toList()) }
    }

    private fun handleOnsetChipsSkipped() {
        _uiState.update { it.copy(onsetChipPrompt = null) }
    }

    private fun handleMarkerSaveConfirmed() {
        val text = _uiState.value.markerDraft.trim()
        updateMarkerState(open = false, draft = "")
        if (text.isNotEmpty()) {
            val ts = nowProvider()
            viewModelScope.launch {
                markerDao.insert(Marker(ts = ts, text = text))
                setToast(MARKER_SAVED_TOAST)
            }
        }
    }

    private fun handleMarkerToggled() {
        val open = !_uiState.value.markerOpen
        updateMarkerState(open = open, draft = "")
    }

    /**
     * Marker visibility and its in-progress draft must survive true process recreation,
     * not only recomposition/configuration changes. Keep the ViewModel state and the
     * platform-backed [SavedStateHandle] in lockstep; [MarkerSection]'s rememberSaveable
     * remains the Compose text-field buffer for state restoration at the UI layer.
     */
    private fun updateMarkerState(
        open: Boolean = _uiState.value.markerOpen,
        draft: String = _uiState.value.markerDraft
    ) {
        savedStateHandle[MARKER_OPEN_KEY] = open
        savedStateHandle[MARKER_DRAFT_KEY] = draft
        _uiState.update { it.copy(markerOpen = open, markerDraft = draft) }
    }

    private fun handleCheckinStillUseful() {
        viewModelScope.launch {
            val settings = settingsDao.observe().first()
            settingsDao.update(settings.copy(checkinAt = nowProvider()))
        }
    }

    private fun handleCheckinPauseRequested() {
        viewModelScope.launch {
            val settings = settingsDao.observe().first()
            settingsDao.update(settings.copy(checkinAt = nowProvider(), paused = true))
        }
    }

    private fun handleResumeTracking() {
        viewModelScope.launch {
            val settings = settingsDao.observe().first()
            settingsDao.update(settings.copy(paused = false))
        }
    }

    private fun handleToastDismissed() {
        toastClearJob?.cancel()
        _uiState.update { it.copy(toast = null) }
    }

    /** Invariant 25: auto-dismiss after [TOAST_DURATION_MILLIS]; a new toast replaces any showing one. */
    private fun setToast(message: String) {
        toastClearJob?.cancel()
        _uiState.update { it.copy(toast = message) }
        toastClearJob = viewModelScope.launch {
            delay(TOAST_DURATION_MILLIS)
            _uiState.update { it.copy(toast = null) }
        }
    }

    private fun formatTime(epochMillis: Long): String =
        ToastTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    private fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60_000L
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours <= 0L -> "${minutes}m"
            minutes == 0L -> "${hours}h"
            else -> "${hours}h ${minutes}m"
        }
    }
}
