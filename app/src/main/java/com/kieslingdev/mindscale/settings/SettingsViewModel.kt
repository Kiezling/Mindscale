package com.kieslingdev.mindscale.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieslingdev.mindscale.data.DataControlDao
import com.kieslingdev.mindscale.data.DEFAULT_ONSET_CHIPS
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.SleepSettingOutcome
import com.kieslingdev.mindscale.data.ThemeMode
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.TrackSettingsDao
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val A2_KEY = "settings.anchor2"
private const val A5_KEY = "settings.anchor5"
private const val A8_KEY = "settings.anchor8"
private const val CHIPS_KEY = "settings.chips"

enum class ExportKind { BACKUP, RECORDS, ERASE_BACKUP }

data class PendingDocument(
    val kind: ExportKind,
    val filename: String,
    val contents: String,
    val entryCount: Int,
    val sleepCount: Int,
    val markerCount: Int
)

data class EraseConfirmation(val entryCount: Int, val sleepCount: Int, val markerCount: Int)

data class SettingsUiState(
    val settings: TrackSettings = TrackSettings(),
    val anchorDraft: AnchorDraft = AnchorDraft(),
    val chipDraft: String = "",
    val anchorError: String? = null,
    val chipError: String? = null,
    val pendingDocument: PendingDocument? = null,
    val retryDocument: PendingDocument? = null,
    val preparingExport: Boolean = false,
    val eraseConfirmation: EraseConfirmation? = null,
    val message: String? = null,
    val readError: String? = null
)

