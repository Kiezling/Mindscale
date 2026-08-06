package com.kieslingdev.mindscale.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.ui.components.MsCard
import com.kieslingdev.mindscale.ui.components.MsChip
import com.kieslingdev.mindscale.ui.components.MsEyebrow
import com.kieslingdev.mindscale.ui.components.MsHairline
import com.kieslingdev.mindscale.ui.components.MsPillButton
import com.kieslingdev.mindscale.ui.components.MsUppercaseText
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.intensityColor
import com.kieslingdev.mindscale.ui.theme.ms
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.roundToLong

/*
 * The design's Insights screen, per `docs/specs/SPEC-insights-visual.md`.
 *
 * Every dimension below is either an `MsSpacing` reference or one of the named constants here,
 * per D-17. These are chart geometry rather than spacing, and a spacing scale should not absorb
 * them: they exist because a raster row, a plot area, a bar well and a legend swatch have sizes of
 * their own that no other screen shares.
 */

/** Raster row heights, by how many days the range covers. Unchanged from Phase 5. */
private val RasterRowHeightWeek: Dp = 20.dp
private val RasterRowHeightMonth: Dp = 10.dp
private val RasterRowHeightQuarter: Dp = 6.dp
private val RasterRowHeightLong: Dp = 4.dp

/** The raster's date gutter. Wider than the design's 30 px because `MMM d` is 9 sp here, not 7.5. */
private val RasterDateColumnWidth: Dp = 44.dp

/** The entry chart's plot area and its axis gutter. */
private val ChartPlotHeight: Dp = 180.dp
private val ChartAxisColumnWidth: Dp = 24.dp
private val ChartTickRowInset: Dp = 30.dp

/** How near a marker a touch must land before the chart snaps its readout to that event. */
private val ChartEventSnapRadius: Dp = 24.dp

/** The step line's stroke, its selection dot, its dash pitch, and the sleep hatching pitch. */
private val ChartStrokeWidth: Dp = 2.dp
private val ChartHairline: Dp = 1.dp
private val ChartSelectionDotRadius: Dp = 4.dp
private val ChartDashPitch: Dp = 5.dp
private val LegendDashPitch: Dp = 2.dp

/** Legend swatches: the design's 16 x 9 raster swatch and the chart's slightly larger mark. */
private val RasterSwatchWidth: Dp = 16.dp
private val RasterSwatchHeight: Dp = 9.dp
private val ChartSwatchWidth: Dp = 18.dp
private val ChartSwatchHeight: Dp = 10.dp

/** Gap-histogram cell geometry. Ten buckets, so the cell is wider than the onset hour's. */
private val GapCellWidth: Dp = 72.dp
private val GapCellMinHeight: Dp = 152.dp
private val GapBarWellHeight: Dp = 88.dp
private val GapBarWidth: Dp = 28.dp
private const val GapBarMaxHeight = 80f

/** Onset-hour cell geometry. Twenty-four buckets, so the cell is narrower. */
private val HourCellWidth: Dp = 64.dp
private val HourCellMinHeight: Dp = 144.dp
private val HourBarWellHeight: Dp = 80.dp
private val HourBarWidth: Dp = 26.dp
private const val HourBarMaxHeight = 72f

/** A non-zero count always draws something, however small its share of the maximum. */
private const val BarMinHeight = 4f

/** A zero count draws nothing at all, so an empty bucket is empty rather than merely short. */
private val NoBar: Dp = 0.dp

/** The two sleep-count cells, which carry three lines of text rather than a bar. */
private val SleepCellMinHeight: Dp = 96.dp

/** The well the loading spinner sits in, so the list does not jump when a snapshot arrives. */
private val LoadingWellHeight: Dp = 96.dp

/**
 * A selected histogram cell doubles its border. The unselected width is `MsSpacing.hairline`, so
 * the pair is a width change as well as a colour change — selection is never colour alone (D-12).
 */
private val SelectedCellBorder: Dp = 2.dp

/**
 * The fact and episode cards pad their own rows rather than their container, because the design's
 * hairline separators run the full width of the card (D-11).
 */
private val NoCardPadding: Dp = 0.dp

