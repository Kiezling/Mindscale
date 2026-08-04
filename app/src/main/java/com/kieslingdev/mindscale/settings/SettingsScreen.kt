package com.kieslingdev.mindscale.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.ThemeMode
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class SettingsFocus { TOP, ANCHORS, DATA }

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    focus: SettingsFocus,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) viewModel.documentPickerCanceled()
        else {
            val document = uiState.pendingDocument
            if (document == null) viewModel.documentWriteFailed()
            else {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                            OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(document.contents) }
                        } ?: error("Document provider returned no stream")
                    }.onSuccess { viewModel.documentWriteSucceeded() }
                        .onFailure { viewModel.documentWriteFailed() }
                }
            }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) viewModel.documentPickerCanceled()
        else {
            val document = uiState.pendingDocument
            if (document == null) viewModel.documentWriteFailed()
            else {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                            OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(document.contents) }
                        } ?: error("Document provider returned no stream")
                    }.onSuccess { viewModel.documentWriteSucceeded() }
                        .onFailure { viewModel.documentWriteFailed() }
                }
            }
        }
    }

    val pending = uiState.pendingDocument
    LaunchedEffect(pending) {
        if (pending != null) {
            if (pending.kind == ExportKind.RECORDS) csvLauncher.launch(pending.filename)
            else jsonLauncher.launch(pending.filename)
        }
    }

    SettingsScreen(uiState = uiState, focus = focus, viewModel = viewModel, modifier = modifier)
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    focus: SettingsFocus,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(focus) {
        listState.animateScrollToItem(when (focus) {
            SettingsFocus.TOP -> 0
            SettingsFocus.ANCHORS -> 4
            SettingsFocus.DATA -> 12
        })
    }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("settings_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "appearance") {
            SettingsSection("Appearance") {
                ChoiceRow(
                    values = ThemeMode.entries,
                    selected = uiState.settings.themeMode,
                    label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    onSelected = viewModel::setTheme
                )
            }
        }
        item(key = "time") {
            SettingsSection("Time format") {
                ChoiceRow(
                    values = HourFormat.entries,
                    selected = uiState.settings.hourFormat,
                    label = { if (it == HourFormat.TWELVE) "12-hour" else "24-hour" },
                    onSelected = viewModel::setHourFormat
                )
            }
        }
        item(key = "hold") {
            SettingsSection("An entry ends after") {
                ChoiceRow(
                    values = HoldDuration.entries,
                    selected = uiState.settings.holdDuration,
                    label = { "${it.hours}h" },
                    onSelected = viewModel::setHoldDuration
                )
                Text(
                    "Waking hours. Sleep pauses this clock. This changes how Insights treats " +
                        "gaps across your history; your records do not change.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item(key = "divider_before_anchors") { HorizontalDivider() }
        item(key = "anchors") {
            SettingsSection("What the numbers mean to you") {
                Text("Personal anchors help the same number mean the same thing months later.")
                AnchorField("2 — noticeable", uiState.anchorDraft.anchor2, viewModel::updateAnchor2, "anchor_2")
                AnchorField("5 — changing what I do", uiState.anchorDraft.anchor5, viewModel::updateAnchor5, "anchor_5")
                AnchorField("8 — most of what is happening", uiState.anchorDraft.anchor8, viewModel::updateAnchor8, "anchor_8")
                uiState.anchorError?.let { ErrorText(it) }
                TextButton(onClick = viewModel::saveAnchors, modifier = Modifier.testTag("save_anchors")) {
                    Text("Save anchors")
                }
            }
        }
        item(key = "onset_words") {
            SettingsSection("What was happening") {
                Text("Separate onset words with commas or new lines.")
                OutlinedTextField(
                    value = uiState.chipDraft,
                    onValueChange = viewModel::updateChipDraft,
                    label = { Text("Onset words") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("onset_words")
                )
                uiState.chipError?.let { ErrorText(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = viewModel::saveOnsetWords, modifier = Modifier.testTag("save_onset_words")) {
                        Text("Save words")
                    }
                    TextButton(onClick = viewModel::restoreDefaultWords) { Text("Restore defaults") }
                }
            }
        }
        item(key = "divider_before_preferences") { HorizontalDivider() }
        item(key = "sleep") {
            SettingSwitch(
                "Sleep and Wake",
                "Show capture controls for falling asleep and waking up.",
                uiState.settings.sleepOn,
                viewModel::setSleepOn
            )
        }
        item(key = "chips") {
            SettingSwitch(
                "Ask what was happening",
                "Prompt for onset words after a symptom begins.",
                uiState.settings.askChips,
                viewModel::setAskChips
            )
        }
        item(key = "notes") {
            SettingSwitch(
                "Hide notes in lists",
                "Keep note actions available while folding preview text.",
                uiState.settings.hideNotes,
                viewModel::setHideNotes
            )
        }
        item(key = "pause") {
            SettingSwitch(
                if (uiState.settings.paused) "Tracking paused" else "Pause tracking",
                "Recorded data and the Full Log remain available.",
                uiState.settings.paused,
                viewModel::setPaused
            )
        }
        item(key = "divider_before_data") { HorizontalDivider() }
        item(key = "data") {
            SettingsSection("Your data") {
                Text("Exports stay local and go only to the document location you choose.")
                Text(
                    "Records CSV contains ratings, sleep, and marked events only. It excludes your Profile name " +
                        "and external PHQ-8/GAD-7 totals. JSON backup includes them; Clinician summary exports " +
                        "the bounded factual summary."
                )
                TextButton(onClick = viewModel::requestBackup, modifier = Modifier.testTag("export_backup")) {
                    Text("Export backup")
                }
                TextButton(onClick = viewModel::requestRecordsCsv, modifier = Modifier.testTag("export_records")) {
                    Text("Export records")
                }
                TextButton(onClick = viewModel::requestExportThenErase, modifier = Modifier.testTag("export_then_erase")) {
                    Text("Export, then erase everything")
                }
                if (uiState.preparingExport) Text("Preparing export…")
            }
        }
        uiState.readError?.let { error ->
            item(key = "read_error") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ErrorText(error)
                    TextButton(onClick = viewModel::retrySettingsRead) { Text("Retry") }
                }
            }
        }
        uiState.message?.let { message ->
            item(key = "message") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message, modifier = Modifier.weight(1f))
                    if (uiState.retryDocument != null) {
                        TextButton(onClick = viewModel::retryDocumentWrite) { Text("Retry export") }
                    }
                    TextButton(onClick = viewModel::dismissMessage) { Text("Dismiss") }
                }
            }
        }
    }

    uiState.eraseConfirmation?.let { confirmation ->
        AlertDialog(
            onDismissRequest = viewModel::cancelErase,
            title = { Text("Erase everything on this device?") },
            text = {
                Text(
                    "This permanently deletes ${confirmation.entryCount} ratings, " +
                        "${confirmation.sleepCount} sleep intervals, and ${confirmation.markerCount} markers. " +
                        "Your Profile name and all externally obtained totals are also deleted. " +
                        "Anchors, custom words, preferences, drafts, and retained export text reset too."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmErase, modifier = Modifier.testTag("confirm_erase")) {
                    Text("Erase everything")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelErase) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun <T> ChoiceRow(values: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        values.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(label(value)) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "${label(value)}, ${if (value == selected) "selected" else "not selected"}" }
            )
        }
    }
}

@Composable
private fun AnchorField(label: String, value: String, onValueChange: (String) -> Unit, tag: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag(tag)
    )
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$title. $description" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
