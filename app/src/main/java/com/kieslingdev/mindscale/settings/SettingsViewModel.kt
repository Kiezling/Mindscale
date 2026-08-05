package com.kieslingdev.mindscale.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieslingdev.mindscale.data.BackupPayload
import com.kieslingdev.mindscale.data.DataControlDao
import com.kieslingdev.mindscale.data.DEFAULT_ONSET_CHIPS
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.ImportConflictException
import com.kieslingdev.mindscale.data.RecordsPayload
import com.kieslingdev.mindscale.data.SleepSettingOutcome
import com.kieslingdev.mindscale.data.ThemeMode
import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.TrackSettingsDao
import java.io.InputStream
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val A2_KEY = "settings.anchor2"
private const val A5_KEY = "settings.anchor5"
private const val A8_KEY = "settings.anchor8"
private const val CHIPS_KEY = "settings.chips"

// The only two import primitives that may be persisted. Raw file bytes, decoded text,
// parsed records, and preview payloads never enter saved state, which the system writes
// to disk (Phase 12, D-9).
private const val IMPORT_KIND_KEY = "settings.import.kind"
private const val IMPORT_PREVIEW_PENDING_KEY = "settings.import.previewPending"

enum class ExportKind { BACKUP, RECORDS, ERASE_BACKUP }

data class PendingDocument(
    val kind: ExportKind,
    val filename: String,
    val contents: String,
    val entryCount: Int,
    val sleepCount: Int,
    val markerCount: Int,
    val safetyPlanItemCount: Int,
    val breathingSessionCount: Int
)

data class EraseConfirmation(
    val entryCount: Int,
    val sleepCount: Int,
    val markerCount: Int,
    val safetyPlanItemCount: Int,
    val breathingSessionCount: Int
)

/** A validated, previewed import awaiting explicit confirmation (Phase 12, D-7). */
data class PendingImport(
    val kind: ImportKind,
    val payload: ImportPayload,
    val preview: ImportPreview
)

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
    val eraseRevision: Long = 0,
    val message: String? = null,
    val readError: String? = null,
    val importLaunch: ImportKind? = null,
    val importing: Boolean = false,
    val pendingImport: PendingImport? = null,
    val importError: String? = null
)

