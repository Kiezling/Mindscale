package com.kieslingdev.mindscale.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryDao
import com.kieslingdev.mindscale.data.MarkerDao
import com.kieslingdev.mindscale.data.SleepDao
import com.kieslingdev.mindscale.data.TrackSettingsDao
import com.kieslingdev.mindscale.notes.RichNoteCodec
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val FROM_KEY = "log.fromEpochDay"
private const val TO_KEY = "log.toEpochDay"
private const val EDIT_ID_KEY = "log.editId"
private const val EDIT_VALUE_KEY = "log.editValue"
private const val EDIT_TIMESTAMP_KEY = "log.editTimestamp"
private const val EDIT_CHIPS_KEY = "log.editChips"
private const val NOTE_ID_KEY = "log.noteId"
private const val NOTE_TEXT_KEY = "log.noteText"
private const val EVENT_ID_KEY = "log.eventId"
private const val EVENT_TEXT_KEY = "log.eventText"
private const val EVENT_TIMESTAMP_KEY = "log.eventTimestamp"
private const val INVALID_RANGE = "From must be on or before To."
private const val INVALID_TIMESTAMP = "Choose a date and time that is not in the future."
private const val MISSING_RECORD = "That record no longer exists"
private const val UPDATE_FAILED = "Could not update that rating. Please try again."
private const val NOTE_FAILED = "Could not save that note. Please try again."
private const val NOTE_INVALID = "Use at most 4,000 characters and only ordinary text, tabs, and line breaks."
private const val DELETE_FAILED = "Could not delete that record. Please try again."
private const val EVENT_INVALID = "Use 1–4,000 characters of ordinary text and a time that is not in the future."
private const val EVENT_FAILED = "Could not update that event. Please try again."

