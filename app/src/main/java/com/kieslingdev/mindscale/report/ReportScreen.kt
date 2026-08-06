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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
            item(key = "report_text:${report.generatedAt}:${report.range}") {
                MsCard(contentPadding = MsSpacing.lg) {
                    SelectionContainer {
                        Text(
                            report.text,
                            style = MaterialTheme.typography.bodyMedium,
                            // The one deliberate refusal of Instrument Sans in the app. This is a
                            // fixed-width document whose alignment carries meaning, it is
                            // selectable, and it is the exact byte sequence Copy, Share and Save
                            // hand out. A proportional face would misrepresent what the user is
                            // about to send (D-10).
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.ms.inkPrimary,
                            modifier = Modifier.fillMaxWidth().testTag("report_text")
                        )
                    }
                }
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
