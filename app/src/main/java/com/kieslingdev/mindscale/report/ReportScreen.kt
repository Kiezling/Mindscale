package com.kieslingdev.mindscale.report

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.insights.InsightRange
import com.kieslingdev.mindscale.ui.components.MsCard
import com.kieslingdev.mindscale.ui.components.MsChip
import com.kieslingdev.mindscale.ui.components.MsEyebrow
import com.kieslingdev.mindscale.ui.components.MsTextAction
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Below this width, two metric columns leave too little room for values such as intensity-hours.
private val ReportMetricsSingleColumnWidth = 320.dp

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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(MsSpacing.lgPlus),
        verticalArrangement = Arrangement.spacedBy(MsSpacing.lg)
    ) {
        item(key = "report_ranges") {
            Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.xs)) {
                MsEyebrow("Summary window")
                // Six ranges stay a scrolling chip row rather than becoming a segmented control:
                // six equal segments at 200% font would leave about 55 dp for `90 days`. The
                // design's own range control on Insights is a chip row, and this is that control
                // on a different screen (D-6).
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm)
                ) {
                    InsightRange.entries.forEach { range ->
                        MsChip(
                            text = range.shortLabel,
                            selected = uiState.report?.range == range,
                            onClick = { onRangeSelected(range) },
                            modifier = Modifier
                                .testTag("report_range_${range.name}")
                                .semantics { selected = uiState.report?.range == range }
                        )
                    }
                }
            }
        }
        item(key = "privacy") {
            // `emphasized` is the design's gold-bordered card, which is exactly "this one
            // matters". The sentence is about sensitive health information leaving the app.
            MsCard(emphasized = true, contentPadding = MsSpacing.mdPlus) {
                Text(
                    "This summary can contain sensitive health information. Nothing leaves MindScale until you choose Copy, Share, or Save.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.ms.inkSecondary,
                    modifier = Modifier.fillMaxWidth()
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
                MsCard(contentPadding = MsSpacing.mdPlus) {
                    Text(
                        error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.ms.danger
                    )
                    MsTextAction(text = "Retry", onClick = onRetry)
                }
            }
        }
        uiState.report?.let { report ->
            val presentation = report.presentation
            item(key = "report_overview:${report.generatedAt}:${report.range}") {
                Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.sm)) {
                    MsEyebrow("Selected dates")
                    Text(
                        presentation.rangeText,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.ms.inkPrimary,
                        modifier = Modifier.testTag("report_dates")
                    )
                    presentation.name?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    MsCard(contentPadding = MsSpacing.mdPlus) {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val columns = if (maxWidth < ReportMetricsSingleColumnWidth || LocalDensity.current.fontScale >= 1.5f) 1 else 2
                            Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.md)) {
                                presentation.metrics.chunked(columns).forEach { metrics ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(MsSpacing.md)) {
                                        metrics.forEach { metric ->
                                            Column(Modifier.weight(1f)) {
                                                Text(metric.label, style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.ms.inkSecondary)
                                                Text(metric.value, style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.ms.inkPrimary)
                                            }
                                        }
                                        if (metrics.size < columns) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item(key = "report_course") {
                ReportSection("Recorded course", "report_course") {
                    if (presentation.ratings.isEmpty()) {
                        Text("No ratings were recorded in this window.")
                    } else {
                        Text("Latest ${presentation.ratings.size} of ${presentation.ratingCount} ratings · 0–10",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ms.inkSecondary)
                        presentation.ratings.forEach { rating ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm)) {
                                Text(rating.time, style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1.5f))
                                LinearProgressIndicator(
                                    progress = { rating.value / 10f },
                                    modifier = Modifier.weight(1f),
                                    drawStopIndicator = {}
                                )
                                Text("${rating.value}/10", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            item(key = "report_episodes") {
                ReportSection("Episodes and starts", "report_episodes") {
                    Text(presentation.episodeDetail, style = MaterialTheme.typography.bodyMedium)
                    Text(presentation.onsetDetail, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item(key = "report_events") {
                ReportSection("Events marked", "report_events") {
                    if (presentation.events.isEmpty()) Text("No events were marked in this window.")
                    else presentation.events.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    if (presentation.omittedEvents > 0) Text("${presentation.omittedEvents} additional marked events not shown.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            item(key = "report_sleep") {
                ReportSection("Sleep", "report_sleep") {
                    Text(presentation.sleepDetail, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (presentation.scores.isNotEmpty()) item(key = "report_scores") {
                ReportSection("Externally obtained totals", "report_scores") {
                    presentation.scores.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    if (presentation.omittedScores > 0) Text("${presentation.omittedScores} additional totals not shown.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("MindScale did not administer or calculate these totals.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.ms.inkSecondary)
                }
            }
            item(key = "report_context") {
                Text(presentation.context + " This is a record summary, not a clinical assessment. Review the underlying records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.ms.inkSecondary,
                    modifier = Modifier.testTag("report_context"))
            }
            uiState.pendingDocument?.takeIf { it.launchToken == null }?.let { pendingDocument ->
                item(key = "retained_report_document") {
                    MsCard(contentPadding = MsSpacing.mdPlus) {
                        Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.xs)) {
                            Text(
                                if (pendingDocument.text == report.text) {
                                    "A captured summary is retained. Save as text retries those exact captured bytes."
                                } else {
                                    "A previously captured summary is retained and differs from the summary now shown. " +
                                        "Save as text retries the previous text."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.ms.inkSecondary
                            )
                            MsTextAction(
                                text = "Discard retained save",
                                onClick = onDiscardPendingSave,
                                modifier = Modifier.testTag("report_discard_pending")
                            )
                        }
                    }
                }
            }
            item(key = "report_actions") {
                // The three actions on one baseline with even gaps — the L-2 treatment Phase 16
                // froze for Track's entry rows, applied to the same shape of problem (D-10).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("report_actions"),
                    horizontalArrangement = Arrangement.spacedBy(MsSpacing.lgPlus),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MsTextAction(
                        text = "Copy",
                        onClick = onCopy,
                        modifier = Modifier.testTag("report_copy")
                    )
                    MsTextAction(
                        text = "Share",
                        onClick = onShare,
                        modifier = Modifier.testTag("report_share")
                    )
                    MsTextAction(
                        text = "Save as text",
                        onClick = onSave,
                        modifier = Modifier.testTag("report_save")
                    )
                }
            }
        }
        uiState.message?.let { message ->
            item(key = "report_message:$message") {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.ms.goldText,
                    modifier = Modifier.testTag("report_message").semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
    }
}

@Composable
private fun ReportSection(title: String, tag: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.sm), modifier = Modifier.testTag(tag)) {
        MsEyebrow(title)
        MsCard(contentPadding = MsSpacing.mdPlus) {
            Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.ms.inkSecondary) {
                    content()
                }
            }
        }
    }
}