/**
 * The design has **two** small-label idioms and `labelSmall` is only one of them.
 *
 * Eyebrows and section titles are tracked 2.2–2.4 px, which is what `labelSmall`'s 0.244 em
 * expresses. Raster row dates, legend labels and axis ticks are tracked 0.5–0.6 px instead
 * (lines 358, 367, 374, 405 of the design authority) — they are data, not identity, and the
 * eyebrow's tracking makes them illegible as data.
 *
 * At 9 sp, 0.6 px is 0.067 em. Using the eyebrow tracking rendered `Jul 8` as `J u l  8` and
 * `nothing` as `n o t h i n g`, which installed-app capture caught and no test would have
 * (`docs/specs/SPEC-insights-visual.md`, D-19).
 */
private val ChartLabelTracking = 0.067.em

@Composable
fun InsightsRoute(
    viewModel: InsightsViewModel,
    modifier: Modifier = Modifier,
    onOpenReport: () -> Unit = {}
) {
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
        onSelectSleepCategory = viewModel::selectSleepCategory,
        onRetry = viewModel::retry,
        onOpenReport = onOpenReport,
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
    onSelectSleepCategory: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenReport: () -> Unit = {},
    zoneId: ZoneId = ZoneId.systemDefault()
) {
    val palette = MaterialTheme.ms
    LazyColumn(
        modifier = modifier.testTag("insights_screen"),
        contentPadding = PaddingValues(MsSpacing.lgPlus),
        verticalArrangement = Arrangement.spacedBy(MsSpacing.xl)
    ) {
        item(key = "ranges") {
            // The design centres this row. MindScale scrolls it instead: six 48 dp targets and
            // their gaps do not fit a 320 dp screen at 200% font, and every range must stay
            // reachable (D-13).
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm)
            ) {
                InsightRange.entries.forEach { range ->
                    MsChip(
                        text = range.shortLabel,
                        selected = uiState.range == range,
                        onClick = { onRangeSelected(range) },
                        modifier = Modifier
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
                // A card rather than the `errorContainer` surface it replaces: every action tone
                // in this design is calibrated for a `bg` or `card` backdrop (D-13).
                MsCard(contentPadding = MsSpacing.lgPlus) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            error,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.danger
                        )
                        // Kept a `TextButton` with only its label uppercased, per D-3 of the Track
                        // and Log spec: `InsightsScreenTest` finds this by text and clicks it, so
                        // the click action has to stay on the node that carries the text.
                        TextButton(onClick = onRetry) {
                            MsUppercaseText(
                                text = "Retry",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        if (uiState.loading && uiState.snapshot == null) {
            item(key = "loading") {
                Box(
                    Modifier.fillMaxWidth().height(LoadingWellHeight),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.testTag("insights_loading"))
                }
            }
        }

        val snapshot = uiState.snapshot
        if (snapshot != null && !snapshot.hasEntries) {
            item(key = "empty") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus),
                    modifier = Modifier.testTag("insights_empty")
                ) {
                    MsEyebrow("Nothing to draw yet")
                    Text(
                        "This page shows only what you recorded — no estimates and no guesses. It fills in as you log.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkSecondary
                    )
                }
            }
        } else if (snapshot != null) {
            if (!snapshot.hasRangeData) {
                item(key = "range_empty") {
                    Text(
                        "No ratings in this range",
                        modifier = Modifier.testTag("insights_range_empty"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkTertiary
                    )
                }
            }
            item(key = "summary") {
                SummaryStrip(snapshot.summary)
            }
            item(key = "raster") {
                val readout = uiState.exploredInstantMillis?.let {
                    rasterReadout(snapshot, it, uiState.hourFormat, zoneId)
                } ?: "Touch or drag to read a day and hour"
                Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.smPlus)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MsSpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle("Days and hours")
                        // Weighted and end-aligned so MindScale's much longer readout wraps inside
                        // the row instead of pushing the title out of it at 200% font (D-8).
                        Text(
                            readout,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkTertiary,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { liveRegion = LiveRegionMode.Polite }
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
                    Caveat(
                        "One row per local day. Plain space is awake time with nothing recorded; sleep pauses symptom time."
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
            // The design draws both lists as one card of hairline-separated rows rather than as a
            // stack of separate cards (D-11). Both are bounded — at most six facts and eight
            // episodes — so folding each into one list item costs no laziness that matters.
            item(key = "facts") {
                Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.smPlus)) {
                    SectionTitle("Episodes")
                    MsCard(contentPadding = NoCardPadding) {
                        snapshot.facts.forEachIndexed { index, fact ->
                            key("fact:$index") {
                                if (index > 0) MsHairline(faint = true)
                                Column(
                                    Modifier.fillMaxWidth().padding(
                                        horizontal = MsSpacing.lgPlus,
                                        vertical = MsSpacing.lg
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(MsSpacing.xxs)
                                ) {
                                    Text(
                                        fact.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.inkPrimary
                                    )
                                    fact.detail?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.inkQuaternary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (snapshot.recentEpisodes.isNotEmpty()) {
                item(key = "episodes") {
                    Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.smPlus)) {
                        SectionTitle("Each episode")
                        MsCard(contentPadding = NoCardPadding) {
                            snapshot.recentEpisodes.forEachIndexed { index, episode ->
                                key("episode:${episode.onsetMillis}") {
                                    if (index > 0) MsHairline(faint = true)
                                    EpisodeRow(episode, uiState.hourFormat, zoneId)
                                }
                            }
                        }
                    }
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
            item(key = "sleep_counts") {
                SleepCountsSection(
                    counts = snapshot.sleepCounts,
                    selectedCategoryIndex = uiState.selectedSleepCategoryIndex,
                    onSelectCategory = onSelectSleepCategory
                )
            }
        }
        item(key = "clinician_summary") {
            // The design's centred ink pill at line 517. `MsPillButton`'s selected treatment is
            // exactly that fill, and it is presentation only — the control is unchanged.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                MsPillButton(
                    text = "Clinician summary",
                    onClick = onOpenReport,
                    selected = true,
                    modifier = Modifier.testTag("insights_open_report")
                )
            }
        }
    }
}

/** The design's 10.5 px tracked uppercase section label, at a compliant emphasis level. */
@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    MsUppercaseText(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.ms.inkTertiary
    )
}

/** The design's data-label idiom: 9 sp weight 500, tracked 0.6 px rather than an eyebrow's 2.4. */
@Composable
private fun chartLabelStyle() =
    MaterialTheme.typography.labelSmall.copy(letterSpacing = ChartLabelTracking)

/** The design's faint caveat paragraph beneath a panel. */
@Composable
private fun Caveat(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.ms.inkTertiary
    )
}

@Composable
private fun SleepCountsSection(
    counts: SleepCounts,
    selectedCategoryIndex: Int?,
    onSelectCategory: (Int) -> Unit
) {
    val palette = MaterialTheme.ms
    Column(
        modifier = Modifier.testTag("sleep_counts_section"),
        verticalArrangement = Arrangement.spacedBy(MsSpacing.smPlus)
    ) {
        SectionTitle("Sleep you recorded")
        if (!counts.isEligible) {
            RefusalPanel(
                text = "No completed sleep periods woke in this range.",
                modifier = Modifier.testTag("sleep_counts_refusal")
            )
        } else {
            Denominator(sleepCountsDenominator(counts))
            Row(
                modifier = Modifier.fillMaxWidth().testTag("sleep_category_cells"),
                horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm)
            ) {
                counts.categories.forEachIndexed { index, category ->
                    val isSelected = selectedCategoryIndex == index
                    val periodWord = if (counts.completedCount == 1) "period" else "periods"
                    val description = "${sleepCategoryVisibleLabel(category.category)}, " +
                        "${category.count} of ${counts.completedCount} completed sleep $periodWord, " +
                        sleepCategoryBoundary(category.category)
                    Surface(
                        onClick = { onSelectCategory(index) },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) palette.ink else Color.Transparent,
                        border = BorderStroke(
                            width = if (isSelected) SelectedCellBorder else MsSpacing.hairline,
                            color = if (isSelected) palette.ink else palette.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SleepCellMinHeight)
                            .testTag("sleep_category_$index")
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                selected = isSelected
                                contentDescription = description
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(MsSpacing.mdPlus),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(MsSpacing.xs)
                        ) {
                            Text(
                                category.count.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isSelected) palette.onInk else palette.inkPrimary
                            )
                            MsUppercaseText(
                                text = sleepCategoryVisibleLabel(category.category),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) palette.onInk else palette.inkSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                if (category.category == SleepCategory.NIGHT) ">3h" else "≤3h",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) palette.onInk else palette.inkQuaternary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            LiveReadout(
                text = selectedCategoryIndex?.let { sleepCategoryReadout(counts, it) }
                    ?: "Select nights or naps to read the exact durations.",
                modifier = Modifier.testTag("sleep_counts_readout")
            )
        }
        sleepIncompleteText(counts)?.let { text ->
            Caveat(text, modifier = Modifier.testTag("sleep_incomplete_text"))
        }
        Caveat(SLEEP_COUNTS_CAVEAT)
    }
}

@Composable
private fun OnsetGapSection(
    histogram: OnsetGapHistogram,
    selectedBucketIndex: Int?,
    onSelectBucket: (Int) -> Unit
) {
    val palette = MaterialTheme.ms
    Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.smPlus)) {
        SectionTitle("Days between onsets")
        if (!histogram.isEligible) {
            RefusalPanel(
                text = onsetGapRefusalText(histogram),
                modifier = Modifier.testTag("onset_gap_refusal")
            )
            return@Column
        }

        Denominator(onsetGapDenominator(histogram))
        val maximumCount = histogram.buckets.maxOfOrNull(OnsetGapBucket::count)?.coerceAtLeast(1) ?: 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("onset_gap_bars"),
            horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm)
        ) {
            histogram.buckets.forEach { bucket ->
                val isSelected = selectedBucketIndex == bucket.index
                val barHeight = if (bucket.count == 0) NoBar else {
                    (GapBarMaxHeight * bucket.count / maximumCount).coerceAtLeast(BarMinHeight).dp
                }
                val description = "${bucket.visibleLabel} bucket, ${bucket.count} of " +
                    "${histogram.gapCount} onset-to-onset gaps, ${bucket.spokenBoundary}"
                Surface(
                    onClick = { onSelectBucket(bucket.index) },
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) palette.ink else Color.Transparent,
                    border = BorderStroke(
                        width = if (isSelected) SelectedCellBorder else MsSpacing.hairline,
                        color = if (isSelected) palette.ink else palette.outline
                    ),
                    modifier = Modifier
                        .width(GapCellWidth)
                        .heightIn(min = GapCellMinHeight)
                        .testTag("onset_gap_bucket_${bucket.index}")
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            selected = isSelected
                            contentDescription = description
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = MsSpacing.xs,
                            vertical = MsSpacing.sm
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            bucket.count.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) palette.onInk else palette.inkPrimary
                        )
                        HistogramBar(
                            wellHeight = GapBarWellHeight,
                            barWidth = GapBarWidth,
                            barHeight = barHeight,
                            selected = isSelected
                        )
                        // A bucket boundary is data, so it keeps its own case: `gapBars`'
                        // label at line 1461 sets neither `text-transform` nor tracking (D-3).
                        Text(
                            text = bucket.visibleLabel,
                            style = chartLabelStyle(),
                            color = if (isSelected) palette.onInk else palette.inkQuaternary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        LiveReadout(
            text = selectedBucketIndex?.let { onsetGapBucketReadout(histogram, it) }
                ?: "Select a bar to read its exact count.",
            modifier = Modifier.testTag("onset_gap_readout")
        )
        Caveat(ONSET_GAP_CAVEAT)
    }
}