class SettingsViewModel(
    private val settingsDao: TrackSettingsDao,
    private val dataControlDao: DataControlDao,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val nowProvider: () -> Instant = Instant::now
) : ViewModel() {

    private val restoredAnchors = listOf(A2_KEY, A5_KEY, A8_KEY).any(savedStateHandle::contains)
    private val restoredChips = savedStateHandle.contains(CHIPS_KEY)
    private var seededDrafts = false
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            anchorDraft = AnchorDraft(
                savedStateHandle[A2_KEY] ?: "",
                savedStateHandle[A5_KEY] ?: "",
                savedStateHandle[A8_KEY] ?: ""
            ),
            chipDraft = savedStateHandle[CHIPS_KEY] ?: ""
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var settingsJob: Job? = null

    init {
        startSettingsCollection()
    }

    fun retrySettingsRead() = startSettingsCollection()

    private fun startSettingsCollection() {
        settingsJob?.cancel()
        settingsJob = viewModelScope.launch {
            try {
                settingsDao.observe().collect { settings ->
                    _uiState.update { state ->
                        val seed = !seededDrafts
                        seededDrafts = true
                        state.copy(
                            settings = settings,
                            anchorDraft = if (seed && !restoredAnchors) {
                                AnchorDraft(settings.anchor2, settings.anchor5, settings.anchor8)
                            } else state.anchorDraft,
                            chipDraft = if (seed && !restoredChips) {
                                settings.onsetChips.joinToString(", ")
                            } else state.chipDraft,
                            readError = null
                        )
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(readError = "Could not read settings. Please retry.") }
            }
        }
    }

    fun setTheme(mode: ThemeMode) = mutate("Could not change appearance.") { settingsDao.setAppearance(mode) }
    fun setHourFormat(format: HourFormat) = mutate("Could not change time format.") { settingsDao.setHourFormat(format) }
    fun setHoldDuration(duration: HoldDuration) =
        mutate("Could not change the episode hold.") { settingsDao.setHoldDuration(duration) }
    fun setAskChips(enabled: Boolean) = mutate("Could not change onset prompts.") { settingsDao.setAskChips(enabled) }
    fun setHideNotes(hidden: Boolean) = mutate("Could not change note previews.") { settingsDao.setHideNotes(hidden) }
    fun setPaused(paused: Boolean) = mutate("Could not change tracking state.") { settingsDao.setPaused(paused) }

    fun setSleepOn(enabled: Boolean) {
        viewModelScope.launch {
            try {
                when (settingsDao.setSleepOnSafely(enabled)) {
                    SleepSettingOutcome.Updated -> Unit
                    SleepSettingOutcome.OpenInterval -> showMessage("Wake first so the interval has an end.")
                    SleepSettingOutcome.MissingSettings -> showMessage("Settings are unavailable. Please retry.")
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                showMessage("Could not change Sleep and Wake.")
            }
        }
    }

    fun updateAnchor2(value: String) = updateAnchors(_uiState.value.anchorDraft.copy(anchor2 = value))
    fun updateAnchor5(value: String) = updateAnchors(_uiState.value.anchorDraft.copy(anchor5 = value))
    fun updateAnchor8(value: String) = updateAnchors(_uiState.value.anchorDraft.copy(anchor8 = value))

    private fun updateAnchors(draft: AnchorDraft) {
        savedStateHandle[A2_KEY] = draft.anchor2
        savedStateHandle[A5_KEY] = draft.anchor5
        savedStateHandle[A8_KEY] = draft.anchor8
        _uiState.update { it.copy(anchorDraft = draft, anchorError = null) }
    }

    fun saveAnchors() {
        when (val result = validateAnchors(_uiState.value.anchorDraft)) {
            is ValidationResult.Invalid -> _uiState.update { it.copy(anchorError = result.message) }
            is ValidationResult.Valid -> viewModelScope.launch {
                try {
                    val draft = result.value
                    if (settingsDao.setAnchors(draft.anchor2, draft.anchor5, draft.anchor8) != 1) {
                        showMessage("Settings are unavailable. Nothing was changed.")
                    } else {
                        updateAnchors(draft)
                        _uiState.update { it.copy(anchorError = null, message = "Anchors saved") }
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    showMessage("Could not save anchors. Your draft is still here.")
                }
            }
        }
    }

    fun updateChipDraft(value: String) {
        savedStateHandle[CHIPS_KEY] = value
        _uiState.update { it.copy(chipDraft = value, chipError = null) }
    }

    fun saveOnsetWords() {
        when (val result = normalizeOnsetWords(_uiState.value.chipDraft)) {
            is ValidationResult.Invalid -> _uiState.update { it.copy(chipError = result.message) }
            is ValidationResult.Valid -> viewModelScope.launch {
                try {
                    if (settingsDao.setOnsetChips(result.value) != 1) {
                        showMessage("Settings are unavailable. Nothing was changed.")
                    } else {
                        val normalizedDraft = result.value.joinToString(", ")
                        updateChipDraft(normalizedDraft)
                        _uiState.update { it.copy(chipError = null, message = "Onset words saved") }
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    showMessage("Could not save onset words. Your draft is still here.")
                }
            }
        }
    }

    fun restoreDefaultWords() {
        updateChipDraft(DEFAULT_ONSET_CHIPS.joinToString(", "))
        viewModelScope.launch {
            try {
                if (settingsDao.setOnsetChips(DEFAULT_ONSET_CHIPS) != 1) {
                    showMessage("Settings are unavailable. Nothing was changed.")
                } else {
                    showMessage("Default onset words restored")
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                showMessage("Could not restore onset words.")
            }
        }
    }

    fun requestBackup() = prepareDocument(ExportKind.BACKUP)
    fun requestRecordsCsv() = prepareDocument(ExportKind.RECORDS)
    fun requestExportThenErase() = prepareDocument(ExportKind.ERASE_BACKUP)

    private fun prepareDocument(kind: ExportKind) {
        if (_uiState.value.preparingExport || _uiState.value.pendingDocument != null) return
        _uiState.update { it.copy(preparingExport = true, message = null) }
        viewModelScope.launch {
            try {
                val snapshot = dataControlDao.snapshot()
                val now = nowProvider()
                val contents = if (kind == ExportKind.RECORDS) {
                    encodeRecordsCsv(snapshot)
                } else {
                    encodeBackup(snapshot, now)
                }
                _uiState.update {
                    it.copy(
                        preparingExport = false,
                        pendingDocument = PendingDocument(
                            kind = kind,
                            filename = if (kind == ExportKind.RECORDS) recordsFilename(now) else backupFilename(now),
                            contents = contents,
                            entryCount = snapshot.entries.size,
                            sleepCount = snapshot.sleeps.size,
                            markerCount = snapshot.markers.size
                        ),
                        retryDocument = null
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(preparingExport = false, message = "Could not prepare the export. Please retry.") }
            }
        }
    }

    fun documentPickerCanceled() {
        _uiState.update { it.copy(pendingDocument = null) }
    }

    fun documentWriteFailed() {
        _uiState.update {
            it.copy(
                pendingDocument = null,
                retryDocument = it.pendingDocument,
                message = "Could not write that file. Please retry."
            )
        }
    }

    fun retryDocumentWrite() {
        _uiState.update { state ->
            state.retryDocument?.let { document ->
                state.copy(pendingDocument = document, retryDocument = null, message = null)
            } ?: state
        }
    }

    fun documentWriteSucceeded() {
        val document = _uiState.value.pendingDocument ?: return
        if (document.kind == ExportKind.ERASE_BACKUP) {
            _uiState.update {
                it.copy(
                    pendingDocument = null,
                    retryDocument = null,
                    eraseConfirmation = EraseConfirmation(
                        document.entryCount,
                        document.sleepCount,
                        document.markerCount
                    ),
                    message = "Backup saved"
                )
            }
        } else {
            _uiState.update { it.copy(pendingDocument = null, retryDocument = null, message = "Export saved") }
        }
    }

    fun cancelErase() = _uiState.update { it.copy(eraseConfirmation = null) }

    fun confirmErase() {
        if (_uiState.value.eraseConfirmation == null) return
        viewModelScope.launch {
            try {
                dataControlDao.eraseEverythingAndResetSettings()
                clearDraftSavedState()
                _uiState.update {
                    it.copy(
                        anchorDraft = AnchorDraft(),
                        chipDraft = defaultChipDraft(),
                        eraseConfirmation = null,
                        message = "Everything on this device was erased"
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                showMessage("Could not erase the data. Nothing was partially deleted.")
            }
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    fun markAnchorPromptDone() = mutate("Could not dismiss the anchor prompt.") {
        settingsDao.setAnchorPromptDone(true)
    }

    private fun mutate(errorMessage: String, operation: suspend () -> Int) {
        viewModelScope.launch {
            try {
                if (operation() != 1) showMessage("Settings are unavailable. Please retry.")
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                showMessage(errorMessage)
            }
        }
    }

    private fun showMessage(message: String) = _uiState.update { it.copy(message = message) }

    private fun clearDraftSavedState() {
        savedStateHandle.remove<String>(A2_KEY)
        savedStateHandle.remove<String>(A5_KEY)
        savedStateHandle.remove<String>(A8_KEY)
        savedStateHandle.remove<String>(CHIPS_KEY)
    }
}
