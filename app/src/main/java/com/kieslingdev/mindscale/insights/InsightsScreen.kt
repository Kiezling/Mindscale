package com.kieslingdev.mindscale.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.ui.theme.intensityColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToLong

@Composable
fun InsightsRoute(viewModel: InsightsViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshTime() }
    InsightsScreen(
        uiState = uiState,
        onRangeSelected = viewModel::selectRange,
        onExplore = viewModel::explore,
        onEarlierHour = { viewModel.moveExplorationHour(-1) },
        onLaterHour = { viewModel.moveExplorationHour(1) },
        onPreviousDay = { viewModel.moveExplorationDay(-1) },
        onNextDay = { viewModel.moveExplorationDay(1) },
        onRetry = viewModel::retry,
        modifier = modifier
    )
}

@Composable
fun InsightsScreen(
    uiState: InsightsUiState,
    onRangeSelected: (InsightRange) -> Unit,
    onExplore: (Long) -> Unit,
    onEarlierHour: () -> Boolean,
    onLaterHour: () -> Boolean,
    onPreviousDay: () -> Boolean,
    onNextDay: () -> Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault()
) {
    LazyColumn(
        modifier = modifier.testTag("insights_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "ranges") {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InsightRange.entries.forEach { range ->
                    FilterChip(
                        selected = uiState.range == range,
                        onClick = { onRangeSelected(range) },
                        label = { Text(range.shortLabel) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("insight_range_${range.name}")
                            .semantics {
                                contentDescription = range.spokenLabel
                                selected = uiState.range == range
                            }
                    )
                }
            }
        }

        uiState.error?.let { error ->
            item(key = "error") {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(error, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                }
            }
        }

        if (uiState.loading && uiState.snapshot == null) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.testTag("insights_loading"))
                }
            }
        }

        val snapshot = uiState.snapshot
        if (snapshot != null && !snapshot.hasEntries) {
            item(key = "empty") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("insights_empty")) {
                    Text("Nothing to draw yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "This page shows only what you recorded — no estimates and no guesses. It fills in as you log.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (snapshot != null) {
            if (!snapshot.hasRangeData) {
                item(key = "range_empty") {
                    Text("No ratings in this range", modifier = Modifier.testTag("insights_range_empty"))
                }
            }
            item(key = "summary") {
                SummaryStrip(snapshot.summary)
            }
            item(key = "raster") {
                val readout = uiState.exploredInstantMillis?.let {
                    rasterReadout(snapshot, it, uiState.hourFormat, zoneId)
                } ?: "Touch or drag to read a day and hour"
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Days and hours", style = MaterialTheme.typography.titleMedium)
                        Text(
                            readout,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                    RasterChart(
                        snapshot = snapshot,
                        readout = readout,
                        onExplore = onExplore,
                        onEarlierHour = onEarlierHour,
                        onLaterHour = onLaterHour,
                        onPreviousDay = onPreviousDay,
                        onNextDay = onNextDay
                    )
                    RasterLegend()
                    Text(
                        "One row per local day. Plain space is awake time with nothing recorded; sleep pauses symptom time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item(key = "facts_title") { Text("Episodes", style = MaterialTheme.typography.titleMedium) }
            items(snapshot.facts.size, key = { "fact:$it" }) { index ->
                val fact = snapshot.facts[index]
                Surface(tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(fact.text)
                        fact.detail?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (snapshot.recentEpisodes.isNotEmpty()) {
                item(key = "recent_title") { Text("Each episode", style = MaterialTheme.typography.titleMedium) }
                items(snapshot.recentEpisodes.size, key = { "episode:${snapshot.recentEpisodes[it].onsetMillis}" }) { index ->
                    EpisodeRow(snapshot.recentEpisodes[index], uiState.hourFormat, zoneId)
                }
            }
        }
    }
}

@Composable
private fun SummaryStrip(summary: InsightSummary) {
    val cells = listOf(
        "Episodes" to summary.episodeCount.toString(),
        "Typical length" to (summary.typicalLengthMillis?.let(::formatDuration) ?: "—"),
        "Clear days" to "${summary.clearDays}/${summary.eligibleDays}",
        "Peak" to (summary.peak?.toString() ?: "—")
    )
    Row(
        modifier = Modifier.fillMaxWidth().testTag("insights_summary"),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        cells.forEach { (label, value) ->
            Column(
                modifier = Modifier.weight(1f).semantics { contentDescription = "$label, $value" },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(value, style = MaterialTheme.typography.titleLarge)
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun RasterChart(
    snapshot: InsightsSnapshot,
    readout: String,
    onExplore: (Long) -> Unit,
    onEarlierHour: () -> Boolean,
    onLaterHour: () -> Boolean,
    onPreviousDay: () -> Boolean,
    onNextDay: () -> Boolean
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val noDataColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val wellColor = MaterialTheme.colorScheme.surface
    val asleepColor = MaterialTheme.colorScheme.outlineVariant
    val futureColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) 0.14f else 0.08f)
    val futureStripeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val rowHeight = when {
        snapshot.rasterDays.size <= 7 -> 20.dp
        snapshot.rasterDays.size <= 30 -> 10.dp
        snapshot.rasterDays.size <= 90 -> 6.dp
        else -> 4.dp
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("raster_chart")
            .semantics {
                contentDescription = "Day and hour symptom raster"
                stateDescription = readout
                customActions = listOf(
                    CustomAccessibilityAction("Earlier hour", onEarlierHour),
                    CustomAccessibilityAction("Later hour", onLaterHour),
                    CustomAccessibilityAction("Previous day", onPreviousDay),
                    CustomAccessibilityAction("Next day", onNextDay)
                )
            }
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            snapshot.rasterDays.forEachIndexed { index, day ->
                key(day.date) { Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (snapshot.rasterDays.size <= 30 || index == 0 || index == snapshot.rasterDays.lastIndex || day.date.dayOfWeek.value == 1) {
                            day.date.format(DateTimeFormatter.ofPattern("MMM d"))
                        } else "",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(44.dp).clearAndSetSemantics { }
                    )
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(rowHeight)
                            .background(noDataColor)
                            .pointerInput(day) {
                                detectTapGestures { offset ->
                                    val fraction = (offset.x / size.width).coerceIn(0f, 0.99999f)
                                    onExplore(day.startMillis + ((day.endMillis - day.startMillis) * fraction).roundToLong())
                                }
                            }
                            .pointerInput(day) {
                                detectHorizontalDragGestures { change, _ ->
                                    val fraction = (change.position.x / size.width).coerceIn(0f, 0.99999f)
                                    onExplore(day.startMillis + ((day.endMillis - day.startMillis) * fraction).roundToLong())
                                }
                            }
                            .clearAndSetSemantics { }
                    ) {
                        day.segments.forEach { segment ->
                            val color = when (segment.state) {
                                RasterState.NO_DATA -> noDataColor
                                RasterState.WELL -> wellColor
                                RasterState.ASLEEP -> asleepColor
                                RasterState.FUTURE -> futureColor
                                RasterState.INTENSITY -> intensityColor(segment.intensity ?: 0, dark)
                            }
                            drawRect(
                                color = color,
                                topLeft = androidx.compose.ui.geometry.Offset(size.width * segment.startFraction, 0f),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width * (segment.endFraction - segment.startFraction),
                                    size.height
                                )
                            )
                            if (segment.state == RasterState.FUTURE) {
                                val startX = size.width * segment.startFraction
                                val endX = size.width * segment.endFraction
                                var x = startX - size.height
                                while (x < endX) {
                                    drawLine(
                                        color = futureStripeColor,
                                        start = androidx.compose.ui.geometry.Offset(x, size.height),
                                        end = androidx.compose.ui.geometry.Offset(x + size.height, 0f),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    x += size.height
                                }
                            }
                        }
                    }
                } }
            }
        }
    }
}

@Composable
private fun RasterLegend() {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val items = listOf(
        "nothing" to MaterialTheme.colorScheme.surface,
        "1" to intensityColor(1, dark),
        "10" to intensityColor(10, dark),
        "asleep" to MaterialTheme.colorScheme.outlineVariant,
        "no data" to MaterialTheme.colorScheme.surfaceVariant,
        "future" to MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) 0.14f else 0.08f)
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp, 9.dp).background(color))
                Spacer(Modifier.width(5.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: DerivedEpisode, hourFormat: HourFormat, zoneId: ZoneId) {
    val onset = formatDateTime(episode.onsetMillis, hourFormat, zoneId)
    val status = when (episode.endReason) {
        EpisodeEndReason.EXPLICIT_ZERO -> "recorded end"
        EpisodeEndReason.ASSUMED_HOLD -> "end assumed"
        EpisodeEndReason.ONGOING -> "ongoing"
    }
    val detail = buildString {
        append(if (episode.endReason == EpisodeEndReason.ONGOING) "ongoing" else "${formatDuration(episode.awakeDurationMillis)} awake")
        if (episode.sleepCount > 0) append(" · slept ${episode.sleepCount}×")
        if (episode.chips.isNotEmpty()) append(" · ${episode.chips.joinToString(", ")}")
        if (episode.endReason != EpisodeEndReason.ONGOING) append(" · $status")
    }
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.semantics {
            contentDescription = "$onset, peak ${episode.peak}, $detail"
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(onset, style = MaterialTheme.typography.titleSmall)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(episode.peak.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun rasterReadout(
    snapshot: InsightsSnapshot,
    instantMillis: Long,
    hourFormat: HourFormat,
    zoneId: ZoneId
): String {
    val (state, intensity) = snapshot.rasterStateAt(instantMillis)
    val stateText = when (state) {
        RasterState.NO_DATA -> "no data"
        RasterState.WELL -> "nothing recorded"
        RasterState.ASLEEP -> "asleep"
        RasterState.FUTURE -> "future"
        RasterState.INTENSITY -> "intensity $intensity"
    }
    return "${formatDateTime(instantMillis, hourFormat, zoneId)} · $stateText"
}

private fun formatDateTime(millis: Long, hourFormat: HourFormat, zoneId: ZoneId): String {
    val zoned = Instant.ofEpochMilli(millis).atZone(zoneId)
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(zoned)
    val timePattern = if (hourFormat == HourFormat.TWENTY_FOUR) "HH:mm XXX" else "h:mm a XXX"
    return "$date · ${DateTimeFormatter.ofPattern(timePattern).format(zoned)}"
}