@Composable
private fun OnsetTimeSection(
    counts: OnsetTimeCounts,
    selectedHour: Int?,
    hourFormat: HourFormat,
    onSelectHour: (Int) -> Unit
) {
    val palette = MaterialTheme.ms
    Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.smPlus)) {
        SectionTitle("Time of day it started")
        if (!counts.isEligible) {
            RefusalPanel(
                text = onsetTimeRefusalText(counts),
                modifier = Modifier.testTag("onset_time_refusal")
            )
            return@Column
        }

        Denominator(onsetTimeDenominator(counts))
        val maximumCount = counts.buckets.maxOfOrNull(OnsetHourBucket::count)?.coerceAtLeast(1) ?: 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("onset_time_bars"),
            horizontalArrangement = Arrangement.spacedBy(MsSpacing.sm)
        ) {
            counts.buckets.forEach { bucket ->
                val isSelected = selectedHour == bucket.hourOfDay
                val barHeight = if (bucket.count == 0) NoBar else {
                    (HourBarMaxHeight * bucket.count / maximumCount).coerceAtLeast(BarMinHeight).dp
                }
                val hourLabel = onsetHourVisibleLabel(bucket.hourOfDay, hourFormat)
                val description = "${onsetHourSpokenLabel(bucket.hourOfDay, hourFormat)} hour, " +
                    "${bucket.count} of ${counts.eligibleOnsetCount} " +
                    "recorded starts, ${onsetHourBoundary(bucket.hourOfDay, hourFormat)}"
                Surface(
                    onClick = { onSelectHour(bucket.hourOfDay) },
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) palette.ink else Color.Transparent,
                    border = BorderStroke(
                        width = if (isSelected) SelectedCellBorder else MsSpacing.hairline,
                        color = if (isSelected) palette.ink else palette.outline
                    ),
                    modifier = Modifier
                        .width(HourCellWidth)
                        .heightIn(min = HourCellMinHeight)
                        .testTag("onset_time_hour_${bucket.hourOfDay}")
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            selected = isSelected
                            contentDescription = description
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = MsSpacing.xs,
                            vertical = MsSpacing.sm
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            bucket.count.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) palette.onInk else palette.inkPrimary
                        )
                        HistogramBar(
                            wellHeight = HourBarWellHeight,
                            barWidth = HourBarWidth,
                            barHeight = barHeight,
                            selected = isSelected
                        )
                        // A clock hour is data, like the bucket boundary above (D-3).
                        Text(
                            text = hourLabel,
                            style = chartLabelStyle(),
                            color = if (isSelected) palette.onInk else palette.inkQuaternary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        Text(
            onsetTimeFourHourSentence(counts, hourFormat),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.ms.inkPrimary
        )
        LiveReadout(
            text = selectedHour?.let { onsetTimeBucketReadout(counts, it, hourFormat) }
                ?: "Select an hour to read its exact count.",
            modifier = Modifier.testTag("onset_time_readout")
        )
        Caveat(ONSET_TIME_CAVEAT)
    }
}

