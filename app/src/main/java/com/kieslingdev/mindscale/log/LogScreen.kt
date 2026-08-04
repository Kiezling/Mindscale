package com.kieslingdev.mindscale.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.settings.vocabularyForEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val RowTimeTwelveHourFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val RowTimeTwentyFourHourFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
private val FilterDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun LogRoute(viewModel: LogViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LogScreen(state, viewModel::onEvent, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    uiState: LogUiState,
    onEvent: (LogEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var picking by remember { mutableStateOf<FilterBound?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("full_log_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterHeader(
                state = uiState,
                onFrom = { picking = FilterBound.FROM },
                onTo = { picking = FilterBound.TO },
                onClear = { onEvent(LogEvent.ClearFilter) }
            )
        }

        uiState.message?.let { message ->
            item {
                Surface(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().testTag("log_message")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onEvent(LogEvent.MessageDismissed) }) { Text("Dismiss") }
                    }
                }
            }
        }

        uiState.readError?.let { error ->
            item {
                Column(modifier = Modifier.fillMaxWidth().testTag("log_read_error")) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { onEvent(LogEvent.Retry) }) { Text("Retry") }
                }
            }
        }

        if (uiState.days.isEmpty() && uiState.readError == null) {
            item {
                LogEmptyState(
                    filtered = uiState.hasAnyRecords && uiState.appliedFilter != LogFilter()
                )
            }
        } else {
            uiState.days.forEach { day ->
                item(key = "day:${day.date}") {
                    DayHeader(day.date)
                }
                items(day.items, key = { it.stableKey }) { item ->
                    LogItemRow(
                        item = item,
                        editDraft = uiState.editDraft?.takeIf { item is LogItem.Rating && it.entryId == item.id },
                        noteDraft = uiState.noteDraft?.takeIf { item is LogItem.Rating && it.entryId == item.id },
                        settings = uiState.settings,
                        onEvent = onEvent
                    )
                }
            }
        }
    }

    picking?.let { bound ->
        val selected = when (bound) {
            FilterBound.FROM -> uiState.pendingFilter.from
            FilterBound.TO -> uiState.pendingFilter.to
        }
        LogDatePicker(
            title = if (bound == FilterBound.FROM) "From" else "To",
            selected = selected,
            onSelected = { date ->
                when (bound) {
                    FilterBound.FROM -> onEvent(LogEvent.FromChanged(date))
                    FilterBound.TO -> onEvent(LogEvent.ToChanged(date))
                }
                picking = null
            },
            onDismiss = { picking = null }
        )
    }

    uiState.deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { onEvent(LogEvent.DeleteCancelled) },
            title = { Text("Delete ${target.description}?") },
            text = { Text("This permanently deletes this record. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onEvent(LogEvent.DeleteConfirmed) }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(LogEvent.DeleteCancelled) }) { Text("Cancel") }
            }
        )
    }
}

private enum class FilterBound { FROM, TO }

@Composable
private fun FilterHeader(
    state: LogUiState,
    onFrom: () -> Unit,
    onTo: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onFrom,
                modifier = Modifier.weight(1f).testTag("log_from_button")
                    .semantics { contentDescription = "Choose From date" }
            ) {
                Text(state.pendingFilter.from?.format(FilterDateFormatter) ?: "From")
            }
            OutlinedButton(
                onClick = onTo,
                modifier = Modifier.weight(1f).testTag("log_to_button")
                    .semantics { contentDescription = "Choose To date" }
            ) {
                Text(state.pendingFilter.to?.format(FilterDateFormatter) ?: "To")
            }
            TextButton(onClick = onClear, modifier = Modifier.testTag("log_all_button")) {
                Text("All")
            }
        }
        state.filterError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("log_filter_error"))
        }
        Text(
            text = "${state.recordCount} ${if (state.recordCount == 1) "record" else "records"} · ratings, sleep and events",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("log_record_count")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogDatePicker(
    title: String,
    selected: LocalDate?,
    onSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = selected?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val date = pickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    onSelected(date)
                }
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = pickerState, title = { Text(title, modifier = Modifier.padding(16.dp)) })
    }
}

@Composable
private fun DayHeader(date: LocalDate) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DayFormatter)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("log_day_$date")
    )
}

