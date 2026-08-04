package com.kieslingdev.mindscale.report

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.insights.InsightRange
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ReportRoute(
    viewModel: ReportProfileViewModel,
    onRangeSelected: (InsightRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val document = uiState.pendingDocument
        viewModel.documentPickerReturned()
        if (uri != null) {
            if (document == null) viewModel.documentWriteFailed()
            else scope.launch(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                        OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                            writer.write(document.text)
                            writer.flush()
                        }
                    } ?: error("Document provider returned no stream")
                }.onSuccess { viewModel.documentWriteSucceeded() }
                    .onFailure { viewModel.documentWriteFailed() }
            }
        }
    }
    val pending = uiState.pendingDocument
    LaunchedEffect(pending?.launchToken) {
        if (pending?.launchToken != null) launcher.launch(pending.filename)
    }
    ReportScreen(
        uiState = uiState,
        onRangeSelected = onRangeSelected,
        onCopy = {
            val report = uiState.report ?: return@ReportScreen
            runCatching {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("MindScale clinician summary", report.text)
                if (Build.VERSION.SDK_INT >= 33) {
                    clip.description.extras = PersistableBundle().apply {
                        putBoolean("android.content.extra.IS_SENSITIVE", true)
                    }
                }
                clipboard.setPrimaryClip(clip)
            }.onSuccess { viewModel.reportActionSucceeded("Clinician summary copied. It is now outside MindScale.") }
                .onFailure { viewModel.reportActionFailed("Could not copy the clinician summary.") }
        },
        onShare = {
            val report = uiState.report ?: return@ReportScreen
            runCatching {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, report.text)
                    putExtra(Intent.EXTRA_SUBJECT, "MindScale clinician summary")
                }
                context.startActivity(Intent.createChooser(intent, "Share clinician summary"))
            }.onSuccess { viewModel.reportActionSucceeded("Share chooser opened. MindScale does not know whether it was sent.") }
                .onFailure { viewModel.reportActionFailed("No app is available to share the clinician summary.") }
        },
        onSave = viewModel::requestSaveDocument,
        onDiscardPendingSave = viewModel::discardPendingDocument,
        onRetry = viewModel::retry,
        modifier = modifier
    )
}

@Composable
fun ReportScreen(
    uiState: ReportProfileUiState,
    onRangeSelected: (InsightRange) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onDiscardPendingSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.testTag("report_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "report_ranges") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Summary window", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InsightRange.entries.forEach { range ->
                        FilterChip(
                            selected = uiState.report?.range == range,
                            onClick = { onRangeSelected(range) },
                            label = { Text(range.shortLabel) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("report_range_${range.name}")
                                .semantics { selected = uiState.report?.range == range }
                        )
                    }
                }
            }
        }
        item(key = "privacy") {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                Text(
                    "This summary can contain sensitive health information. Nothing leaves MindScale until you choose Copy, Share, or Save.",
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        if (uiState.loading && uiState.report == null) {
            item(key = "loading") {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.testTag("report_loading"))
                }
            }
        }
        uiState.error?.let { error ->
            item(key = "report_error") {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                }
            }
        }
        uiState.report?.let { report ->
            item(key = "report_text:${report.generatedAt}:${report.range}") {
                SelectionContainer {
                    Text(
                        report.text,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth().testTag("report_text")
                    )
                }
            }
            uiState.pendingDocument?.takeIf { it.launchToken == null }?.let { pendingDocument ->
                item(key = "retained_report_document") {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                if (pendingDocument.text == report.text) {
                                    "A captured summary is retained. Save as text retries those exact captured bytes."
                                } else {
                                    "A previously captured summary is retained and differs from the summary now shown. " +
                                        "Save as text retries the previous text."
                                }
                            )
                            TextButton(
                                onClick = onDiscardPendingSave,
                                modifier = Modifier.heightIn(min = 48.dp).testTag("report_discard_pending")
                            ) { Text("Discard retained save") }
                        }
                    }
                }
            }
            item(key = "report_actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("report_actions"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onCopy, modifier = Modifier.heightIn(min = 48.dp).testTag("report_copy")) {
                        Text("Copy")
                    }
                    TextButton(onClick = onShare, modifier = Modifier.heightIn(min = 48.dp).testTag("report_share")) {
                        Text("Share")
                    }
                    TextButton(onClick = onSave, modifier = Modifier.heightIn(min = 48.dp).testTag("report_save")) {
                        Text("Save as text")
                    }
                }
            }
        }
        uiState.message?.let { message ->
            item(key = "report_message:$message") {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("report_message").semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
    }
}