/**
 * The design's bar: full width of its own column, 2 dp top corners, gold — or `onInk` when the
 * cell beneath it has inverted to an ink fill, because the design's own selected bar colour is
 * `var(--ink)` and that would vanish (D-12).
 *
 * Full gold rather than the design's `rgba(gold,.85)`: the bar is the mark the histogram exists to
 * draw, and 85% gold measures 2.59:1 on `card` in light where D-23 requires 3:1 (D-7).
 */
@Composable
private fun HistogramBar(wellHeight: Dp, barWidth: Dp, barHeight: Dp, selected: Boolean) {
    val palette = MaterialTheme.ms
    Box(
        modifier = Modifier.fillMaxWidth().height(wellHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .width(barWidth)
                .height(barHeight)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(if (selected) palette.onInk else palette.gold)
                .clearAndSetSemantics { }
        )
    }
}

/** The design's "needs more data" panel: a card holding one calm paragraph. */
@Composable
private fun RefusalPanel(text: String, modifier: Modifier = Modifier) {
    MsCard(modifier = modifier.fillMaxWidth(), contentPadding = MsSpacing.lgPlus) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.ms.inkTertiary
        )
    }
}

/** The exact-denominator line each histogram carries above its bars. */
@Composable
private fun Denominator(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.ms.inkSecondary
    )
}

