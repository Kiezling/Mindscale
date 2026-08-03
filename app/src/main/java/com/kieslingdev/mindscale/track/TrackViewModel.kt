package com.kieslingdev.mindscale.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * How long a transient tap readout (e.g. "7 - severe") stays visible before it
 * self-clears. Not specified precisely by the spec ("a few seconds"); 3000ms
 * is a reasonable implementer's call.
 */
private const val READOUT_DURATION_MILLIS = 3_000L

private const val FUTURE_TIMESTAMP_ERROR = "Timestamp cannot be in the future."

class TrackViewModel(
    private val entryDao: EntryDao,
    private val nowProvider: () -> Long = System::currentTimeMillis
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackUiState())
    val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

    private var readoutClearJob: Job? = null

    init {
        viewModelScope.launch {
            combine(entryDao.observeRecent(), entryDao.observeCount()) { recent, count ->
                recent to count
            }.collect { (recent, count) ->
                _uiState.update { it.copy(recentEntries = recent, isEmpty = count == 0) }
            }
        }
    }

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
        }
    }

    private fun handleKeyTapped(value: Int) {
        require(value in 0..10) { "value out of range 0..10: $value" }
        val now = nowProvider()
        viewModelScope.launch {
            entryDao.insert(Entry(ts = now, value = value))
        }

        val expiresAt = now + READOUT_DURATION_MILLIS
        _uiState.update { it.copy(transientReadout = ReadoutState(value, band(value), expiresAt)) }

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

        viewModelScope.launch {
            entryDao.insert(Entry(ts = dialog.timestampMillis, value = dialog.value))
        }
        _uiState.update { it.copy(backdateDialog = null) }
    }

    private fun handleEditRequested(entry: Entry) {
        _uiState.update {
            it.copy(editDialog = EditEntryState(originalEntry = entry, value = entry.value, timestampMillis = entry.ts))
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

    private fun handleEditSaveConfirmed() {
        val dialog = _uiState.value.editDialog ?: return
        if (dialog.error != null) return

        val updated = dialog.originalEntry.copy(
            ts = dialog.timestampMillis,
            value = dialog.value
        )
        viewModelScope.launch { entryDao.update(updated) }
        _uiState.update { it.copy(editDialog = null) }
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
        viewModelScope.launch { entryDao.update(dialog.originalEntry.copy(note = dialog.text)) }
        _uiState.update { it.copy(noteDialog = null) }
    }

    private fun handleDeleteConfirmed() {
        val entry = _uiState.value.pendingDelete
        if (entry != null) {
            viewModelScope.launch { entryDao.delete(entry) }
        }
        _uiState.update { it.copy(pendingDelete = null) }
    }

    private fun handleReadoutDismissed() {
        readoutClearJob?.cancel()
        _uiState.update { it.copy(transientReadout = null) }
    }
}
