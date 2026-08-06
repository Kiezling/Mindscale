package com.kieslingdev.mindscale.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kieslingdev.mindscale.ui.components.MsDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.data.ExternalInstrument
import com.kieslingdev.mindscale.safety.SafetyCopy
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ProfileRoute(
    viewModel: ReportProfileViewModel,
    onOpenReport: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSafety: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(uiState, viewModel, onOpenReport, onOpenSettings, modifier, onOpenSafety)
}

@Composable
fun ProfileScreen(
    uiState: ReportProfileUiState,
    viewModel: ReportProfileViewModel,
    onOpenReport: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSafety: () -> Unit = {}
) {
    val pendingDelete = uiState.pendingDeleteScoreId?.let { id -> uiState.scores.firstOrNull { it.id == id } }
    LazyColumn(
        modifier = modifier.testTag("profile_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "identity") {
            ProfileSection("Your profile") {
                OutlinedTextField(
                    value = uiState.nameDraft,
                    onValueChange = viewModel::updateNameDraft,
                    label = { Text("Display name (optional)") },
                    supportingText = { Text("Stored only on this device. Exports you create can contain your name.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { viewModel.saveName(false) },
                        enabled = uiState.nameDirty,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("profile_name_save")
                    ) { Text("Save name") }
                    if (uiState.nameConflict) {
                        TextButton(
                            onClick = { viewModel.saveName(true) },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("profile_name_replace")
                        ) { Text("Replace saved name") }
                    }
                }
            }
        }
        item(key = "stats") {
            ProfileSection("Recording facts") {
                val since = uiState.stats.firstRecordedTs?.let {
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
                } ?: "Nothing recorded yet"
                Text("Recording since: $since")
                Text("${uiState.stats.ratingCount} ratings · ${uiState.stats.sleepCount} sleep periods · ${uiState.stats.markerCount} marked events")
            }
        }
        item(key = "actions") {
            ProfileSection("Use your records") {
                TextButton(
                    onClick = onOpenReport,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("profile_open_report")
                ) { Text("Clinician summary") }
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("profile_open_settings")
                ) { Text("Settings") }
            }
        }
        item(key = "safety") {
            ProfileSection("Safety") {
                TextButton(
                    onClick = onOpenSafety,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("profile_open_safety")
                ) { Text(SafetyCopy.PROFILE_ROW) }
            }
        }
        item(key = "score_intro") {
            HorizontalDivider()
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Totals from elsewhere", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Enter a total only when PHQ-8 or GAD-7 was completed elsewhere. " +
                        "MindScale does not show the questions, administer either questionnaire, calculate a total, or interpret it.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Total entered by you from a result obtained elsewhere. MindScale did not administer or calculate it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item(key = "score_form") {
            ProfileSection(
                if (uiState.editingScoreId == null) "Add an external total" else "Edit external total",
                modifier = Modifier.testTag("score_form")
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExternalInstrument.entries.forEach { instrument ->
                        FilterChip(
                            selected = uiState.scoreInstrument == instrument,
                            onClick = { viewModel.selectInstrument(instrument) },
                            label = { Text(instrument.visibleLabel) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("score_instrument_${instrument.name}")
                                .semantics { selected = uiState.scoreInstrument == instrument }
                        )
                    }
                }
                OutlinedTextField(
                    value = uiState.scoreDateDraft,
                    onValueChange = viewModel::updateScoreDate,
                    label = { Text("Assessment date") },
                    supportingText = {
                        Text(uiState.scoreDateError ?: "YYYY-MM-DD · required · not later than today")
                    },
                    singleLine = true,
                    isError = uiState.scoreDateError != null,
                    modifier = Modifier.fillMaxWidth().testTag("score_date")
                )
                OutlinedTextField(
                    value = uiState.scoreTotalDraft,
                    onValueChange = viewModel::updateScoreTotal,
                    label = { Text("${uiState.scoreInstrument.visibleLabel} total from the external result") },
                    supportingText = {
                        Text(uiState.scoreTotalError ?: "Whole number 0–${uiState.scoreInstrument.maxTotal}")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = uiState.scoreTotalError != null,
                    modifier = Modifier.fillMaxWidth().testTag("score_total")
                )
                uiState.scoreFormError?.let { LiveMessage(it, isError = true, tag = "score_error") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = viewModel::saveScore,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("score_save")
                    ) { Text(if (uiState.editingScoreId == null) "Add total" else "Save changes") }
                    if (uiState.editingScoreId != null) {
                        TextButton(
                            onClick = viewModel::cancelScoreEdit,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("score_edit_cancel")
                        ) { Text("Cancel edit") }
                    }
                }
            }
        }
        if (uiState.scores.isEmpty()) {
            item(key = "scores_empty") {
                Text("No externally obtained totals stored.", modifier = Modifier.testTag("scores_empty"))
            }
        } else {
            item(key = "scores_title") { Text("Stored external totals", style = MaterialTheme.typography.titleMedium) }
            items(uiState.scores, key = { "score:${it.id}" }) { score ->
                Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp).testTag("score_row_${score.id}"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("${score.instrument.visibleLabel} total ${score.total}", style = MaterialTheme.typography.titleSmall)
                        Text(LocalDate.ofEpochDay(score.assessedEpochDay).toString())
                        Text(
                            "Entered by you from a result obtained elsewhere. MindScale did not administer or calculate it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { viewModel.editScore(score.id) },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("score_edit_${score.id}")
                            ) { Text("Edit") }
                            TextButton(
                                onClick = { viewModel.requestDeleteScore(score.id) },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("score_delete_${score.id}")
                            ) { Text("Delete") }
                        }
                    }
                }
            }
        }
        uiState.message?.let { message ->
            item(key = "profile_message:$message") { LiveMessage(message, isError = false, tag = "profile_message") }
        }
        uiState.error?.let { error ->
            item(key = "profile_error") {
                ProfileSection("Could not load Profile") {
                    LiveMessage(error, isError = true, tag = "profile_error")
                    TextButton(onClick = viewModel::retry) { Text("Retry") }
                }
            }
        }
    }

    if (pendingDelete != null) {
        MsDialog(
            onDismissRequest = viewModel::cancelDeleteScore,
            title = { Text("Delete external total?") },
            text = {
                Text(
                    "Delete ${pendingDelete.instrument.visibleLabel} total ${pendingDelete.total} from " +
                        "${LocalDate.ofEpochDay(pendingDelete.assessedEpochDay)}?"
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteScore, modifier = Modifier.testTag("score_delete_confirm")) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteScore, modifier = Modifier.testTag("score_delete_cancel")) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun LiveMessage(message: String, isError: Boolean, tag: String) {
    Text(
        message,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag(tag).semantics { liveRegion = LiveRegionMode.Polite }
    )
}