/**
 * A selection readout. The live region is what makes a coloured bar legal under Invariant 14, so
 * it is never restyled away (D-6).
 */
@Composable
private fun LiveReadout(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.ms.inkSecondary,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }
    )
}

/**
 * L-3. The design's four `flex:1` cells render at four different widths because each holds
 * `white-space:nowrap` spans; `Modifier.weight(1f)` divides the row equally whatever the content
 * says, which is the correction D-22 specifies. `InsightsVisualTest` pins it at 100% and 200% font
 * so uppercasing `TYPICAL LENGTH` cannot quietly reintroduce the flaw (D-9).
 */
@Composable
private fun SummaryStrip(summary: InsightSummary) {
    val palette = MaterialTheme.ms
    val cells = listOf(
        "Episodes" to summary.episodeCount.toString(),
        "Typical length" to (summary.typicalLengthMillis?.let(::formatDuration) ?: "—"),
        "Clear days" to "${summary.clearDays}/${summary.eligibleDays}",
        "Peak" to (summary.peak?.toString() ?: "—")
    )
    Column {
        MsHairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MsSpacing.lg)
                .testTag("insights_summary"),
            horizontalArrangement = Arrangement.spacedBy(MsSpacing.xxs)
        ) {
            cells.forEach { (label, value) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "$label, $value" },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MsSpacing.xxs)
                ) {
                    Text(
                        value,
                        style = MaterialTheme.typography.headlineLarge,
                        color = palette.inkPrimary,
                        textAlign = TextAlign.Center
                    )
                    MsUppercaseText(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.inkQuaternary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        MsHairline()
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
    val palette = MaterialTheme.ms
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val noDataColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    // The design's `wellC`: "nothing recorded" is the card itself.
    val wellColor = palette.card
    // The design's `sleepC`. The token has existed since Phase 15 and this is its first caller.
    val asleepColor = palette.sleepBand
    val futureColor = palette.ink.copy(alpha = if (dark) 0.14f else 0.08f)
    // The hatching is what distinguishes FUTURE from NO_DATA, so it is a mark rather than a
    // decoration and takes the control-boundary token at 3.47:1 (D-7).
    val futureStripeColor = palette.outline
    val rowHeight = when {
        snapshot.rasterDays.size <= 7 -> RasterRowHeightWeek
        snapshot.rasterDays.size <= 30 -> RasterRowHeightMonth
        snapshot.rasterDays.size <= 90 -> RasterRowHeightQuarter
        else -> RasterRowHeightLong
    }
    // The panel's interior stays exactly what it was — day rows and nothing else. The design puts
    // its legend inside; here that would push the panel's centre past the only row a one-day
    // snapshot has, and `InsightsScreenTest` clicks that centre (D-8).
    MsCard(
        contentPadding = MsSpacing.md,
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
        Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.hairline)) {
            snapshot.rasterDays.forEachIndexed { index, day ->
                key(day.date) { Row(verticalAlignment = Alignment.CenterVertically) {
                    // Left in the formatter's own case, and out of the semantics tree, per D-3.
                    Text(
                        if (snapshot.rasterDays.size <= 30 || index == 0 || index == snapshot.rasterDays.lastIndex || day.date.dayOfWeek.value == 1) {
                            day.date.format(DateTimeFormatter.ofPattern("MMM d"))
                        } else "",
                        style = chartLabelStyle(),
                        color = palette.inkQuaternary,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .width(RasterDateColumnWidth)
                            .padding(end = MsSpacing.xs)
                            .clearAndSetSemantics { }
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
                                topLeft = Offset(size.width * segment.startFraction, 0f),
                                size = Size(
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
                                        start = Offset(x, size.height),
                                        end = Offset(x + size.height, 0f),
                                        strokeWidth = ChartHairline.toPx()
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

/**
 * The design's legend, with one addition it does not make: a swatch that does not itself clear 3:1
 * against the card gets an `ms.outline` boundary, not only the "nothing" swatch. Four of the six
 * are quiet ground and would otherwise be invisible squares beside their own labels (D-7, D-8).
 */
@Composable
private fun RasterLegend() {
    val palette = MaterialTheme.ms
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val items = listOf(
        Triple("nothing", palette.card, true),
        Triple("1", intensityColor(1, dark), false),
        Triple("10", intensityColor(10, dark), false),
        Triple("asleep", palette.sleepBand, true),
        Triple("no data", MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), true),
        Triple("future", palette.ink.copy(alpha = if (dark) 0.14f else 0.08f), true)
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MsSpacing.mdPlus)
    ) {
        items.forEach { (label, color, outlined) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(RasterSwatchWidth, RasterSwatchHeight)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(color)
                        .then(
                            if (outlined) {
                                Modifier.border(
                                    MsSpacing.hairline,
                                    palette.outline,
                                    MaterialTheme.shapes.extraSmall
                                )
                            } else Modifier
                        )
                )
                Spacer(Modifier.width(MsSpacing.xs))
                Text(
                    label,
                    style = chartLabelStyle(),
                    color = palette.inkQuaternary
                )
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
    Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.smPlus)) {
        SectionTitle("What you recorded")
        Text(
            readout,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.ms.inkTertiary,
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
        Caveat(
            "Ratings stay flat until another rating or the ${holdHours}h waking-hour limit. " +
                "The line stops during sleep. Dotted lines are events you marked."
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
    val palette = MaterialTheme.ms
    // Full gold rather than the design's `rgba(gold,.95)`, which measures 2.94:1 in light (D-7).
    val line = palette.gold
    val area = palette.gold.copy(alpha = 0.16f)
    val grid = palette.hairline
    val baseline = palette.outlineDecorative
    // The design's solid sleep column. The hatching MindScale used here is dropped: the step line
    // genuinely stops across a sleep span, which is already a signal that is not colour (D-10).
    val sleep = palette.sleepBand
    val event = palette.outline
    val selection = palette.ink
    val span = (chart.endMillis - chart.startMillis).coerceAtLeast(1L)

    MsCard(
        contentPadding = MsSpacing.md,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MsSpacing.minTouchTarget)
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
        Column(verticalArrangement = Arrangement.spacedBy(MsSpacing.xs)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier
                        .width(ChartAxisColumnWidth)
                        .height(ChartPlotHeight)
                        .clearAndSetSemantics { },
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    listOf("10", "5", "0").forEach { label ->
                        Text(
                            label,
                            style = chartLabelStyle(),
                            color = palette.inkQuaternary
                        )
                    }
                }
                Spacer(Modifier.width(MsSpacing.xs))
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(ChartPlotHeight)
                        .pointerInput(chart) {
                            detectTapGestures { offset ->
                                onExplore(
                                    chartInstantFromPosition(
                                        x = offset.x,
                                        width = size.width.toFloat(),
                                        chart = chart,
                                        eventSnapPixels = ChartEventSnapRadius.toPx()
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
                                        eventSnapPixels = ChartEventSnapRadius.toPx()
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
                    }
                    listOf(10, 5).forEach { value ->
                        drawLine(grid, Offset(0f, yOf(value)), Offset(size.width, yOf(value)), ChartHairline.toPx())
                    }
                    drawLine(baseline, Offset(0f, yOf(0)), Offset(size.width, yOf(0)), ChartHairline.toPx())
                    chart.markers.forEach { marker ->
                        val x = xOf(marker.atMillis)
                        drawLine(
                            color = event,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = ChartHairline.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(ChartDashPitch.toPx(), ChartDashPitch.toPx())
                            )
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
                            color = line,
                            start = Offset(left, y),
                            end = Offset(right, y),
                            strokeWidth = ChartStrokeWidth.toPx(),
                            cap = StrokeCap.Round
                        )
                        val next = chart.segments.getOrNull(index + 1)
                        if (next != null && next.startMillis == segment.endMillis) {
                            drawLine(
                                color = line,
                                start = Offset(right, y),
                                end = Offset(right, yOf(next.value)),
                                strokeWidth = ChartStrokeWidth.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    selectedInstantMillis?.let { selectedMillis ->
                        val clamped = selectedMillis.coerceIn(chart.startMillis, chart.endMillis)
                        val x = xOf(clamped)
                        drawLine(palette.crosshair, Offset(x, 0f), Offset(x, size.height), ChartHairline.toPx())
                        val reading = chart.readingAt(clamped)
                        if (reading.state == EntryChartState.WELL || reading.state == EntryChartState.INTENSITY) {
                            drawCircle(selection, ChartSelectionDotRadius.toPx(), Offset(x, yOf(reading.value ?: 0)))
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ChartTickRowInset)
                    .clearAndSetSemantics { },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                entryChartTicks(chart, range, hourFormat, zoneId).forEachIndexed { index, label ->
                    Text(
                        label,
                        style = chartLabelStyle(),
                        color = palette.inkQuaternary,
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
    val palette = MaterialTheme.ms
    val items = listOf(
        Triple("recorded intensity", palette.gold, false),
        Triple("asleep", palette.sleepBand, false),
        Triple("event", palette.outline, true)
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(MsSpacing.lg)
    ) {
        items.forEach { (label, color, dotted) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(
                    Modifier
                        .size(ChartSwatchWidth, ChartSwatchHeight)
                        .clearAndSetSemantics { }
                ) {
                    if (dotted) {
                        drawLine(
                            color,
                            Offset(size.width / 2f, 0f),
                            Offset(size.width / 2f, size.height),
                            ChartHairline.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(LegendDashPitch.toPx(), LegendDashPitch.toPx())
                            )
                        )
                    } else drawRect(color)
                }
                Spacer(Modifier.width(MsSpacing.xs))
                Text(
                    label,
                    style = chartLabelStyle(),
                    color = palette.inkQuaternary
                )
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
    val palette = MaterialTheme.ms
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$onset, peak ${episode.peak}, $detail" }
            .padding(horizontal = MsSpacing.lgPlus, vertical = MsSpacing.mdPlus),
        horizontalArrangement = Arrangement.spacedBy(MsSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MsSpacing.xxxs)
        ) {
            Text(
                onset,
                style = MaterialTheme.typography.titleSmall,
                color = palette.inkPrimary
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = palette.inkQuaternary
            )
        }
        Text(
            episode.peak.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = palette.goldText
        )
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
