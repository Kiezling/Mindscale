package com.kieslingdev.mindscale.insights

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.kieslingdev.mindscale.data.EpisodeSourceRow
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The geometry `docs/specs/SPEC-insights-visual.md` freezes for Insights, asserted rather than
 * eyeballed.
 *
 * These are new assertions on a restyled screen. They replace nothing: `InsightsScreenTest` still
 * owns Insights' behavior, is untouched by this phase, and must keep passing — a break there would
 * mean behavior changed, which D-1 forbids.
 *
 * The pattern is `TrackVisualTest`'s and `LogVisualTest`'s, and it is used again for the reason
 * Phase 16 recorded: numeric assertions caught a 2 px numpad defect and a 33 dp chip defect that
 * looking at a screenshot did not.
 */
class InsightsVisualTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** A pixel and a half of slack, so an odd-width rounding cannot fail an exact-equality claim. */
    private val tolerancePx = 1.5f

    /** The same slack expressed in dp, for the unclipped measurements. */
    private val toleranceDp = 0.6f

    @Composable
    private fun InsightsUnderTest(state: InsightsUiState, onExplore: (Long) -> Unit = {}) {
        InsightsScreen(
            uiState = state,
            onRangeSelected = {},
            onExplore = onExplore,
            onEarlierHour = { true },
            onLaterHour = { true },
            onPreviousDay = { true },
            onNextDay = { true },
            onExploreChart = {},
            onEarlierChartHour = { true },
            onLaterChartHour = { true },
            onPreviousRating = { true },
            onNextRating = { true },
            onPreviousEvent = { true },
            onNextEvent = { true },
            onSelectOnsetGapBucket = {},
            onSelectOnsetHour = {},
            onSelectSleepCategory = {},
            onRetry = {},
            zoneId = ZoneOffset.UTC
        )
    }

    private fun setContent(state: InsightsUiState, fontScale: Float = 1f) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MindScaleTheme { InsightsUnderTest(state) }
            }
        }
    }

    private fun bounds(tag: String): Rect =
        composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    /**
     * Unclipped bounds, in dp.
     *
     * The histogram rows scroll horizontally, and `boundsInRoot` is **clipped** by that scroller —
     * a bucket half past the viewport reports half its width. Measuring equal widths from clipped
     * bounds would have asserted "every visible sliver is the same size", which is not the claim.
     * Found on this test's first run: bucket 4 reported 156 px against a 189 px reference.
     */
    private fun unclipped(tag: String): DpRect =
        composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    // ── L-3: the summary strip's four columns are equal-width ────────────────

    private fun summaryCellBounds(): List<Rect> =
        composeTestRule.onNodeWithTag("insights_summary")
            .fetchSemanticsNode()
            .children
            .map { it.boundsInRoot }

    private fun assertFourEqualColumns() {
        val cells = summaryCellBounds()
        assertEquals("the summary strip should have four cells", 4, cells.size)

        val reference = cells.first().width
        cells.forEachIndexed { index, rect ->
            assertEquals(
                "summary column $index is ${rect.width} px against a reference of $reference px",
                reference,
                rect.width,
                tolerancePx
            )
        }

        // Left to right, evenly gapped, so "equal width" cannot be satisfied by overlap.
        val gaps = cells.zipWithNext { a, b -> b.left - a.right }
        gaps.zipWithNext { a, b ->
            assertEquals("the gaps between summary columns are uneven", a, b, tolerancePx)
        }
    }

    /**
     * L-3, from D-22 of the foundation. The prototype's four `flex:1` cells hold
     * `white-space:nowrap` spans and render at four different widths, which is what makes
     * `TYPICAL LENGTH` break the strip's rhythm in `dark-insights-top.png`.
     */
    @Test
    fun theSummaryStripsFourColumnsAreEqualWidth() {
        setContent(populated())

        assertFourEqualColumns()
    }

    /**
     * The half of L-3 this phase can actually get wrong. `TYPICAL LENGTH` is fourteen tracked
     * characters at 9 sp; at 200% font it is wider than a quarter of the row and has to wrap
     * rather than steal width from its neighbours (D-9, D-16).
     */
    @Test
    fun theSummaryStripsFourColumnsStayEqualWidthAt200PercentFont() {
        setContent(populated(), fontScale = 2f)

        assertFourEqualColumns()
    }

    /** The four values sit on one line, so the strip reads as a row rather than as four stacks. */
    @Test
    fun theSummaryStripsFourValuesShareOneTopEdge() {
        setContent(populated())

        val tops = summaryCellBounds().map { it.top }
        tops.zipWithNext { a, b ->
            assertEquals("the summary cells do not share one top edge", a, b, tolerancePx)
        }
    }

    // ── D-8: the raster panel's interior is day rows and nothing else ────────

    /**
     * The design puts its legend inside the raster panel. MindScale cannot, and this is the
     * assertion that says why rather than leaving it as a comment:
     * `InsightsScreenTest.rasterTouchAndAccessibilityActionUseOneExplorationSurface` clicks the
     * panel's **centre** and requires that click to reach a day row. On a one-day snapshot the
     * panel is exactly its 10 dp padding around one 20 dp row, so the centre lands on the row. A
     * legend inside would push the centre roughly 38 dp down, past the only row there is.
     */
    @Test
    fun theRasterPanelHoldsNothingButItsPaddedDayRows() {
        setContent(populated())

        val panel = bounds("raster_chart")
        val expected = with(composeTestRule.density) { (10 + 20 + 10).dp.toPx() }

        assertEquals(
            "the raster panel is ${panel.height} px tall; anything beyond its padded day rows " +
                "moves the centre `InsightsScreenTest` clicks (D-8)",
            expected,
            panel.height,
            tolerancePx
        )
    }

    /** And the consequence, asserted directly: a centre click still explores. */
    @Test
    fun aCentreClickOnTheRasterPanelStillReachesADayRow() {
        var explored: Long? = null
        composeTestRule.setContent {
            MindScaleTheme { InsightsUnderTest(populated(), onExplore = { explored = it }) }
        }

        composeTestRule.onNodeWithTag("raster_chart").performTouchInput { click(center) }

        assertNotNull("a centre click on the raster panel did not reach a day row", explored)
    }

    // ── D-23: touch targets ──────────────────────────────────────────────────

    @Test
    fun everyRangeChipReachesTheTouchTargetFloor() {
        setContent(populated())

        InsightRange.entries.forEach { range ->
            composeTestRule.onNodeWithTag("insight_range_${range.name}")
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun everyRangeChipStillReachesTheTouchTargetFloorAt200PercentFont() {
        setContent(populated(), fontScale = 2f)

        InsightRange.entries.forEach { range ->
            composeTestRule.onNodeWithTag("insight_range_${range.name}")
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun everyHistogramAndCountCellReachesTheTouchTargetFloorOnBothAxes() {
        setContent(eligible())

        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("onset_gap_bars"))
        (0 until 10).forEach { index ->
            composeTestRule.onNodeWithTag("onset_gap_bucket_$index")
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }

        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("onset_time_bars"))
        (0 until 24).forEach { hour ->
            composeTestRule.onNodeWithTag("onset_time_hour_$hour")
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    /**
     * The cells keep a fixed width and a `heightIn(min = …)`, never a fixed height, so at 200%
     * font the count and label grow downward rather than clipping. Asserted rather than captured:
     * the histogram sits far enough down a 200%-font Insights that a screenshot of it is a
     * scroll-position accident, and D-16 wants this pinned rather than looked at once.
     */
    @Test
    fun everyHistogramCellStillReachesTheTouchTargetFloorAt200PercentFont() {
        setContent(eligible(), fontScale = 2f)

        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("onset_gap_bars"))
        (0 until 10).forEach { index ->
            composeTestRule.onNodeWithTag("onset_gap_bucket_$index")
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }

        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("onset_time_bars"))
        (0 until 24).forEach { hour ->
            composeTestRule.onNodeWithTag("onset_time_hour_$hour")
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun bothSleepCountCellsReachTheTouchTargetFloorOnBothAxes() {
        setContent(sleepPopulated())

        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("sleep_category_cells"))
        (0 until 2).forEach { index ->
            composeTestRule.onNodeWithTag("sleep_category_$index")
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    // ── D-12: selection is drawn in space reserved in every state ────────────

    /**
     * A selected cell doubles its border from 1 dp to 2 dp. If that border grew the cell rather
     * than being drawn inside it, the whole scrolling row would shift under the user's finger
     * between the tap that selects and the next tap. This is the same class of assertion
     * `TrackVisualTest.armingThePadDoesNotMoveOrResizeAnyKey` makes about the armed pad.
     *
     * The border's *colour* is not in the semantics tree, so its appearance is verified by
     * installed-app capture rather than here. Stated rather than implied.
     */
    @Test
    fun selectingAHistogramCellMovesAndResizesNothing() {
        var state by mutableStateOf(eligible())
        composeTestRule.setContent {
            MindScaleTheme { InsightsUnderTest(state) }
        }
        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("onset_gap_bars"))
        val unselected = (0 until 10).associateWith { unclipped("onset_gap_bucket_$it") }

        composeTestRule.runOnIdle { state = eligible(selectedGapBucket = 0) }
        composeTestRule.waitForIdle()

        unselected.forEach { (index, before) ->
            val after = unclipped("onset_gap_bucket_$index")
            assertEquals(
                "bucket $index width changed on selection",
                before.width.value, after.width.value, toleranceDp
            )
            assertEquals(
                "bucket $index height changed on selection",
                before.height.value, after.height.value, toleranceDp
            )
            assertEquals(
                "bucket $index moved relative to bucket 0 on selection",
                (before.left - unselected.getValue(0).left).value,
                (after.left - unclipped("onset_gap_bucket_0").left).value,
                toleranceDp
            )
        }
    }

    /** Every gap bucket keeps one width, so a bucket labelled `<1d` is not narrower than `10-13d`. */
    @Test
    fun everyGapBucketSharesOneWidthAndOneHeight() {
        setContent(eligible())
        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("onset_gap_bars"))

        val cells = (0 until 10).map { it to unclipped("onset_gap_bucket_$it") }
        val referenceWidth = cells.first().second.width.value
        val referenceHeight = cells.first().second.height.value
        cells.forEach { (index, rect) ->
            assertEquals("bucket $index width", referenceWidth, rect.width.value, toleranceDp)
            assertEquals("bucket $index height", referenceHeight, rect.height.value, toleranceDp)
        }
    }

    /** The report link is a pill that hugs its label rather than a full-bleed button (D-13). */
    @Test
    fun theReportLinkIsACentredPillRatherThanAFullWidthButton() {
        setContent(populated())
        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("insights_open_report"))

        val screen = bounds("insights_screen")
        val pill = bounds("insights_open_report")

        assertTrue(
            "the report link is ${pill.width} px wide against a ${screen.width} px screen",
            pill.width < screen.width * 0.95f
        )
        assertEquals(
            "the report link is not centred",
            pill.left - screen.left,
            screen.right - pill.right,
            tolerancePx
        )
        composeTestRule.onNodeWithTag("insights_open_report").assertHeightIsAtLeast(48.dp)
    }

    // ── fixtures, matching `InsightsScreenTest`'s ────────────────────────────

    private fun populated() = InsightsUiState(
        loading = false,
        snapshot = snapshot(listOf(entry(1, 0, 5), entry(2, 2 * HOUR, 0)))
    )

    private fun eligible(selectedGapBucket: Int? = null) = InsightsUiState(
        loading = false,
        selectedOnsetGapBucketIndex = selectedGapBucket,
        snapshot = snapshot(
            episodeRows(listOf(0L, 2 * HOUR, 4 * HOUR, 6 * HOUR, 8 * HOUR, 10 * HOUR))
        )
    )

    private fun sleepPopulated() = InsightsUiState(
        loading = false,
        snapshot = snapshot(
            listOf(
                entry(1, 0, 0),
                sleep(2, 0, 2 * HOUR),
                sleep(3, 3 * HOUR, 7 * HOUR)
            )
        )
    )

    private fun snapshot(rows: List<EpisodeSourceRow>) = deriveInsights(
        rows = rows,
        hold = HoldDuration.SIXTEEN,
        now = Instant.ofEpochMilli(12 * HOUR),
        zoneId = ZoneOffset.UTC,
        range = InsightRange.ONE_DAY
    )

    private fun entry(id: Long, ts: Long, value: Int) =
        EpisodeSourceRow("ENTRY", id, ts, null, value, emptyList(), note = null)

    private fun sleep(id: Long, start: Long, end: Long?) =
        EpisodeSourceRow("SLEEP", id, start, end, null, null)

    private fun episodeRows(onsets: List<Long>): List<EpisodeSourceRow> = buildList {
        var id = 1L
        onsets.forEach { onset ->
            add(entry(id++, onset, 5))
            add(entry(id++, onset + HOUR / 2, 0))
        }
    }

    private companion object { const val HOUR = 3_600_000L }
}