private sealed interface LogQueryResult {
    data class Success(val items: List<LogItem>) : LogQueryResult
    data class Failure(val error: Throwable) : LogQueryResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModel(
    private val entryDao: EntryDao,
    private val sleepDao: SleepDao,
    private val markerDao: MarkerDao,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val zoneProvider: () -> ZoneId = ZoneId::systemDefault,
    private val nowProvider: () -> Long = System::currentTimeMillis,
    private val settingsDao: TrackSettingsDao? = null
) : ViewModel() {

    private val initialFilter = LogFilter(
        from = savedStateHandle.get<Long>(FROM_KEY)?.let(LocalDate::ofEpochDay),
        to = savedStateHandle.get<Long>(TO_KEY)?.let(LocalDate::ofEpochDay)
    )
    private val appliedFilter = MutableStateFlow(initialFilter)
    private val retryVersion = MutableStateFlow(0)
    private val _uiState = MutableStateFlow(
        LogUiState(
            appliedFilter = initialFilter,
            pendingFilter = initialFilter,
            editDraft = restoredEditDraft(),
            noteDraft = restoredNoteDraft(),
            eventDraft = restoredEventDraft()
        )
    )
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(appliedFilter, retryVersion) { filter, _ -> filter }
                .flatMapLatest { filter ->
                    val range = epochRange(filter, zoneProvider())
                    combine(
                        entryDao.observeBetween(range.fromTs, range.toTsExclusive),
                        sleepDao.observeBetween(range.fromTs, range.toTsExclusive),
                        markerDao.observeBetween(range.fromTs, range.toTsExclusive)
                    ) { entries, sleeps, markers ->
                        LogQueryResult.Success(combineLogItems(entries, sleeps, markers)) as LogQueryResult
                    }.catch { error ->
                        emit(LogQueryResult.Failure(error))
                    }
                }
                .collect { result ->
                    when (result) {
                        is LogQueryResult.Success -> _uiState.update {
                            it.copy(
                                days = groupLogItems(result.items, zoneProvider()),
                                recordCount = result.items.size,
                                readError = null
                            )
                        }
                        is LogQueryResult.Failure -> _uiState.update {
                            it.copy(readError = result.error.message ?: "Could not read the log")
                        }
                    }
                }
        }
        settingsDao?.let { dao ->
            viewModelScope.launch {
                dao.observe().collect { settings -> _uiState.update { it.copy(settings = settings) } }
            }
        }
        viewModelScope.launch {
            combine(
                entryDao.observeCount(),
                sleepDao.observeCount(),
                markerDao.observeCount()
            ) { entryCount, sleepCount, markerCount -> entryCount + sleepCount + markerCount }
                .collect { total -> _uiState.update { it.copy(hasAnyRecords = total > 0) } }
        }
    }

    fun onEvent(event: LogEvent) {
        when (event) {
            is LogEvent.FromChanged -> updatePendingFilter(_uiState.value.pendingFilter.copy(from = event.date))
            is LogEvent.ToChanged -> updatePendingFilter(_uiState.value.pendingFilter.copy(to = event.date))
            LogEvent.ClearFilter -> updatePendingFilter(LogFilter())
            is LogEvent.EditToggled -> toggleEdit(event.entryId)
            is LogEvent.EditValueSelected -> updateEditValue(event.value)
            is LogEvent.EditChipToggled -> updateEditChip(event.chip)
            is LogEvent.EditTimestampTextChanged -> updateEditTimestamp(event.text)
            is LogEvent.NoteToggled -> toggleNote(event.entryId)
            is LogEvent.NoteTextChanged -> updateNoteText(event.text)
            LogEvent.NoteSaved -> saveNote()
            LogEvent.NoteCancelled -> setNoteDraft(null)
            LogEvent.NoteDeleteRequested -> updateNoteText("")
            is LogEvent.EventEditToggled -> toggleEventEdit(event.markerId)
            is LogEvent.EventTextChanged -> updateEventDraft { it.copy(text = event.text, error = null) }
            is LogEvent.EventTimestampChanged -> updateEventDraft { it.copy(timestampText = event.text, error = null) }
            LogEvent.EventSaveRequested -> saveEvent()
            LogEvent.EventEditCancelled -> if (_uiState.value.eventDraft?.isSaving != true) setEventDraft(null)
            is LogEvent.DeleteRequested -> requestDelete(event.item)
            LogEvent.DeleteConfirmed -> confirmDelete()
            LogEvent.DeleteCancelled -> _uiState.update { it.copy(deleteTarget = null) }
            LogEvent.MessageDismissed -> _uiState.update { it.copy(message = null) }
            LogEvent.Retry -> retryVersion.update { it + 1 }
        }
    }

    private fun updatePendingFilter(filter: LogFilter) {
        val invalid = filter.from != null && filter.to != null && filter.from > filter.to
        _uiState.update { it.copy(pendingFilter = filter, filterError = if (invalid) INVALID_RANGE else null) }
        if (invalid) return
        persistFilter(filter)
        appliedFilter.value = filter
        _uiState.update { it.copy(appliedFilter = filter) }
    }

    private fun persistFilter(filter: LogFilter) {
        if (filter.from == null) savedStateHandle.remove<Long>(FROM_KEY)
        else savedStateHandle[FROM_KEY] = filter.from.toEpochDay()
        if (filter.to == null) savedStateHandle.remove<Long>(TO_KEY)
        else savedStateHandle[TO_KEY] = filter.to.toEpochDay()
    }

    private fun allEntries(): List<Entry> = _uiState.value.days.flatMap { day ->
        day.items.mapNotNull { (it as? LogItem.Rating)?.entry }
    }

    private fun findEntry(id: Long): Entry? = allEntries().firstOrNull { it.id == id }

    private fun toggleEdit(entryId: Long) {
        if (_uiState.value.eventDraft?.isSaving == true) return
        val current = _uiState.value.editDraft
        if (current?.entryId == entryId) {
            setEditDraft(null)
            return
        }
        val entry = findEntry(entryId) ?: return showMissing()
        setNoteDraft(null)
        setEventDraft(null)
        setEditDraft(
            LogEditDraft(
                entryId = entry.id,
                value = entry.value,
                timestampText = formatEditTimestamp(entry.ts, zoneProvider()),
                chips = entry.chips.toSet()
            )
        )
    }

    private fun updateEditValue(value: Int) {
        require(value in 0..10)
        val draft = _uiState.value.editDraft ?: return
        val ts = validEditTimestampOrMarkError(draft) ?: return
        val updated = draft.copy(value = value)
        setEditDraft(updated)
        persistEdit(updated, ts, closeOnSuccess = true)
    }

    private fun updateEditChip(chip: String) {
        val draft = _uiState.value.editDraft ?: return
        val ts = validEditTimestampOrMarkError(draft) ?: return
        val chips = if (chip in draft.chips) draft.chips - chip else draft.chips + chip
        val updated = draft.copy(chips = chips)
        setEditDraft(updated)
        persistEdit(updated, ts)
    }

    private fun updateEditTimestamp(text: String) {
        val draft = _uiState.value.editDraft ?: return
        val parsed = parseEditTimestamp(text, zoneProvider())
        val valid = parsed != null && parsed <= nowProvider()
        val updated = draft.copy(timestampText = text, error = if (valid) null else INVALID_TIMESTAMP)
        setEditDraft(updated)
        if (valid) persistEdit(updated, parsed!!)
    }

    private fun persistEdit(
        draft: LogEditDraft,
        timestamp: Long,
        closeOnSuccess: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                if (timestamp > nowProvider()) {
                    val current = _uiState.value.editDraft
                    if (current?.entryId == draft.entryId) setEditDraft(current.copy(error = INVALID_TIMESTAMP))
                    return@launch
                }
                val changed = entryDao.updateEditableFields(
                    draft.entryId,
                    timestamp,
                    draft.value,
                    draft.chips.toList()
                )
                if (changed == 0) {
                    showMissing()
                } else if (closeOnSuccess && _uiState.value.editDraft == draft) {
                    setEditDraft(null)
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(message = UPDATE_FAILED) }
            }
        }
    }

    private fun toggleNote(entryId: Long) {
        if (_uiState.value.eventDraft?.isSaving == true) return
        val current = _uiState.value.noteDraft
        if (current?.entryId == entryId) {
            setNoteDraft(null)
            return
        }
        val entry = findEntry(entryId) ?: return showMissing()
        setEditDraft(null)
        setEventDraft(null)
        setNoteDraft(LogNoteDraft(entry.id, entry.note.orEmpty()))
    }

    private fun toggleEventEdit(markerId: Long) {
        val current = _uiState.value.eventDraft
        if (current?.isSaving == true) return
        if (current?.markerId == markerId) {
            setEventDraft(null)
            return
        }
        val marker = _uiState.value.days.flatMap { it.items }
            .filterIsInstance<LogItem.Event>().firstOrNull { it.id == markerId }?.marker
            ?: return showMissing()
        setEditDraft(null)
        setNoteDraft(null)
        setEventDraft(LogEventDraft(marker.id, marker.text, formatEditTimestamp(marker.ts, zoneProvider())))
    }

    private fun updateEventDraft(change: (LogEventDraft) -> LogEventDraft) {
        val draft = _uiState.value.eventDraft ?: return
        if (!draft.isSaving) setEventDraft(change(draft))
    }

    private fun saveEvent() {
        val draft = _uiState.value.eventDraft ?: return
        if (draft.isSaving) return
        val timestamp = parseEditTimestamp(draft.timestampText, zoneProvider())
        val text = draft.text.trim()
        if (timestamp == null || timestamp > nowProvider() || text.isBlank() ||
            !isAllowedLongText(text)
        ) {
            setEventDraft(draft.copy(error = EVENT_INVALID))
            return
        }
        setEventDraft(draft.copy(isSaving = true, error = null))
        viewModelScope.launch {
            try {
                val exists = markerDao.getById(draft.markerId)
                if (exists == null) {
                    showMissing()
                    return@launch
                }
                if (timestamp > nowProvider()) {
                    setEventDraft(draft.copy(error = EVENT_INVALID))
                    return@launch
                }
                val changed = markerDao.updateEditableFields(draft.markerId, timestamp, text)
                if (changed == 0) showMissing() else setEventDraft(null)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                setEventDraft(draft.copy(error = EVENT_FAILED))
            }
        }
    }

    private fun updateNoteText(text: String) {
        val draft = _uiState.value.noteDraft ?: return
        setNoteDraft(draft.copy(text = text, error = null))
    }

    private fun saveNote() {
        val draft = _uiState.value.noteDraft ?: return
        if (!isAllowedLongText(draft.text)) {
            setNoteDraft(draft.copy(error = NOTE_INVALID))
            return
        }
        viewModelScope.launch {
            try {
                val decoded = RichNoteCodec.plainText(draft.text)
                val persisted = when {
                    decoded.isBlank() -> null
                    draft.text.startsWith("[[MindScale note v1]]\n") -> draft.text
                    else -> draft.text.trim()
                }
                if (entryDao.updateNote(draft.entryId, persisted) == 0) {
                    showMissing()
                } else if (_uiState.value.noteDraft == draft) {
                    setNoteDraft(null)
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(message = NOTE_FAILED) }
            }
        }
    }

    private fun requestDelete(item: LogItem) {
        if (_uiState.value.eventDraft?.isSaving == true) return
        val description = when (item) {
            is LogItem.Rating -> "rating ${item.entry.value}"
            is LogItem.Sleep -> "sleep interval"
            is LogItem.Event -> "event ${item.marker.text}"
        }
        _uiState.update { it.copy(deleteTarget = LogDeleteTarget(item, description)) }
    }

    private fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            try {
                val changed = when (val item = target.item) {
                    is LogItem.Rating -> entryDao.deleteById(item.id)
                    is LogItem.Sleep -> sleepDao.deleteById(item.id)
                    is LogItem.Event -> markerDao.deleteById(item.id)
                }
                if (changed == 0) {
                    showMissing()
                } else if (_uiState.value.deleteTarget == target) {
                    _uiState.update { it.copy(deleteTarget = null) }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(message = DELETE_FAILED) }
            }
        }
    }

    private fun setEditDraft(draft: LogEditDraft?) {
        if (draft == null) {
            savedStateHandle.remove<Long>(EDIT_ID_KEY)
            savedStateHandle.remove<Int>(EDIT_VALUE_KEY)
            savedStateHandle.remove<String>(EDIT_TIMESTAMP_KEY)
            savedStateHandle.remove<ArrayList<String>>(EDIT_CHIPS_KEY)
        } else {
            savedStateHandle[EDIT_ID_KEY] = draft.entryId
            savedStateHandle[EDIT_VALUE_KEY] = draft.value
            savedStateHandle[EDIT_TIMESTAMP_KEY] = draft.timestampText
            savedStateHandle[EDIT_CHIPS_KEY] = ArrayList(draft.chips)
        }
        _uiState.update { it.copy(editDraft = draft) }
    }

    private fun restoredEditDraft(): LogEditDraft? {
        val id = savedStateHandle.get<Long>(EDIT_ID_KEY) ?: return null
        val value = savedStateHandle.get<Int>(EDIT_VALUE_KEY) ?: return null
        val timestamp = savedStateHandle.get<String>(EDIT_TIMESTAMP_KEY) ?: return null
        val chips = savedStateHandle.get<ArrayList<String>>(EDIT_CHIPS_KEY)?.toSet().orEmpty()
        val parsed = parseEditTimestamp(timestamp, zoneProvider())
        return LogEditDraft(
            id,
            value,
            timestamp,
            chips,
            if (parsed == null || parsed > nowProvider()) INVALID_TIMESTAMP else null
        )
    }

    private fun setNoteDraft(draft: LogNoteDraft?) {
        if (draft == null) {
            savedStateHandle.remove<Long>(NOTE_ID_KEY)
            savedStateHandle.remove<String>(NOTE_TEXT_KEY)
        } else {
            savedStateHandle[NOTE_ID_KEY] = draft.entryId
            savedStateHandle[NOTE_TEXT_KEY] = draft.text
        }
        _uiState.update { it.copy(noteDraft = draft) }
    }

    private fun restoredNoteDraft(): LogNoteDraft? {
        val id = savedStateHandle.get<Long>(NOTE_ID_KEY) ?: return null
        return LogNoteDraft(id, savedStateHandle[NOTE_TEXT_KEY] ?: "")
    }

    private fun setEventDraft(draft: LogEventDraft?) {
        if (draft == null) {
            savedStateHandle.remove<Long>(EVENT_ID_KEY)
            savedStateHandle.remove<String>(EVENT_TEXT_KEY)
            savedStateHandle.remove<String>(EVENT_TIMESTAMP_KEY)
        } else {
            savedStateHandle[EVENT_ID_KEY] = draft.markerId
            savedStateHandle[EVENT_TEXT_KEY] = draft.text
            savedStateHandle[EVENT_TIMESTAMP_KEY] = draft.timestampText
        }
        _uiState.update { it.copy(eventDraft = draft) }
    }

    private fun restoredEventDraft(): LogEventDraft? {
        val id = savedStateHandle.get<Long>(EVENT_ID_KEY) ?: return null
        val text = savedStateHandle.get<String>(EVENT_TEXT_KEY) ?: return null
        val timestamp = savedStateHandle.get<String>(EVENT_TIMESTAMP_KEY) ?: return null
        return LogEventDraft(id, text, timestamp)
    }

    private fun validEditTimestampOrMarkError(draft: LogEditDraft): Long? {
        val parsed = parseEditTimestamp(draft.timestampText, zoneProvider())
        if (parsed == null || parsed > nowProvider()) {
            if (_uiState.value.editDraft == draft) setEditDraft(draft.copy(error = INVALID_TIMESTAMP))
            return null
        }
        return parsed
    }

    private fun isAllowedLongText(text: String): Boolean {
        if (text.isBlank()) return true
        if (text.codePointCount(0, text.length) > 4_000) return false
        return text.none { c ->
            c == '\uFEFF' || c.code == 0x7F ||
                (c.code < 0x20 && c != '\t' && c != '\n' && c != '\r')
        }
    }

    private fun showMissing() {
        setEditDraft(null)
        setNoteDraft(null)
        setEventDraft(null)
        _uiState.update { it.copy(deleteTarget = null, message = MISSING_RECORD) }
    }
}