class SettingsViewModel(
    private val settingsDao: TrackSettingsDao,
    private val dataControlDao: DataControlDao,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val nowProvider: () -> Instant = Instant::now,
    private val onEraseCompleted: () -> Unit = {},
    private val zoneProvider: () -> ZoneId = ZoneId::systemDefault,
    private val onDataReplaced: () -> Unit = {},
    // Injected so reading and parsing stay off the main thread in production and stay
    // deterministic under test.
    private val ioContext: CoroutineContext = Dispatchers.IO
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
            chipDraft = savedStateHandle[CHIPS_KEY] ?: "",
            // A preview that did not survive recreation is reported, never silently
            // reinstated from a persisted URI or a re-read of the file (D-9).
            importError = if (savedStateHandle.get<Boolean>(IMPORT_PREVIEW_PENDING_KEY) == true) {
                ImportMessages.PREVIEW_LOST
            } else null
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var settingsJob: Job? = null
    private var importJob: Job? = null

    init {
        clearImportSavedState()
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

    fun setBreathingOn(enabled: Boolean) =
        mutate("Could not change paced breathing.") { settingsDao.setBreathingOn(enabled) }

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
                            markerCount = snapshot.markers.size,
                            safetyPlanItemCount = snapshot.safetyPlan.size,
                            breathingSessionCount = snapshot.breathingSessions.size
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
                        document.markerCount,
                        document.safetyPlanItemCount,
                        document.breathingSessionCount
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
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                showMessage("Could not erase the data. Nothing was partially deleted.")
                return@launch
            }
            importJob?.cancel()
            clearDraftSavedState()
            clearImportSavedState()
            _uiState.update {
                it.copy(
                    anchorDraft = AnchorDraft(),
                    chipDraft = defaultChipDraft(),
                    pendingDocument = null,
                    retryDocument = null,
                    preparingExport = false,
                    eraseConfirmation = null,
                    importLaunch = null,
                    importing = false,
                    pendingImport = null,
                    importError = null,
                    eraseRevision = maxOf(it.eraseRevision + 1, nowProvider().toEpochMilli()),
                    message = "Everything on this device was erased"
                )
            }
            runCatching(onEraseCompleted)
        }
    }

    // ---- Phase 12 import (docs/specs/SPEC-import-restore.md) ----

    fun requestBackupRestore() = startImport(ImportKind.BACKUP_RESTORE)

    fun requestRecordsImport() = startImport(ImportKind.RECORDS_MERGE)

    private fun startImport(kind: ImportKind) {
        val state = _uiState.value
        if (state.importing || state.pendingImport != null || state.importLaunch != null) return
        _uiState.update {
            it.copy(importLaunch = kind, importError = null, message = null)
        }
    }

    /** Consumes the one-shot launch signal so recreation cannot reopen the picker. */
    fun importLaunchHandled() = _uiState.update { it.copy(importLaunch = null) }

    fun importPickerCanceled() {
        clearImportSavedState()
        _uiState.update { it.copy(importLaunch = null, importing = false) }
    }

    /**
     * Reads, decodes, parses, validates, and conflict-checks the chosen file entirely off
     * the main thread, then publishes one immutable preview. Nothing here writes to Room.
     * [open] is a read-only stream opener supplied by the Compose shell; the ViewModel
     * never sees a URI, a filename, or a `ContentResolver`.
     */
    fun importFileSelected(kind: ImportKind, open: suspend () -> InputStream) {
        if (_uiState.value.importing || _uiState.value.pendingImport != null) return
        savedStateHandle[IMPORT_KIND_KEY] = kind.name
        _uiState.update {
            it.copy(importLaunch = null, importing = true, importError = null, message = null)
        }
        importJob?.cancel()
        importJob = viewModelScope.launch {
            val outcome = try {
                withContext(ioContext) { preflight(kind, open) }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                ParseResult.Rejected(ImportMessages.READ_FAILED)
            }
            when (outcome) {
                is ParseResult.Rejected -> {
                    clearImportSavedState()
                    _uiState.update { it.copy(importing = false, importError = outcome.message) }
                }
                is ParseResult.Ok -> {
                    savedStateHandle[IMPORT_PREVIEW_PENDING_KEY] = true
                    _uiState.update { it.copy(importing = false, pendingImport = outcome.value) }
                }
            }
        }
    }

    private suspend fun preflight(
        kind: ImportKind,
        open: suspend () -> InputStream
    ): ParseResult<PendingImport> {
        val text = when (val read = open().use(::readBoundedUtf8)) {
            is ParseResult.Rejected -> return read
            is ParseResult.Ok -> read.value
        }
        val now = nowProvider()
        return when (kind) {
            ImportKind.BACKUP_RESTORE -> when (val parsed = parseBackup(text, now, zoneProvider())) {
                is ParseResult.Rejected -> parsed
                is ParseResult.Ok -> pendingRestore(parsed.value)
            }
            ImportKind.RECORDS_MERGE -> when (val parsed = parseRecordsCsv(text, now)) {
                is ParseResult.Rejected -> parsed
                is ParseResult.Ok -> when (
                    val checked = checkRecordConflicts(parsed.value, dataControlDao.recordSnapshot())
                ) {
                    is ParseResult.Rejected -> checked
                    is ParseResult.Ok -> pendingMerge(checked.value)
                }
            }
        }
    }

    private suspend fun pendingRestore(backup: BackupPayload): ParseResult<PendingImport> {
        val existing = dataControlDao.recordSnapshot()
        val payload = ImportPayload.Restore(backup)
        val counts = RecordCounts(
            entries = existing.entries.size,
            sleeps = existing.sleeps.size,
            markers = existing.markers.size,
            externalScores = dataControlDao.allExternalScores().size,
            safetyPlanItems = dataControlDao.safetyPlanItemCount(),
            breathingSessions = dataControlDao.breathingSessionCount()
        )
        return ParseResult.Ok(
            PendingImport(ImportKind.BACKUP_RESTORE, payload, previewOf(payload, counts))
        )
    }

    private fun pendingMerge(records: RecordsPayload): ParseResult<PendingImport> {
        val payload = ImportPayload.Merge(records)
        return ParseResult.Ok(
            PendingImport(ImportKind.RECORDS_MERGE, payload, previewOf(payload, RecordCounts()))
        )
    }

    /**
     * Cancels a preview that has not been confirmed yet.
     *
     * Once [confirmImport] has started the Room transaction the operation is deliberately
     * no longer cancellable. Clearing the pending state mid-mutation would tell the user
     * the import was cancelled while the transaction went on to complete, so the reported
     * state would not match their data. Writes stay cancellable only before the
     * transaction starts (D-10, D-11).
     */
    fun cancelImport() {
        if (_uiState.value.importing) return
        importJob?.cancel()
        clearImportSavedState()
        _uiState.update {
            it.copy(importLaunch = null, importing = false, pendingImport = null)
        }
    }

    fun confirmImport() {
        val pending = _uiState.value.pendingImport ?: return
        _uiState.update { it.copy(importing = true) }
        viewModelScope.launch {
            try {
                when (val payload = pending.payload) {
                    is ImportPayload.Restore -> applyRestore(payload.backup)
                    is ImportPayload.Merge -> applyMerge(payload.records)
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                clearImportSavedState()
                _uiState.update {
                    it.copy(
                        importing = false,
                        pendingImport = null,
                        importError = if (error is ImportConflictException) {
                            ImportMessages.RECORDS_CHANGED
                        } else {
                            ImportMessages.IMPORT_FAILED
                        }
                    )
                }
            }
        }
    }

    private suspend fun applyRestore(backup: BackupPayload) {
        val counts = dataControlDao.replaceEverything(backup)
        clearImportSavedState()
        clearDraftSavedState()
        // Settings were replaced, so the seeded drafts are stale. Reseed them from the
        // restored values rather than letting a stale draft overwrite them on next save.
        val settings = backup.settings
        _uiState.update {
            it.copy(
                importing = false,
                pendingImport = null,
                importError = null,
                anchorDraft = AnchorDraft(settings.anchor2, settings.anchor5, settings.anchor8),
                chipDraft = settings.onsetChips.joinToString(", "),
                anchorError = null,
                chipError = null,
                pendingDocument = null,
                retryDocument = null,
                eraseConfirmation = null,
                message = ImportMessages.restored(
                    counts.entries,
                    counts.sleeps,
                    counts.markers,
                    counts.externalScores,
                    counts.safetyPlanItems,
                    counts.breathingSessions
                )
            )
        }
        runCatching(onDataReplaced)
    }

    private suspend fun applyMerge(records: RecordsPayload) {
        val counts = dataControlDao.addRecords(records) { snapshot ->
            checkRecordConflicts(records, snapshot) is ParseResult.Ok
        }
        clearImportSavedState()
        _uiState.update {
            it.copy(
                importing = false,
                pendingImport = null,
                importError = null,
                message = ImportMessages.added(
                    counts.entries, counts.sleeps, counts.markers, counts.breathingSessions
                )
            )
        }
    }

    fun dismissImportError() = _uiState.update { it.copy(importError = null) }

    private fun clearImportSavedState() {
        savedStateHandle.remove<String>(IMPORT_KIND_KEY)
        savedStateHandle.remove<Boolean>(IMPORT_PREVIEW_PENDING_KEY)
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
