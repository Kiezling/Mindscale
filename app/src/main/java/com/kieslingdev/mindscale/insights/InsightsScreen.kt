package com.kieslingdev.mindscale.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
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
import kotlin.math.abs
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
        onExploreChart = viewModel::exploreChart,
        onEarlierChartHour = { viewModel.moveChartHour(-1) },
        onLaterChartHour = { viewModel.moveChartHour(1) },
        onPreviousRating = { viewModel.moveChartRating(-1) },
        onNextRating = { viewModel.moveChartRating(1) },
        onPreviousEvent = { viewModel.moveChartMarker(-1) },
        onNextEvent = { viewModel.moveChartMarker(1) },
        onSelectOnsetGapBucket = viewModel::selectOnsetGapBucket,
        onSelectOnsetHour = viewModel::selectOnsetHour,
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
    onExploreChart: (Long) -> Unit,
    onEarlierChartHour: () -> Boolean,
    onLaterChartHour: () -> Boolean,
    onPreviousRating: () -> Boolean,
    onNextRating: () -> Boolean,
    onPreviousEvent: () -> Boolean,
    onNextEvent: () -> Boolean,
    onSelectOnsetGapBucket: (Int) -> Unit,
    onSelectOnsetHour: (Int) -> Unit,
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
            item(key = "entry_chart") {
                val chartReadout = uiState.chartExploredInstantMillis?.let {
                    entryChartReadout(
                        chart = snapshot.entryChart,
                        instantMillis = it,
                        hourFormat = uiState.hourFormat,
                        zoneId = zoneId,
                        hideNotes = uiState.hideNotes
                    )
                } ?: "Touch or drag to read the chart"
                EntryChartSection(
                    chart = snapshot.entryChart,
                    selectedInstantMillis = uiState.chartExploredInstantMillis,
                    readout = chartReadout,
                    holdHours = uiState.holdDuration.hours,
                    range = uiState.range,
                    hourFormat = uiState.hourFormat,
                    zoneId = zoneId,
                    onExplore = onExploreChart,
                    onEarlierHour = onEarlierChartHour,
                    onLaterHour = onLaterChartHour,
                    onPreviousRating = onPreviousRating,
                    onNextRating = onNextRating,
                    onPreviousEvent = onPreviousEvent,
                    onNextEvent = onNextEvent
                )
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
            item(key = "onset_gap_histogram") {
                OnsetGapSection(
                    histogram = snapshot.onsetGapHistogram,
                    selectedBucketIndex = uiState.selectedOnsetGapBucketIndex,
                    onSelectBucket = onSelectOnsetGapBucket
                )
            }
            item(key = "onset_time_counts") {
                OnsetTimeSection(
                    counts = snapshot.onsetTimeCounts,
                    selectedHour = uiState.selectedOnsetHour,
                    hourFormat = uiState.hourFormat,
                    onSelectHour = onSelectOnsetHour
                )
            }
        }
    }
}

