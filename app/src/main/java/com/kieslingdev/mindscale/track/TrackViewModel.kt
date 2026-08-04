package com.kieslingdev.mindscale.track

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryDao
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.EpisodeSourceDao
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.MarkerDao
import com.kieslingdev.mindscale.data.SleepCaptureOutcome
import com.kieslingdev.mindscale.data.SleepDao
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.TrackSettingsDao
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val READOUT_DURATION_MILLIS = 3_000L
private const val TOAST_DURATION_MILLIS = 2_200L
private const val CHECKIN_MIN_ENTRIES = 40
private const val CHECKIN_COOLDOWN_MILLIS = 60L * 24 * 60 * 60 * 1000

private const val INVALID_TIMESTAMP_ERROR = "Use yyyy-MM-dd and HH:mm."
private const val FUTURE_TIMESTAMP_ERROR = "Timestamp cannot be in the future."
private const val RESTORE_FAILED = "The unfinished dialog could not be restored."
private const val RECORD_CHECK_FAILED = "Could not check that record. Please try again."
private const val ENTRY_SAVE_FAILED = "Could not save that entry. Please try again."
private const val ENTRY_PARTIAL_SLEEP_FAILED =
    "Your rating was saved, but sleep tracking could not be updated."
private const val EDIT_FAILED = "Could not update that rating. Please try again."
private const val NOTE_FAILED = "Could not save that note. Please try again."
private const val DELETE_FAILED = "Could not delete that record. Please try again."
private const val MISSING_RECORD = "That record no longer exists"
private const val SLEEP_INTRO_TOAST = "Marks time asleep — nothing is counted while you sleep"
private const val SLEEP_ARMED_TOAST = "Now tap how you felt going to sleep"
private const val WAKE_ARMED_TOAST = "Now tap how you feel waking up"
private const val NO_SLEEP_OPEN_TOAST = "No sleep was open"
private const val MARKER_SAVED_TOAST = "Event marked"

private const val MARKER_OPEN_KEY = "track.markerOpen"
private const val MARKER_DRAFT_KEY = "track.markerDraft"

private const val DIALOG_VERSION_KEY = "track.dialog.version"
private const val DIALOG_KIND_KEY = "track.dialog.kind"
private const val BACKDATE_VALUE_KEY = "track.dialog.backdate.value"
private const val BACKDATE_DATE_KEY = "track.dialog.backdate.dateText"
private const val BACKDATE_TIME_KEY = "track.dialog.backdate.timeText"
private const val BACKDATE_CAPTURE_KIND_KEY = "track.dialog.backdate.captureKind"
private const val EDIT_ID_KEY = "track.dialog.edit.entryId"
private const val EDIT_BASELINE_TIMESTAMP_KEY = "track.dialog.edit.baselineTimestamp"
private const val EDIT_BASELINE_VALUE_KEY = "track.dialog.edit.baselineValue"
private const val EDIT_BASELINE_CHIPS_KEY = "track.dialog.edit.baselineChips"
private const val EDIT_VALUE_KEY = "track.dialog.edit.value"
private const val EDIT_DATE_KEY = "track.dialog.edit.dateText"
private const val EDIT_TIME_KEY = "track.dialog.edit.timeText"
private const val EDIT_CHIPS_KEY = "track.dialog.edit.chips"
private const val NOTE_ID_KEY = "track.dialog.note.entryId"
private const val NOTE_BASELINE_TEXT_KEY = "track.dialog.note.baselineText"
private const val NOTE_TEXT_KEY = "track.dialog.note.text"
private const val DIALOG_VERSION = 1

private val DraftDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val DraftTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val TwelveHourTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val TwentyFourHourTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private data class RestoredModal(
    val modal: TrackModalState?,
    val malformed: Boolean = false
)

