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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.breathing.BreathingCopy
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.ThemeMode
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class SettingsFocus { TOP, ANCHORS, DATA }

private val BACKUP_IMPORT_MIME_TYPES = arrayOf("application/json")
private val RECORDS_IMPORT_MIME_TYPES =
    arrayOf("text/csv", "text/comma-separated-values", "text/plain")

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

    // Read-only document access. No persistable permission is taken, no storage
    // permission is declared, and content is validated regardless of the declared MIME
    // type because providers routinely mislabel CSV (Phase 12, D-11).
    // Lint cannot follow the deferred opener across the lambda boundary. The stream is
    // opened lazily on the ViewModel's IO context and always closed there by
    // `open().use(::readBoundedUtf8)`, which is covered by SettingsImportViewModelTest.
    @Suppress("Recycle")
    val backupImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) viewModel.importPickerCanceled()
        else viewModel.importFileSelected(ImportKind.BACKUP_RESTORE) {
            context.contentResolver.openInputStream(uri) ?: error("No stream")
        }
    }
    @Suppress("Recycle")
    val recordsImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) viewModel.importPickerCanceled()
        else viewModel.importFileSelected(ImportKind.RECORDS_MERGE) {
            context.contentResolver.openInputStream(uri) ?: error("No stream")
        }
    }

    val importLaunch = uiState.importLaunch
    LaunchedEffect(importLaunch) {
        when (importLaunch) {
            ImportKind.BACKUP_RESTORE -> backupImportLauncher.launch(BACKUP_IMPORT_MIME_TYPES)
            ImportKind.RECORDS_MERGE -> recordsImportLauncher.launch(RECORDS_IMPORT_MIME_TYPES)
            null -> return@LaunchedEffect
        }
        viewModel.importLaunchHandled()
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
        item(key = "breathing") {
            SettingSwitch(
                BreathingCopy.SETTING_TITLE,
                BreathingCopy.SETTING_DESCRIPTION,
                uiState.settings.breathingOn,
                viewModel::setBreathingOn
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
                    "Records CSV contains ratings, sleep, marked events, and breathing sessions only. It " +
                        "excludes your Profile name and external PHQ-8/GAD-7 totals. JSON backup includes " +
                        "them; Clinician summary exports the bounded factual summary."
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
        item(key = "import") {
            SettingsSection("Bring data back") {
                Text(
                    "Restoring a backup replaces everything on this device. Importing a " +
                        "records CSV only adds ratings, sleep, marked events, and breathing " +
                        "sessions. MindScale shows exactly what will change and waits for you " +
                        "to confirm."
                )
                TextButton(
                    onClick = viewModel::requestBackupRestore,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("import_backup")
                        .semantics { contentDescription = "Restore from a MindScale JSON backup" }
                ) { Text("Restore from backup") }
                TextButton(
                    onClick = viewModel::requestRecordsImport,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("import_records")
                        .semantics { contentDescription = "Import a MindScale records CSV" }
                ) { Text("Import records") }
                if (uiState.importing) {
                    Text(
                        "Checking that file…",
                        modifier = Modifier
                            .testTag("import_progress")
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                uiState.importError?.let { error ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_error")
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        )
                        TextButton(onClick = viewModel::dismissImportError) { Text("Dismiss") }
                    }
                }
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

    uiState.pendingImport?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(pending.preview.title) },
            text = {
                // Scrollable so the whole factual preview stays readable at 200% font
                // scale, in landscape, and on small screens. No fixed height cap: the
                // dialog already constrains its body, and capping it pushed the
                // permanent-deletion sentence below the fold at large font sizes while
                // leaving the confirm action visible.
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .testTag("import_preview"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pending.preview.lines.forEach { Text(it) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmImport,
                    enabled = !uiState.importing,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("confirm_import")
                ) { Text(pending.preview.confirmLabel) }
            },
            dismissButton = {
                // Disabled once the mutation is running: the transaction can no longer be
                // cancelled, so offering Cancel would misreport what happened to the data.
                TextButton(
                    onClick = viewModel::cancelImport,
                    enabled = !uiState.importing,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("cancel_import")
                ) { Text("Cancel") }
            }
        )
    }

    uiState.eraseConfirmation?.let { confirmation ->
        AlertDialog(
            onDismissRequest = viewModel::cancelErase,
            title = { Text("Erase everything on this device?") },
            text = {
                Text(
                    "This permanently deletes ${confirmation.entryCount} ratings, " +
                        "${confirmation.sleepCount} sleep intervals, and ${confirmation.markerCount} markers. " +
                        "Your Profile name, all externally obtained totals, your safety plan " +
                        "(${confirmation.safetyPlanItemCount} " +
                        "${if (confirmation.safetyPlanItemCount == 1) "line" else "lines"}), and " +
                        "${confirmation.breathingSessionCount} breathing " +
                        "${if (confirmation.breathingSessionCount == 1) "session" else "sessions"} " +
                        "are also deleted. " +
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