@Composable
private fun OnsetGapSection(
    histogram: OnsetGapHistogram,
    selectedBucketIndex: Int?,
    onSelectBucket: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Days between onsets", style = MaterialTheme.typography.titleMedium)
        if (!histogram.isEligible) {
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().testTag("onset_gap_refusal")
            ) {
                Text(
                    onsetGapRefusalText(histogram),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        Text(onsetGapDenominator(histogram), style = MaterialTheme.typography.bodySmall)
        val maximumCount = histogram.buckets.maxOfOrNull(OnsetGapBucket::count)?.coerceAtLeast(1) ?: 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("onset_gap_bars"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            histogram.buckets.forEach { bucket ->
                val isSelected = selectedBucketIndex == bucket.index
                val barHeight = if (bucket.count == 0) 0.dp else {
                    (80f * bucket.count / maximumCount).coerceAtLeast(4f).dp
                }
                val description = "${bucket.visibleLabel} bucket, ${bucket.count} of " +
                    "${histogram.gapCount} onset-to-onset gaps, ${bucket.spokenBoundary}"
                Surface(
                    onClick = { onSelectBucket(bucket.index) },
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .width(72.dp)
                        .heightIn(min = 152.dp)
                        .testTag("onset_gap_bucket_${bucket.index}")
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            selected = isSelected
                            contentDescription = description
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(bucket.count.toString(), style = MaterialTheme.typography.labelLarge)
                        Box(
                            modifier = Modifier.fillMaxWidth().height(88.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                Modifier
                                    .width(28.dp)
                                    .height(barHeight)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clearAndSetSemantics { }
                            )
                        }
                        Text(
                            bucket.visibleLabel,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        Text(
            selectedBucketIndex?.let { onsetGapBucketReadout(histogram, it) }
                ?: "Select a bar to read its exact count.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("onset_gap_readout").semantics {
                liveRegion = LiveRegionMode.Polite
            }
        )
        Text(
            ONSET_GAP_CAVEAT,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnsetTimeSection(
    counts: OnsetTimeCounts,
    selectedHour: Int?,
    hourFormat: HourFormat,
    onSelectHour: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Time of day it started", style = MaterialTheme.typography.titleMedium)
        if (!counts.isEligible) {
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().testTag("onset_time_refusal")
            ) {
                Text(
                    onsetTimeRefusalText(counts),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        Text(onsetTimeDenominator(counts), style = MaterialTheme.typography.bodySmall)
        val maximumCount = counts.buckets.maxOfOrNull(OnsetHourBucket::count)?.coerceAtLeast(1) ?: 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("onset_time_bars"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            counts.buckets.forEach { bucket ->
                val isSelected = selectedHour == bucket.hourOfDay
                val barHeight = if (bucket.count == 0) 0.dp else {
                    (72f * bucket.count / maximumCount).coerceAtLeast(4f).dp
                }
                val hourLabel = onsetHourVisibleLabel(bucket.hourOfDay, hourFormat)
                val description = "${onsetHourSpokenLabel(bucket.hourOfDay, hourFormat)} hour, " +
                    "${bucket.count} of ${counts.eligibleOnsetCount} " +
                    "recorded starts, ${onsetHourBoundary(bucket.hourOfDay, hourFormat)}"
                Surface(
                    onClick = { onSelectHour(bucket.hourOfDay) },
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier
                        .width(64.dp)
                        .heightIn(min = 144.dp)
                        .testTag("onset_time_hour_${bucket.hourOfDay}")
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            selected = isSelected
                            contentDescription = description
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(bucket.count.toString(), style = MaterialTheme.typography.labelLarge)
                        Box(
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                Modifier
                                    .width(26.dp)
                                    .height(barHeight)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clearAndSetSemantics { }
                            )
                        }
                        Text(hourLabel, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        Text(onsetTimeFourHourSentence(counts, hourFormat), style = MaterialTheme.typography.bodyMedium)
        Text(
            selectedHour?.let { onsetTimeBucketReadout(counts, it, hourFormat) }
                ?: "Select an hour to read its exact count.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("onset_time_readout").semantics {
                liveRegion = LiveRegionMode.Polite
            }
        )
        Text(
            ONSET_TIME_CAVEAT,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
private fun EntryChartSection(
    chart: EntryChart,
    selectedInstantMillis: Long?,
    readout: String,
    holdHours: Int,
    range: InsightRange,
    hourFormat: HourFormat,
    zoneId: ZoneId,
    onExplore: (Long) -> Unit,
    onEarlierHour: () -> Boolean,
    onLaterHour: () -> Boolean,
    onPreviousRating: () -> Boolean,
    onNextRating: () -> Boolean,
    onPreviousEvent: () -> Boolean,
    onNextEvent: () -> Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("What you recorded", style = MaterialTheme.typography.titleMedium)
        Text(
            readout,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        EntryStepChart(
            chart = chart,
            selectedInstantMillis = selectedInstantMillis,
            readout = readout,
            range = range,
            hourFormat = hourFormat,
            zoneId = zoneId,
            onExplore = onExplore,
            onEarlierHour = onEarlierHour,
            onLaterHour = onLaterHour,
            onPreviousRating = onPreviousRating,
            onNextRating = onNextRating,
            onPreviousEvent = onPreviousEvent,
            onNextEvent = onNextEvent
        )
        EntryChartLegend()
        Text(
            "Ratings stay flat until another rating or the ${holdHours}h waking-hour limit. " +
                "The line stops during sleep. Dotted lines are events you marked.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EntryStepChart(
    chart: EntryChart,
    selectedInstantMillis: Long?,
    readout: String,
    range: InsightRange,
    hourFormat: HourFormat,
    zoneId: ZoneId,
    onExplore: (Long) -> Unit,
    onEarlierHour: () -> Boolean,
    onLaterHour: () -> Boolean,
    onPreviousRating: () -> Boolean,
    onNextRating: () -> Boolean,
    onPreviousEvent: () -> Boolean,
    onNextEvent: () -> Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val area = primary.copy(alpha = 0.14f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val sleep = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val sleepStripe = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val event = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    val selection = MaterialTheme.colorScheme.onSurface
    val span = (chart.endMillis - chart.startMillis).coerceAtLeast(1L)

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("entry_chart")
            .semantics {
                contentDescription = "Recorded intensity step chart"
                stateDescription = readout
                customActions = listOf(
                    CustomAccessibilityAction("Earlier chart hour", onEarlierHour),
                    CustomAccessibilityAction("Later chart hour", onLaterHour),
                    CustomAccessibilityAction("Previous recorded change", onPreviousRating),
                    CustomAccessibilityAction("Next recorded change", onNextRating),
                    CustomAccessibilityAction("Previous event", onPreviousEvent),
                    CustomAccessibilityAction("Next event", onNextEvent)
                )
            }
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.width(24.dp).height(180.dp).clearAndSetSemantics { },
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("10", style = MaterialTheme.typography.labelSmall)
                    Text("5", style = MaterialTheme.typography.labelSmall)
                    Text("0", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.width(6.dp))
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(180.dp)
                        .pointerInput(chart) {
                            detectTapGestures { offset ->
                                onExplore(
                                    chartInstantFromPosition(
                                        x = offset.x,
                                        width = size.width.toFloat(),
                                        chart = chart,
                                        eventSnapPixels = 24.dp.toPx()
                                    )
                                )
                            }
                        }
                        .pointerInput(chart) {
                            detectHorizontalDragGestures { change, _ ->
                                onExplore(
                                    chartInstantFromPosition(
                                        x = change.position.x,
                                        width = size.width.toFloat(),
                                        chart = chart,
                                        eventSnapPixels = 24.dp.toPx()
                                    )
                                )
                            }
                        }
                        .clearAndSetSemantics { }
                ) {
                    fun xOf(millis: Long): Float =
                        size.width * ((millis - chart.startMillis).toDouble() / span).toFloat().coerceIn(0f, 1f)
                    fun yOf(value: Int): Float = size.height * (1f - value.coerceIn(0, 10) / 10f)

                    chart.sleeps.forEach { band ->
                        val left = xOf(band.startMillis)
                        val right = xOf(band.endMillis)
                        drawRect(sleep, Offset(left, 0f), Size((right - left).coerceAtLeast(1f), size.height))
                        var stripeX = left - size.height
                        while (stripeX < right) {
                            drawLine(
                                color = sleepStripe,
                                start = Offset(stripeX, size.height),
                                end = Offset(stripeX + size.height, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                            stripeX += 14.dp.toPx()
                        }
                    }
                    listOf(10, 5, 0).forEach { value ->
                        drawLine(grid, Offset(0f, yOf(value)), Offset(size.width, yOf(value)), 1.dp.toPx())
                    }
                    chart.markers.forEach { marker ->
                        val x = xOf(marker.atMillis)
                        drawLine(
                            color = event,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))
                        )
                    }
                    chart.segments.forEachIndexed { index, segment ->
                        val left = xOf(segment.startMillis)
                        val right = xOf(segment.endMillis)
                        val y = yOf(segment.value)
                        if (segment.value > 0) {
                            drawRect(area, Offset(left, y), Size((right - left).coerceAtLeast(1f), size.height - y))
                        }
                        drawLine(
                            color = primary,
                            start = Offset(left, y),
                            end = Offset(right, y),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        val next = chart.segments.getOrNull(index + 1)
                        if (next != null && next.startMillis == segment.endMillis) {
                            drawLine(
                                color = primary,
                                start = Offset(right, y),
                                end = Offset(right, yOf(next.value)),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    selectedInstantMillis?.let { selectedMillis ->
                        val clamped = selectedMillis.coerceIn(chart.startMillis, chart.endMillis)
                        val x = xOf(clamped)
                        drawLine(selection, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                        val reading = chart.readingAt(clamped)
                        if (reading.state == EntryChartState.WELL || reading.state == EntryChartState.INTENSITY) {
                            drawCircle(selection, 4.dp.toPx(), Offset(x, yOf(reading.value ?: 0)))
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 30.dp).clearAndSetSemantics { },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                entryChartTicks(chart, range, hourFormat, zoneId).forEachIndexed { index, label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        textAlign = when (index) {
                            0 -> TextAlign.Start
                            3 -> TextAlign.End
                            else -> TextAlign.Center
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryChartLegend() {
    val items = listOf(
        Triple("recorded intensity", MaterialTheme.colorScheme.primary, false),
        Triple("asleep", MaterialTheme.colorScheme.outlineVariant, false),
        Triple("event", MaterialTheme.colorScheme.onSurfaceVariant, true)
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items.forEach { (label, color, dotted) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(18.dp, 10.dp).clearAndSetSemantics { }) {
                    if (dotted) {
                        drawLine(
                            color,
                            Offset(size.width / 2f, 0f),
                            Offset(size.width / 2f, size.height),
                            1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))
                        )
                    } else drawRect(color)
                }
                Spacer(Modifier.width(5.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun chartInstantFromPosition(
    x: Float,
    width: Float,
    chart: EntryChart,
    eventSnapPixels: Float
): Long {
    val safeWidth = width.coerceAtLeast(1f)
    val fraction = (x / safeWidth).coerceIn(0f, 0.999999f)
    val raw = chart.startMillis + ((chart.endMillis - chart.startMillis) * fraction).roundToLong()
    val closest = chart.markers.minByOrNull { marker ->
        abs((marker.atMillis - chart.startMillis).toDouble() / (chart.endMillis - chart.startMillis).coerceAtLeast(1L) * safeWidth - x)
    }
    val markerDistance = closest?.let { marker ->
        abs((marker.atMillis - chart.startMillis).toDouble() / (chart.endMillis - chart.startMillis).coerceAtLeast(1L) * safeWidth - x)
    }
    return if (closest != null && markerDistance != null && markerDistance <= eventSnapPixels) closest.atMillis else raw
}

private fun entryChartTicks(
    chart: EntryChart,
    range: InsightRange,
    hourFormat: HourFormat,
    zoneId: ZoneId
): List<String> = (0..3).map { index ->
    val millis = chart.startMillis + (chart.endMillis - chart.startMillis) * index / 3L
    val zoned = Instant.ofEpochMilli(millis).atZone(zoneId)
    val pattern = when (range) {
        InsightRange.ONE_DAY -> if (hourFormat == HourFormat.TWENTY_FOUR) "HH:mm" else "h a"
        InsightRange.THREE_DAYS -> if (hourFormat == HourFormat.TWENTY_FOUR) "M/d HH" else "M/d h a"
        else -> "M/d"
    }
    DateTimeFormatter.ofPattern(pattern).format(zoned)
}

private fun entryChartReadout(
    chart: EntryChart,
    instantMillis: Long,
    hourFormat: HourFormat,
    zoneId: ZoneId,
    hideNotes: Boolean
): String {
    val instant = instantMillis.coerceIn(chart.startMillis, chart.endMillis)
    val reading = chart.readingAt(instant)
    val parts = mutableListOf(formatDateTime(instant, hourFormat, zoneId))
    parts += when (reading.state) {
        EntryChartState.NO_DATA -> "no data yet"
        EntryChartState.WELL -> "nothing recorded"
        EntryChartState.INTENSITY -> "intensity ${reading.value}"
        EntryChartState.ASLEEP -> "asleep"
    }
    reading.sourceEntryMillis?.let { parts += "recorded ${formatDateTime(it, hourFormat, zoneId)}" }
    if (reading.chips.isNotEmpty()) parts += reading.chips.joinToString(", ")
    if (!hideNotes) reading.note?.takeIf(String::isNotBlank)?.let { note ->
        parts += if (note.length <= 120) note else note.take(120) + "…"
    }
    reading.markers.forEach { marker -> parts += "event: ${marker.text.ifBlank { "event" }}" }
    return parts.joinToString(" · ")
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