class TrackViewModel(
    private val entryDao: EntryDao,
    private val sleepDao: SleepDao,
    private val markerDao: MarkerDao,
    private val settingsDao: TrackSettingsDao,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val nowProvider: () -> Long = System::currentTimeMillis,
    private val episodeSourceDao: EpisodeSourceDao? = null,
    private val zoneProvider: () -> ZoneId = ZoneId::systemDefault
) : ViewModel() {

    private val restoredModal = restoreSavedModal()
    private val _uiState = MutableStateFlow(
        TrackUiState(
            activeModal = restoredModal.modal,
            armedCapture = (restoredModal.modal as? TrackModalState.Backdate)?.draft?.captureKind,
            markerOpen = savedStateHandle[MARKER_OPEN_KEY] ?: false,
            markerDraft = savedStateHandle[MARKER_DRAFT_KEY] ?: "",
            toast = if (restoredModal.malformed) RESTORE_FAILED else null
        )
    )
    val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

    private var readoutClearJob: Job? = null
    private var toastClearJob: Job? = null
    private var recordValidationJob: Job? = null

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
                            showCheckin = computeShowCheckin(settings, count),
                            settings = settings,
                            showAnchorPrompt = count >= 15 && !settings.anchorPromptDone &&
                                settings.anchor2.isBlank() && settings.anchor5.isBlank() &&
                                settings.anchor8.isBlank()
                        )
                    }
                }
        }
        viewModelScope.launch { refreshOpenSleepInterval() }
        if (restoredModal.modal is TrackModalState.Edit || restoredModal.modal is TrackModalState.Note) {
            startRecordValidation()
        }
    }

    fun onEvent(event: TrackEvent) {
        when (event) {
            is TrackEvent.KeyTapped -> handleKeyTapped(event.value)
            is TrackEvent.KeyLongPressed -> handleKeyLongPressed(event.value)
            is TrackEvent.BackdateDateTextChanged -> updateBackdateDate(event.text)
            is TrackEvent.BackdateTimeTextChanged -> updateBackdateTime(event.text)
            is TrackEvent.BackdateTimestampChanged -> updateBackdateTimestamp(event.timestampMillis)
            TrackEvent.BackdateSaveConfirmed -> handleBackdateSaveConfirmed()
            TrackEvent.BackdateCancelled -> cancelActiveModal(TrackModalState.Backdate::class.java)
            is TrackEvent.EditRequested -> handleEditRequested(event.entry)
            is TrackEvent.EditValueChanged -> handleEditValueChanged(event.value)
            is TrackEvent.EditDateTextChanged -> updateEditDate(event.text)
            is TrackEvent.EditTimeTextChanged -> updateEditTime(event.text)
            is TrackEvent.EditTimestampChanged -> updateEditTimestamp(event.timestampMillis)
            TrackEvent.EditSaveConfirmed -> handleEditSaveConfirmed()
            TrackEvent.EditCancelled -> cancelActiveModal(TrackModalState.Edit::class.java)
            is TrackEvent.NoteRequested -> handleNoteRequested(event.entry)
            is TrackEvent.NoteTextChanged -> handleNoteTextChanged(event.text)
            TrackEvent.NoteSaveConfirmed -> handleNoteSaveConfirmed()
            TrackEvent.NoteCancelled -> cancelActiveModal(TrackModalState.Note::class.java)
            is TrackEvent.DeleteRequested -> handleDeleteRequested(event.entry)
            TrackEvent.DeleteConfirmed -> handleDeleteConfirmed()
            TrackEvent.DeleteCancelled -> cancelActiveModal(TrackModalState.Delete::class.java)
            TrackEvent.DialogValidationRetry -> retryRecordValidation()
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
            TrackEvent.AnchorPromptDone -> viewModelScope.launch { settingsDao.setAnchorPromptDone(true) }
        }
    }

    private fun computeShowCheckin(settings: TrackSettings, totalEntryCount: Int): Boolean =
        !settings.paused && totalEntryCount >= CHECKIN_MIN_ENTRIES &&
            (nowProvider() - settings.checkinAt) > CHECKIN_COOLDOWN_MILLIS

    private fun handleKeyTapped(value: Int) {
        require(value in 0..10) { "value out of range 0..10: $value" }
        if (_uiState.value.activeModal != null) return
        val now = nowProvider()
        val armed = _uiState.value.armedCapture
        val expiresAt = now + READOUT_DURATION_MILLIS
        _uiState.update {
            it.copy(
                transientReadout = ReadoutState(value, band(value), expiresAt, anchorFor(value, it.settings)),
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
                } else state
            }
        }
        viewModelScope.launch { performCapture(value, now, armed) }
    }

    private fun handleKeyLongPressed(value: Int) {
        require(value in 0..10) { "value out of range 0..10: $value" }
        if (_uiState.value.activeModal != null) return
        val now = nowProvider()
        val draft = BackdateDraft(
            value = value,
            dateText = formatDraftDate(now),
            timeText = formatDraftTime(now),
            captureKind = _uiState.value.armedCapture
        )
        setActiveModal(TrackModalState.Backdate(draft, timestampError = timestampError(draft)))
    }

    private fun updateBackdateDate(text: String) {
        if (text.length > 10) return
        val modal = _uiState.value.activeModal as? TrackModalState.Backdate ?: return
        if (modal.isSaving) return
        val draft = modal.draft.copy(dateText = text)
        replaceActiveModal(modal.copy(draft = draft, timestampError = timestampError(draft), mutationError = null))
    }

    private fun updateBackdateTime(text: String) {
        if (text.length > 5) return
        val modal = _uiState.value.activeModal as? TrackModalState.Backdate ?: return
        if (modal.isSaving) return
        val draft = modal.draft.copy(timeText = text)
        replaceActiveModal(modal.copy(draft = draft, timestampError = timestampError(draft), mutationError = null))
    }

    private fun updateBackdateTimestamp(timestampMillis: Long) {
        val modal = _uiState.value.activeModal as? TrackModalState.Backdate ?: return
        if (modal.isSaving) return
        val draft = modal.draft.copy(
            dateText = formatDraftDate(timestampMillis),
            timeText = formatDraftTime(timestampMillis)
        )
        replaceActiveModal(modal.copy(draft = draft, timestampError = timestampError(draft), mutationError = null))
    }

    private fun handleBackdateSaveConfirmed() {
        val modal = _uiState.value.activeModal as? TrackModalState.Backdate ?: return
        if (modal.isSaving || modal.timestampError != null) return
        val timestamp = parseTimestamp(modal.draft.dateText, modal.draft.timeText) ?: return
        val saving = modal.copy(isSaving = true, mutationError = null)
        replaceActiveModal(saving, persist = false)
        viewModelScope.launch {
            if (modal.draft.captureKind == null) {
                try {
                    performOrdinaryCapture(modal.draft.value, timestamp)
                    finishBackdateSuccess(saving)
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    retainBackdateFailure(saving, ENTRY_SAVE_FAILED)
                }
                return@launch
            }

            try {
                entryDao.insert(
                    Entry(ts = timestamp, value = modal.draft.value, kind = modal.draft.captureKind)
                )
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                retainBackdateFailure(saving, ENTRY_SAVE_FAILED)
                return@launch
            }

            finishBackdateSuccess(saving)
            try {
                performArmedSideEffect(modal.draft.value, timestamp, modal.draft.captureKind)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                setToast(ENTRY_PARTIAL_SLEEP_FAILED)
                refreshOpenSleepInterval()
            }
        }
    }

    private fun finishBackdateSuccess(submitted: TrackModalState.Backdate) {
        val current = _uiState.value.activeModal as? TrackModalState.Backdate ?: return
        if (current.draft != submitted.draft) return
        clearActiveModal()
        _uiState.update { it.copy(helpOpen = false, armedCapture = null) }
    }

    private fun retainBackdateFailure(submitted: TrackModalState.Backdate, message: String) {
        val current = _uiState.value.activeModal as? TrackModalState.Backdate ?: return
        if (current.draft == submitted.draft) {
            replaceActiveModal(current.copy(isSaving = false, mutationError = message), persist = false)
        }
    }

    private suspend fun performCapture(value: Int, ts: Long, armed: EntryKind?) {
        if (armed == null) {
            performOrdinaryCapture(value, ts)
        } else {
            entryDao.insert(Entry(ts = ts, value = value, kind = armed))
            performArmedSideEffect(value, ts, armed)
        }
    }

    private suspend fun performOrdinaryCapture(value: Int, ts: Long) {
        if (episodeSourceDao != null) {
            val result = episodeSourceDao.insertOrdinaryAndClassify(Entry(ts = ts, value = value))
            when {
                !result.settingsAvailable -> setToast("Settings are unavailable. Your rating was saved.")
                !result.classificationAvailable ->
                    setToast("Your rating was saved, but the onset prompt is unavailable.")
                result.promptEnabled -> _uiState.update {
                    it.copy(onsetChipPrompt = OnsetChipPromptState(entryId = result.entryId))
                }
            }
            return
        }

        val priorEntry = entryDao.mostRecentAtOrBefore(ts)
        val isOnset = value > 0 && (priorEntry == null || priorEntry.value == 0)
        val insertedId = entryDao.insert(Entry(ts = ts, value = value))
        val settings = settingsDao.observe().first()
        if (settings.askChips && isOnset) {
            _uiState.update { it.copy(onsetChipPrompt = OnsetChipPromptState(insertedId)) }
        }
    }

    private suspend fun performArmedSideEffect(value: Int, ts: Long, armed: EntryKind) {
        when (armed) {
            EntryKind.SLEEP -> when (val outcome = sleepDao.captureSleep(ts)) {
                is SleepCaptureOutcome.Opened -> setToast("Asleep at $value")
                is SleepCaptureOutcome.AlreadyOpen ->
                    setToast("Already asleep since ${formatTime(outcome.since)}")
                else -> Unit
            }

            EntryKind.WAKE -> when (val outcome = sleepDao.captureWake(ts)) {
                is SleepCaptureOutcome.Closed ->
                    setToast("Slept ${formatDuration(outcome.until - outcome.since)}")
                is SleepCaptureOutcome.NothingOpen -> setToast(NO_SLEEP_OPEN_TOAST)
                else -> Unit
            }
        }
        refreshOpenSleepInterval()
    }

    private suspend fun refreshOpenSleepInterval() {
        _uiState.update { it.copy(openSleepInterval = sleepDao.openInterval()) }
    }

    private fun handleArm(kind: EntryKind, armedToast: String) {
        if (_uiState.value.activeModal != null) return
        val current = _uiState.value.armedCapture
        if (current == kind) {
            _uiState.update { it.copy(armedCapture = null) }
            return
        }
        _uiState.update { it.copy(armedCapture = kind) }
        viewModelScope.launch {
            val settings = settingsDao.observe().first()
            if (!settings.sleepIntroShown) {
                settingsDao.setSleepIntroShown(true)
                setToast(SLEEP_INTRO_TOAST)
            } else setToast(armedToast)
        }
    }

    private fun handleEditRequested(entry: Entry) {
        if (_uiState.value.activeModal != null) return
        val draft = EditEntryDraft(
            entryId = entry.id,
            baselineTimestampMillis = entry.ts,
            baselineValue = entry.value,
            baselineChips = entry.chips.toList(),
            value = entry.value,
            dateText = formatDraftDate(entry.ts),
            timeText = formatDraftTime(entry.ts),
            chips = entry.chips.toList()
        )
        setActiveModal(
            TrackModalState.Edit(
                draft = draft,
                validation = RecordValidation.Current,
                timestampError = timestampError(draft.dateText, draft.timeText)
            )
        )
        startRecordValidation()
    }

    private fun handleEditValueChanged(value: Int) {
        require(value in 0..10)
        val modal = _uiState.value.activeModal as? TrackModalState.Edit ?: return
        if (modal.isSaving) return
        replaceActiveModal(modal.copy(draft = modal.draft.copy(value = value), mutationError = null))
    }

    private fun updateEditDate(text: String) {
        if (text.length > 10) return
        val modal = _uiState.value.activeModal as? TrackModalState.Edit ?: return
        if (modal.isSaving) return
        val draft = modal.draft.copy(dateText = text)
        replaceActiveModal(
            modal.copy(draft = draft, timestampError = timestampError(text, draft.timeText), mutationError = null)
        )
    }

    private fun updateEditTime(text: String) {
        if (text.length > 5) return
        val modal = _uiState.value.activeModal as? TrackModalState.Edit ?: return
        if (modal.isSaving) return
        val draft = modal.draft.copy(timeText = text)
        replaceActiveModal(
            modal.copy(draft = draft, timestampError = timestampError(draft.dateText, text), mutationError = null)
        )
    }

    private fun updateEditTimestamp(timestampMillis: Long) {
        val modal = _uiState.value.activeModal as? TrackModalState.Edit ?: return
        if (modal.isSaving) return
        val draft = modal.draft.copy(
            dateText = formatDraftDate(timestampMillis),
            timeText = formatDraftTime(timestampMillis)
        )
        replaceActiveModal(
            modal.copy(
                draft = draft,
                timestampError = timestampError(draft.dateText, draft.timeText),
                mutationError = null
            )
        )
    }

    private fun handleEditChipToggled(chip: String) {
        val modal = _uiState.value.activeModal as? TrackModalState.Edit ?: return
        if (modal.isSaving) return
        val chips = if (chip in modal.draft.chips) {
            modal.draft.chips - chip
        } else {
            modal.draft.chips + chip
        }
        replaceActiveModal(modal.copy(draft = modal.draft.copy(chips = chips), mutationError = null))
    }

    private fun handleEditSaveConfirmed() {
        val modal = _uiState.value.activeModal as? TrackModalState.Edit ?: return
        if (modal.isSaving || modal.timestampError != null ||
            modal.validation == RecordValidation.Checking || modal.validation == RecordValidation.ReadFailed
        ) return
        val timestamp = parseTimestamp(modal.draft.dateText, modal.draft.timeText) ?: return
        val saving = modal.copy(isSaving = true, mutationError = null)
        replaceActiveModal(saving, persist = false)
        viewModelScope.launch {
            try {
                val changed = entryDao.updateEditableFields(
                    modal.draft.entryId,
                    timestamp,
                    modal.draft.value,
                    modal.draft.chips
                )
                if (changed == 0) {
                    clearActiveModalIfDraft(modal.draft)
                    setToast(MISSING_RECORD)
                } else {
                    clearActiveModalIfDraft(modal.draft)
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                val current = _uiState.value.activeModal as? TrackModalState.Edit
                if (current?.draft == modal.draft) {
                    replaceActiveModal(current.copy(isSaving = false, mutationError = EDIT_FAILED), persist = false)
                }
            }
        }
    }

    private fun handleNoteRequested(entry: Entry) {
        if (_uiState.value.activeModal != null) return
        val draft = NoteEntryDraft(entry.id, entry.note.orEmpty(), entry.note.orEmpty())
        setActiveModal(TrackModalState.Note(draft, validation = RecordValidation.Current))
        startRecordValidation()
    }

    private fun handleNoteTextChanged(text: String) {
        val modal = _uiState.value.activeModal as? TrackModalState.Note ?: return
        if (modal.isSaving) return
        replaceActiveModal(modal.copy(draft = modal.draft.copy(text = text), mutationError = null))
    }

    private fun handleNoteSaveConfirmed() {
        val modal = _uiState.value.activeModal as? TrackModalState.Note ?: return
        if (modal.isSaving || modal.validation == RecordValidation.Checking ||
            modal.validation == RecordValidation.ReadFailed
        ) return
        val saving = modal.copy(isSaving = true, mutationError = null)
        replaceActiveModal(saving, persist = false)
        viewModelScope.launch {
            try {
                val changed = entryDao.updateNote(
                    modal.draft.entryId,
                    modal.draft.text.ifBlank { null }
                )
                if (changed == 0) {
                    clearActiveModalIfDraft(modal.draft)
                    setToast(MISSING_RECORD)
                } else {
                    clearActiveModalIfDraft(modal.draft)
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                val current = _uiState.value.activeModal as? TrackModalState.Note
                if (current?.draft == modal.draft) {
                    replaceActiveModal(current.copy(isSaving = false, mutationError = NOTE_FAILED), persist = false)
                }
            }
        }
    }

    private fun handleDeleteRequested(entry: Entry) {
        if (_uiState.value.activeModal != null) return
        setActiveModal(TrackModalState.Delete(entry), persist = false)
    }

    private fun handleDeleteConfirmed() {
        val modal = _uiState.value.activeModal as? TrackModalState.Delete ?: return
        if (modal.isSaving) return
        val saving = modal.copy(isSaving = true, mutationError = null)
        replaceActiveModal(saving, persist = false)
        viewModelScope.launch {
            try {
                val changed = entryDao.deleteById(modal.entry.id)
                val current = _uiState.value.activeModal as? TrackModalState.Delete
                if (current?.entry?.id != modal.entry.id) return@launch
                clearActiveModal()
                if (changed == 0) setToast(MISSING_RECORD)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                val current = _uiState.value.activeModal as? TrackModalState.Delete
                if (current?.entry?.id == modal.entry.id) {
                    replaceActiveModal(current.copy(isSaving = false, mutationError = DELETE_FAILED), persist = false)
                }
            }
        }
    }

    private fun startRecordValidation() {
        recordValidationJob?.cancel()
        val modal = _uiState.value.activeModal
        val id = when (modal) {
            is TrackModalState.Edit -> modal.draft.entryId
            is TrackModalState.Note -> modal.draft.entryId
            else -> return
        }
        recordValidationJob = viewModelScope.launch {
            try {
                entryDao.observeById(id).collect { entry -> handleValidatedEntry(id, entry) }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                when (val current = _uiState.value.activeModal) {
                    is TrackModalState.Edit -> if (current.draft.entryId == id) {
                        replaceActiveModal(
                            current.copy(validation = RecordValidation.ReadFailed, mutationError = RECORD_CHECK_FAILED),
                            persist = false
                        )
                    }
                    is TrackModalState.Note -> if (current.draft.entryId == id) {
                        replaceActiveModal(
                            current.copy(validation = RecordValidation.ReadFailed, mutationError = RECORD_CHECK_FAILED),
                            persist = false
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun handleValidatedEntry(id: Long, entry: Entry?) {
        val current = _uiState.value.activeModal
        if (entry == null) {
            val matches = when (current) {
                is TrackModalState.Edit -> current.draft.entryId == id
                is TrackModalState.Note -> current.draft.entryId == id
                else -> false
            }
            if (matches) {
                clearActiveModal()
                setToast(MISSING_RECORD)
            }
            return
        }
        when (current) {
            is TrackModalState.Edit -> if (current.draft.entryId == id) {
                val conflicts = entry.ts != current.draft.baselineTimestampMillis ||
                    entry.value != current.draft.baselineValue || entry.chips != current.draft.baselineChips
                replaceActiveModal(
                    current.copy(
                        validation = if (conflicts) RecordValidation.Conflicting else RecordValidation.Current,
                        mutationError = if (current.mutationError == RECORD_CHECK_FAILED) null else current.mutationError
                    ),
                    persist = false
                )
            }
            is TrackModalState.Note -> if (current.draft.entryId == id) {
                val conflicts = entry.note.orEmpty() != current.draft.baselineText
                replaceActiveModal(
                    current.copy(
                        validation = if (conflicts) RecordValidation.Conflicting else RecordValidation.Current,
                        mutationError = if (current.mutationError == RECORD_CHECK_FAILED) null else current.mutationError
                    ),
                    persist = false
                )
            }
            else -> Unit
        }
    }

    private fun retryRecordValidation() {
        when (val modal = _uiState.value.activeModal) {
            is TrackModalState.Edit -> {
                replaceActiveModal(
                    modal.copy(validation = RecordValidation.Checking, mutationError = null), persist = false
                )
                startRecordValidation()
            }
            is TrackModalState.Note -> {
                replaceActiveModal(
                    modal.copy(validation = RecordValidation.Checking, mutationError = null), persist = false
                )
                startRecordValidation()
            }
            else -> Unit
        }
    }

    private fun cancelActiveModal(type: Class<out TrackModalState>) {
        val modal = _uiState.value.activeModal ?: return
        if (!type.isInstance(modal)) return
        val saving = when (modal) {
            is TrackModalState.Backdate -> modal.isSaving
            is TrackModalState.Edit -> modal.isSaving
            is TrackModalState.Note -> modal.isSaving
            is TrackModalState.Delete -> modal.isSaving
        }
        if (!saving) clearActiveModal()
    }

    private fun setActiveModal(modal: TrackModalState, persist: Boolean = true) {
        if (_uiState.value.activeModal != null) return
        replaceActiveModal(modal, persist)
    }

    private fun replaceActiveModal(modal: TrackModalState, persist: Boolean = true) {
        if (persist) persistModal(modal)
        _uiState.update { it.copy(activeModal = modal) }
    }

    private fun clearActiveModal(cancelValidation: Boolean = true) {
        if (cancelValidation) recordValidationJob?.cancel()
        recordValidationJob = null
        clearSavedDialog()
        _uiState.update { it.copy(activeModal = null) }
    }

    private fun clearActiveModalIfDraft(draft: Any) {
        val currentDraft = when (val modal = _uiState.value.activeModal) {
            is TrackModalState.Edit -> modal.draft
            is TrackModalState.Note -> modal.draft
            else -> null
        }
        if (currentDraft == draft) clearActiveModal()
    }

    private fun persistModal(modal: TrackModalState) {
        clearSavedDialog()
        when (modal) {
            is TrackModalState.Backdate -> {
                savedStateHandle[DIALOG_VERSION_KEY] = DIALOG_VERSION
                savedStateHandle[DIALOG_KIND_KEY] = "BACKDATE"
                savedStateHandle[BACKDATE_VALUE_KEY] = modal.draft.value
                savedStateHandle[BACKDATE_DATE_KEY] = modal.draft.dateText
                savedStateHandle[BACKDATE_TIME_KEY] = modal.draft.timeText
                savedStateHandle[BACKDATE_CAPTURE_KIND_KEY] = modal.draft.captureKind?.name ?: "NONE"
            }
            is TrackModalState.Edit -> {
                savedStateHandle[DIALOG_VERSION_KEY] = DIALOG_VERSION
                savedStateHandle[DIALOG_KIND_KEY] = "EDIT"
                savedStateHandle[EDIT_ID_KEY] = modal.draft.entryId
                savedStateHandle[EDIT_BASELINE_TIMESTAMP_KEY] = modal.draft.baselineTimestampMillis
                savedStateHandle[EDIT_BASELINE_VALUE_KEY] = modal.draft.baselineValue
                savedStateHandle[EDIT_BASELINE_CHIPS_KEY] = ArrayList(modal.draft.baselineChips)
                savedStateHandle[EDIT_VALUE_KEY] = modal.draft.value
                savedStateHandle[EDIT_DATE_KEY] = modal.draft.dateText
                savedStateHandle[EDIT_TIME_KEY] = modal.draft.timeText
                savedStateHandle[EDIT_CHIPS_KEY] = ArrayList(modal.draft.chips)
            }
            is TrackModalState.Note -> {
                savedStateHandle[DIALOG_VERSION_KEY] = DIALOG_VERSION
                savedStateHandle[DIALOG_KIND_KEY] = "NOTE"
                savedStateHandle[NOTE_ID_KEY] = modal.draft.entryId
                savedStateHandle[NOTE_BASELINE_TEXT_KEY] = modal.draft.baselineText
                savedStateHandle[NOTE_TEXT_KEY] = modal.draft.text
            }
            is TrackModalState.Delete -> Unit
        }
    }

    private fun restoreSavedModal(): RestoredModal {
        if (savedStateHandle.keys().none { it.startsWith("track.dialog.") }) return RestoredModal(null)
        val version = savedStateHandle.get<Any?>(DIALOG_VERSION_KEY) as? Int
        val kind = savedStateHandle.get<Any?>(DIALOG_KIND_KEY) as? String
        if (version != DIALOG_VERSION || kind == null) return malformedRestore()
        val modal = when (kind) {
            "BACKDATE" -> restoreBackdate()
            "EDIT" -> restoreEdit()
            "NOTE" -> restoreNote()
            else -> null
        } ?: return malformedRestore()
        return RestoredModal(modal)
    }

    private fun restoreBackdate(): TrackModalState.Backdate? {
        val value = savedStateHandle.get<Any?>(BACKDATE_VALUE_KEY) as? Int ?: return null
        val date = savedStateHandle.get<Any?>(BACKDATE_DATE_KEY) as? String ?: return null
        val time = savedStateHandle.get<Any?>(BACKDATE_TIME_KEY) as? String ?: return null
        val captureName = savedStateHandle.get<Any?>(BACKDATE_CAPTURE_KIND_KEY) as? String ?: return null
        if (value !in 0..10 || date.length > 10 || time.length > 5) return null
        val capture = when (captureName) {
            "NONE" -> null
            EntryKind.SLEEP.name -> EntryKind.SLEEP
            EntryKind.WAKE.name -> EntryKind.WAKE
            else -> return null
        }
        val draft = BackdateDraft(value, date, time, capture)
        return TrackModalState.Backdate(draft, timestampError = timestampError(draft))
    }

    private fun restoreEdit(): TrackModalState.Edit? {
        val id = savedStateHandle.get<Any?>(EDIT_ID_KEY) as? Long ?: return null
        val baselineTs = savedStateHandle.get<Any?>(EDIT_BASELINE_TIMESTAMP_KEY) as? Long ?: return null
        val baselineValue = savedStateHandle.get<Any?>(EDIT_BASELINE_VALUE_KEY) as? Int ?: return null
        val value = savedStateHandle.get<Any?>(EDIT_VALUE_KEY) as? Int ?: return null
        val date = savedStateHandle.get<Any?>(EDIT_DATE_KEY) as? String ?: return null
        val time = savedStateHandle.get<Any?>(EDIT_TIME_KEY) as? String ?: return null
        val baselineChips = restoredChips(EDIT_BASELINE_CHIPS_KEY) ?: return null
        val chips = restoredChips(EDIT_CHIPS_KEY) ?: return null
        if (id <= 0L || baselineValue !in 0..10 || value !in 0..10 ||
            date.length > 10 || time.length > 5
        ) return null
        val draft = EditEntryDraft(id, baselineTs, baselineValue, baselineChips, value, date, time, chips)
        return TrackModalState.Edit(
            draft,
            validation = RecordValidation.Checking,
            timestampError = timestampError(date, time)
        )
    }

    private fun restoreNote(): TrackModalState.Note? {
        val id = savedStateHandle.get<Any?>(NOTE_ID_KEY) as? Long ?: return null
        val baseline = savedStateHandle.get<Any?>(NOTE_BASELINE_TEXT_KEY) as? String ?: return null
        val text = savedStateHandle.get<Any?>(NOTE_TEXT_KEY) as? String ?: return null
        if (id <= 0L) return null
        return TrackModalState.Note(NoteEntryDraft(id, baseline, text), RecordValidation.Checking)
    }

    private fun restoredChips(key: String): List<String>? {
        val raw = savedStateHandle.get<Any?>(key) as? ArrayList<*> ?: return null
        val strings = raw.map { it as? String ?: return null }
        if (strings.any { it.isBlank() || it.codePointCount(0, it.length) > 32 }) return null
        if (strings.distinct().size != strings.size) return null
        return strings
    }

    private fun malformedRestore(): RestoredModal {
        clearSavedDialog()
        return RestoredModal(null, malformed = true)
    }

    private fun clearSavedDialog() {
        savedStateHandle.keys()
            .filter { it.startsWith("track.dialog.") }
            .forEach { key -> savedStateHandle.remove<Any?>(key) }
    }

    private fun timestampError(draft: BackdateDraft): String? =
        timestampError(draft.dateText, draft.timeText)

    private fun timestampError(dateText: String, timeText: String): String? {
        val timestamp = parseTimestamp(dateText, timeText) ?: return INVALID_TIMESTAMP_ERROR
        return if (timestamp > nowProvider()) FUTURE_TIMESTAMP_ERROR else null
    }

    private fun parseTimestamp(dateText: String, timeText: String): Long? = try {
        val date = LocalDate.parse(dateText, DraftDateFormatter)
        val time = LocalTime.parse(timeText, DraftTimeFormatter)
        LocalDateTime.of(date, time).atZone(zoneProvider()).toInstant().toEpochMilli()
    } catch (_: DateTimeException) {
        null
    }

    private fun formatDraftDate(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneProvider()).format(DraftDateFormatter)

    private fun formatDraftTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zoneProvider()).format(DraftTimeFormatter)

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
        if (_uiState.value.activeModal != null) return
        val open = !_uiState.value.markerOpen
        updateMarkerState(open = open, draft = "")
    }

    private fun updateMarkerState(
        open: Boolean = _uiState.value.markerOpen,
        draft: String = _uiState.value.markerDraft
    ) {
        savedStateHandle[MARKER_OPEN_KEY] = open
        savedStateHandle[MARKER_DRAFT_KEY] = draft
        _uiState.update { it.copy(markerOpen = open, markerDraft = draft) }
    }

    private fun handleCheckinStillUseful() {
        viewModelScope.launch { settingsDao.recordCheckin(nowProvider(), paused = false) }
    }

    private fun handleCheckinPauseRequested() {
        viewModelScope.launch { settingsDao.recordCheckin(nowProvider(), paused = true) }
    }

    private fun handleResumeTracking() {
        viewModelScope.launch { settingsDao.setPaused(false) }
    }

    private fun handleToastDismissed() {
        toastClearJob?.cancel()
        _uiState.update { it.copy(toast = null) }
    }

    private fun setToast(message: String) {
        toastClearJob?.cancel()
        _uiState.update { it.copy(toast = message) }
        toastClearJob = viewModelScope.launch {
            delay(TOAST_DURATION_MILLIS)
            _uiState.update { it.copy(toast = null) }
        }
    }

    private fun formatTime(epochMillis: Long): String {
        val formatter = if (_uiState.value.settings.hourFormat == HourFormat.TWENTY_FOUR) {
            TwentyFourHourTimeFormatter
        } else TwelveHourTimeFormatter
        return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneProvider()))
    }

    private fun anchorFor(value: Int, settings: TrackSettings): String = when (value) {
        in 1..3 -> settings.anchor2
        in 4..6 -> settings.anchor5
        in 7..10 -> settings.anchor8
        else -> ""
    }.trim()

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