@Composable
private fun LogItemRow(
    item: LogItem,
    editDraft: LogEditDraft?,
    noteDraft: LogNoteDraft?,
    settings: com.kieslingdev.mindscale.data.TrackSettings,
    onEvent: (LogEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().testTag("log_row_${item.stableKey}")) {
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = when (item) {
                    is LogItem.Rating -> item.entry.value.toString()
                    is LogItem.Sleep -> "—"
                    is LogItem.Event -> "×"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.widthIn(min = 28.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = itemDescription(item, settings.hourFormat, settings.hideNotes)
                    }
            ) {
                Text(formatTime(item.timestamp, settings.hourFormat), style = MaterialTheme.typography.bodyMedium)
                val meta = itemMeta(item)
                if (meta.isNotBlank()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (item is LogItem.Rating) {
                TextButton(
                    onClick = { onEvent(LogEvent.EditToggled(item.id)) },
                    modifier = Modifier.semantics { contentDescription = "Edit rating ${item.entry.value}" }
                ) { Text("Edit") }
                TextButton(
                    onClick = { onEvent(LogEvent.NoteToggled(item.id)) },
                    modifier = Modifier.semantics { contentDescription = "Edit note for rating ${item.entry.value}" }
                ) { Text("Note") }
            }
            TextButton(
                onClick = { onEvent(LogEvent.DeleteRequested(item)) },
                modifier = Modifier.semantics { contentDescription = "Delete ${deleteType(item)}" }
            ) { Text("Delete") }
        }
        if (item is LogItem.Rating && !settings.hideNotes && !item.entry.note.isNullOrBlank()) {
            Text(
                item.entry.note.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 38.dp, bottom = 8.dp).testTag("log_note_${item.id}")
            )
        }
        if (editDraft != null) {
            InlineEditPanel(
                editDraft,
                vocabularyForEntry(settings, editDraft.chips),
                onEvent
            )
        }
        if (noteDraft != null) InlineNotePanel(noteDraft, onEvent)
    }
}

@Composable
private fun InlineEditPanel(
    draft: LogEditDraft,
    vocabulary: List<String>,
    onEvent: (LogEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 38.dp, bottom = 12.dp)
            .testTag("log_inline_edit_${draft.entryId}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..10).forEach { value ->
                FilterChip(
                    selected = value == draft.value,
                    onClick = { onEvent(LogEvent.EditValueSelected(value)) },
                    label = { Text(value.toString()) },
                    modifier = Modifier.testTag("log_edit_value_$value")
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            vocabulary.forEach { chip ->
                FilterChip(
                    selected = chip in draft.chips,
                    onClick = { onEvent(LogEvent.EditChipToggled(chip)) },
                    label = { Text(chip) },
                    modifier = Modifier.testTag("log_edit_chip_$chip")
                )
            }
        }
        OutlinedTextField(
            value = draft.timestampText,
            onValueChange = { onEvent(LogEvent.EditTimestampTextChanged(it)) },
            label = { Text("Time (yyyy-MM-dd HH:mm)") },
            supportingText = draft.error?.let { { Text(it) } },
            isError = draft.error != null,
            modifier = Modifier.fillMaxWidth().testTag("log_edit_timestamp")
        )
    }
}

@Composable
private fun InlineNotePanel(draft: LogNoteDraft, onEvent: (LogEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 38.dp, bottom = 12.dp)
            .testTag("log_inline_note_${draft.entryId}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = draft.text,
            onValueChange = { onEvent(LogEvent.NoteTextChanged(it)) },
            label = { Text("Note") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("log_note_field")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onEvent(LogEvent.NoteSaved) }) { Text("Save") }
            TextButton(onClick = { onEvent(LogEvent.NoteCancelled) }) { Text("Cancel") }
        }
    }
}

@Composable
private fun LogEmptyState(filtered: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp).testTag("log_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (filtered) "No records in this date range" else "No records yet")
        if (!filtered) {
            Text(
                "A well day costs nothing and needs no log.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTime(timestamp: Long, hourFormat: HourFormat): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(
        if (hourFormat == HourFormat.TWENTY_FOUR) RowTimeTwentyFourHourFormatter
        else RowTimeTwelveHourFormatter
    )

private fun itemMeta(item: LogItem): String = when (item) {
    is LogItem.Rating -> buildList {
        when (item.entry.kind) {
            EntryKind.SLEEP -> add("went to sleep")
            EntryKind.WAKE -> add("woke up")
            null -> if (item.entry.value == 0) add("ended")
        }
        if (item.entry.chips.isNotEmpty()) add(item.entry.chips.joinToString(" · "))
    }.joinToString(" · ")
    is LogItem.Sleep -> if (item.interval.endTs == null) "sleeping now" else "slept ${formatSleepDuration(item.interval)}"
    is LogItem.Event -> item.marker.text
}

private fun itemDescription(item: LogItem, hourFormat: HourFormat, hideNotes: Boolean): String = when (item) {
    is LogItem.Rating -> buildString {
        append("Rating ${item.entry.value}, ${formatTime(item.timestamp, hourFormat)}")
        val meta = itemMeta(item)
        if (meta.isNotBlank()) append(", $meta")
        if (!hideNotes) item.entry.note?.takeIf { it.isNotBlank() }?.let { append(", note $it") }
    }
    is LogItem.Sleep -> "Sleep interval, ${formatTime(item.timestamp, hourFormat)}, ${itemMeta(item)}"
    is LogItem.Event -> "Event, ${formatTime(item.timestamp, hourFormat)}, ${item.marker.text}"
}

private fun deleteType(item: LogItem): String = when (item) {
    is LogItem.Rating -> "rating ${item.entry.value}"
    is LogItem.Sleep -> "sleep interval"
    is LogItem.Event -> "event"
}
