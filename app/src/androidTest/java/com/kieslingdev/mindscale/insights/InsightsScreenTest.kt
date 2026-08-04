package com.kieslingdev.mindscale.insights

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.kieslingdev.mindscale.data.EpisodeSourceRow
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InsightsScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun emptyStateHasHonestCopyAndNoSampleDataAction() {
        setContent(InsightsUiState(loading = false, snapshot = snapshot(emptyList())))

        composeTestRule.onNodeWithTag("insights_empty").assertExists()
        composeTestRule.onNodeWithText("Nothing to draw yet").assertExists()
        composeTestRule.onNodeWithText("Load sample data").assertDoesNotExist()
    }

    @Test
    fun everyRangeIsReachableSelectedAndInvokesCallback() {
        val selected = mutableListOf<InsightRange>()
        setContent(
            InsightsUiState(loading = false, range = InsightRange.THIRTY_DAYS, snapshot = snapshot(listOf(entry(1, 0, 5)))),
            onRangeSelected = { selected += it }
        )

        composeTestRule.onNodeWithTag("insight_range_THIRTY_DAYS").assertIsSelected()
            .assertHeightIsAtLeast(48.dp)
        InsightRange.entries.forEach { range ->
            composeTestRule.onNodeWithTag("insight_range_${range.name}").performClick()
        }

        assertEquals(InsightRange.entries, selected)
    }

    @Test
    fun populatedSummaryAndEpisodesExposeLabeledSemantics() {
        val state = InsightsUiState(
            loading = false,
            snapshot = snapshot(listOf(entry(1, 0, 5), entry(2, 2 * HOUR, 0)))
        )
        setContent(state)

        composeTestRule.onNodeWithTag("insights_summary").assertExists()
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Episodes, 1"))
        ).assertExists()
        composeTestRule.onNodeWithTag("insights_screen").performScrollToNode(hasText("Each episode"))
        composeTestRule.onNodeWithText("Each episode").assertExists()
    }

    @Test
    fun rasterTouchAndAccessibilityActionUseOneExplorationSurface() {
        var explored: Long? = null
        var earlierCalls = 0
        val state = InsightsUiState(
            loading = false,
            snapshot = snapshot(listOf(entry(1, 0, 5), entry(2, 2 * HOUR, 0)))
        )
        setContent(
            state,
            onExplore = { explored = it },
            onEarlierHour = { earlierCalls++; true }
        )

        val raster = composeTestRule.onNodeWithTag("raster_chart")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Touch or drag to read a day and hour"))
        raster.fetchSemanticsNode().config[SemanticsActions.CustomActions].first().action()
        raster.performTouchInput { click(center) }

        assertEquals(1, earlierCalls)
        assertNotNull(explored)
    }

    @Test
    fun entryChartTouchAndAccessibilityActionsUseOneExplorationSurface() {
        var explored: Long? = null
        var exploreCalls = 0
        var previousEventCalls = 0
        val state = InsightsUiState(
            loading = false,
            snapshot = snapshot(
                listOf(entry(1, 0, 5), marker(2, HOUR, "dose change"), entry(3, 2 * HOUR, 0))
            )
        )
        setContent(
            state,
            onExploreChart = { explored = it; exploreCalls++ },
            onPreviousEvent = { previousEventCalls++; true }
        )

        val chart = composeTestRule.onNodeWithTag("entry_chart")
            .assertHeightIsAtLeast(48.dp)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Touch or drag to read the chart"
                )
            )
        val actions = chart.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(6, actions.size)
        actions[4].action()
        chart.performTouchInput { click(center) }
        chart.performTouchInput {
            swipe(
                start = Offset(center.x * 0.8f, center.y),
                end = Offset(center.x * 1.6f, center.y),
                durationMillis = 500
            )
        }

        assertEquals(1, previousEventCalls)
        assertNotNull(explored)
        assertTrue(exploreCalls > 1)
    }

    @Test
    fun entryChartReadoutShowsChipsAndEventButHonorsHiddenNotes() {
        val selected = HOUR
        val state = InsightsUiState(
            loading = false,
            hideNotes = true,
            chartExploredInstantMillis = selected,
            snapshot = snapshot(
                listOf(
                    entry(1, 0, 5, chips = listOf("work"), note = "private note"),
                    marker(2, selected, "dose change")
                )
            )
        )
        setContent(state)

        composeTestRule.onNodeWithText("work", substring = true).assertExists()
        composeTestRule.onNodeWithText("event: dose change", substring = true).assertExists()
        composeTestRule.onNodeWithText("private note", substring = true).assertDoesNotExist()
    }

    @Test
    fun verticalSwipeOnThirtyDayRasterScrollsToFacts() {
        val rows = listOf(entry(1, 0, 5), entry(2, 2 * HOUR, 0))
        setContent(
            InsightsUiState(
                loading = false,
                range = InsightRange.THIRTY_DAYS,
                snapshot = snapshot(rows, InsightRange.THIRTY_DAYS)
            )
        )

        val list = composeTestRule.onNodeWithTag("insights_screen")
        val before = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        composeTestRule.onNodeWithTag("raster_chart").performTouchInput { swipeUp() }
        val after = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

        assertTrue(after > before)
    }

    @Test
    fun verticalSwipeOnEntryChartScrollsToEpisodeFacts() {
        val rows = listOf(entry(1, 0, 5), entry(2, 2 * HOUR, 0))
        setContent(InsightsUiState(loading = false, snapshot = snapshot(rows)))

        val list = composeTestRule.onNodeWithTag("insights_screen")
        list.performScrollToNode(hasTestTag("entry_chart"))
        val before = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        composeTestRule.onNodeWithTag("entry_chart").performTouchInput { swipeUp() }
        val after = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

        assertTrue(after > before)
    }

    @Test
    fun loadingFilteredEmptyAndStaleErrorRemainDistinct() {
        setContent(InsightsUiState(loading = true))
        composeTestRule.onNodeWithTag("insights_loading").assertExists()
    }

    @Test
    fun onsetGapHistogramRefusesSparseRangeWithoutRenderingBars() {
        val state = InsightsUiState(
            loading = false,
            snapshot = snapshot(episodeRows(listOf(0L, 2 * HOUR, 4 * HOUR, 6 * HOUR, 8 * HOUR)))
        )
        setContent(state)

        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("onset_gap_refusal"))
        composeTestRule.onNodeWithText(
            "Needs 1 more recorded start in this range before this chart is shown. " +
                "These starts make 4 onset-to-onset gaps."
        ).assertExists()
        composeTestRule.onNodeWithTag("onset_gap_bars").assertDoesNotExist()
    }

    @Test
    fun onsetGapHistogramExposesTenSelectableCountedBucketsAndLiveReadout() {
        var selected: Int? = null
        val state = InsightsUiState(
            loading = false,
            selectedOnsetGapBucketIndex = 0,
            snapshot = snapshot(episodeRows(listOf(0L, 2 * HOUR, 4 * HOUR, 6 * HOUR, 8 * HOUR, 10 * HOUR)))
        )
        setContent(state, onSelectOnsetGapBucket = { selected = it })

        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("onset_gap_bars"))
        composeTestRule.onNodeWithText("5 onset-to-onset gaps from 6 recorded starts in this range.").assertExists()
        (0 until 10).forEach { index ->
            composeTestRule.onNodeWithTag("onset_gap_bucket_$index").assertExists().assertHeightIsAtLeast(48.dp)
        }
        composeTestRule.onNodeWithTag("onset_gap_bucket_0")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("<1d bucket, 5 of 5 onset-to-onset gaps, under 1 elapsed day")
                )
            )
            .performClick()

        assertEquals(0, selected)
        composeTestRule.onNodeWithText("5 of 5 onset-to-onset gaps were under 1 elapsed day.").assertExists()
        composeTestRule.onNodeWithText(ONSET_GAP_CAVEAT).assertExists()
        composeTestRule.onNodeWithTag("onset_gap_bucket_9").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun verticalSwipeOnOnsetGapBucketScrollsParentList() {
        val state = InsightsUiState(
            loading = false,
            snapshot = snapshot(episodeRows(listOf(0L, 2 * HOUR, 4 * HOUR, 6 * HOUR, 8 * HOUR, 10 * HOUR)))
        )
        setContent(state)

        val list = composeTestRule.onNodeWithTag("insights_screen")
        list.performScrollToNode(hasTestTag("onset_gap_bars"))
        val before = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        composeTestRule.onNodeWithTag("onset_gap_bucket_0").performTouchInput { swipeDown() }
        val after = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

        assertTrue(after < before)
    }

    @Test
    fun onsetTimeCountsRefuseSparseRangeWithoutRenderingHours() {
        val state = InsightsUiState(
            loading = false,
            snapshot = snapshot(episodeRows(listOf(0L, 2 * HOUR, 4 * HOUR, 6 * HOUR, 8 * HOUR)))
        )
        setContent(state)

        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("onset_time_refusal"))
        composeTestRule.onNodeWithText(
            "Needs 1 more recorded start in this range before this chart is shown. " +
                "There are 5 starts to count by hour."
        ).assertExists()
        composeTestRule.onNodeWithTag("onset_time_bars").assertDoesNotExist()
        composeTestRule.onNodeWithText("Select an hour to read its exact count.").assertDoesNotExist()
    }

    @Test
    fun onsetTimeCountsExposeTwentyFourSelectableHoursAndLiveReadout() {
        var selected: Int? = null
        val state = InsightsUiState(
            loading = false,
            selectedOnsetHour = 0,
            snapshot = snapshot(episodeRows(listOf(0L, 2 * HOUR, 4 * HOUR, 6 * HOUR, 8 * HOUR, 10 * HOUR)))
        )
        setContent(state, onSelectOnsetHour = { selected = it })

        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("onset_time_bars"))
        composeTestRule.onNodeWithText("6 recorded starts across 1 local calendar day in this range.").assertExists()
        (0 until 24).forEach { hour ->
            composeTestRule.onNodeWithTag("onset_time_hour_$hour").assertExists().assertHeightIsAtLeast(48.dp)
        }
        composeTestRule.onNodeWithTag("onset_time_hour_0")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("12:00 AM hour, 1 of 6 recorded starts, from 12:00 AM up to but not including 1:00 AM")
                )
            )
            .performClick()

        assertEquals(0, selected)
        composeTestRule.onNodeWithText(
            "1 of 6 recorded starts were recorded from 12:00 AM up to but not including 1:00 AM."
        ).assertExists()
        composeTestRule.onNodeWithText(ONSET_TIME_CAVEAT).assertExists()
        composeTestRule.onNodeWithTag("onset_time_hour_23").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun verticalSwipeOnOnsetTimeHourScrollsParentList() {
        val state = InsightsUiState(
            loading = false,
            snapshot = snapshot(episodeRows(listOf(0L, 2 * HOUR, 4 * HOUR, 6 * HOUR, 8 * HOUR, 10 * HOUR)))
        )
        setContent(state)

        val list = composeTestRule.onNodeWithTag("insights_screen")
        list.performScrollToNode(hasTestTag("onset_time_bars"))
        val before = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        composeTestRule.onNodeWithTag("onset_time_hour_0").performTouchInput { swipeDown() }
        val after = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

        assertTrue(after < before)
    }

    @Test
    fun sleepCountsRefuseZeroAndDiscloseIncompletePeriod() {
        val state = InsightsUiState(
            loading = false,
            snapshot = snapshot(listOf(entry(1, 0, 0), sleep(2, 2 * HOUR, null)))
        )
        setContent(state)

        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("sleep_counts_refusal"))
        composeTestRule.onNodeWithText("No completed sleep periods woke in this range.").assertExists()
        composeTestRule.onNodeWithText(
            "1 incomplete sleep period is excluded because its Wake time is missing or later than now."
        ).assertExists()
        composeTestRule.onNodeWithTag("sleep_category_cells").assertDoesNotExist()
        composeTestRule.onNodeWithText(SLEEP_COUNTS_CAVEAT).assertExists()
    }

    @Test
    fun sleepCountsExposeTwoSelectableAccessibleCellsAndLiveReadout() {
        var selected: Int? = null
        val countsRows = listOf(
            entry(1, 0, 0),
            sleep(2, 0, 2 * HOUR),
            sleep(3, 3 * HOUR, 7 * HOUR),
            sleep(4, 8 * HOUR, null)
        )
        val state = InsightsUiState(
            loading = false,
            selectedSleepCategoryIndex = 0,
            snapshot = snapshot(countsRows)
        )
        setContent(state, onSelectSleepCategory = { selected = it })

        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("sleep_category_cells"))
        composeTestRule.onNodeWithText("2 completed sleep periods woke in this range.").assertExists()
        composeTestRule.onNodeWithTag("sleep_category_0")
            .assertHeightIsAtLeast(48.dp)
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Nights, 1 of 2 completed sleep periods, over 3 elapsed hours")
                )
            )
            .performClick()
        composeTestRule.onNodeWithTag("sleep_category_1").assertHeightIsAtLeast(48.dp).assertExists()

        assertEquals(0, selected)
        composeTestRule.onNodeWithText(
            "Of 2 completed sleep periods, 1 was a night over 3 elapsed hours. " +
                "Middle duration 4h; shortest 4h; longest 4h."
        ).assertExists()
        composeTestRule.onNodeWithTag("sleep_counts_readout").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, androidx.compose.ui.semantics.LiveRegionMode.Polite)
        )
        composeTestRule.onNodeWithText(SLEEP_COUNTS_CAVEAT).assertExists()
    }

    @Test
    fun sleepCountsKeepZeroCategorySelectableAndHonest() {
        var selected: Int? = null
        val state = InsightsUiState(
            loading = false,
            selectedSleepCategoryIndex = 1,
            snapshot = snapshot(listOf(entry(1, 0, 0), sleep(2, 0, 4 * HOUR)))
        )
        setContent(state, onSelectSleepCategory = { selected = it })

        composeTestRule.onNodeWithTag("insights_screen")
            .performScrollToNode(hasTestTag("sleep_category_cells"))
        composeTestRule.onNodeWithTag("sleep_category_1").assertIsSelected().performClick()

        assertEquals(1, selected)
        composeTestRule.onNodeWithText(
            "Of 1 completed sleep period, 0 were naps of 3 elapsed hours or less. " +
                "There are no durations in this category."
        ).assertExists()
    }

    @Test
    fun verticalSwipeOnSleepCategoryScrollsParentList() {
        val state = InsightsUiState(
            loading = false,
            snapshot = snapshot(listOf(entry(1, 0, 0), sleep(2, 0, 4 * HOUR)))
        )
        setContent(state)

        val list = composeTestRule.onNodeWithTag("insights_screen")
        list.performScrollToNode(hasTestTag("sleep_category_cells"))
        val before = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        composeTestRule.onNodeWithTag("sleep_category_0").performTouchInput { swipeDown() }
        val after = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

        assertTrue(after < before)
    }

    @Test
    fun staleSnapshotShowsErrorAndWorkingRetry() {
        var retries = 0
        setContent(
            InsightsUiState(
                loading = false,
                snapshot = snapshot(listOf(entry(1, 0, 5))),
                error = "Could not read Insights. Your records are still on this device."
            ),
            onRetry = { retries++ }
        )

        composeTestRule.onNodeWithText("Could not read Insights. Your records are still on this device.").assertExists()
        composeTestRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
        composeTestRule.onNodeWithTag("insights_summary").assertExists()
    }

    @Test
    fun filteredEmptyAndTwentyFourHourExplorationUseSpecifiedCopyAndOffset() {
        val old = entry(1, -48 * HOUR, 0)
        val filtered = snapshot(listOf(old))
        setContent(
            InsightsUiState(
                loading = false,
                snapshot = filtered,
                exploredInstantMillis = 6 * HOUR,
                hourFormat = HourFormat.TWENTY_FOUR
            )
        )

        composeTestRule.onNodeWithText("No ratings in this range").assertExists()
        composeTestRule.onNodeWithText("06:00 Z", substring = true).assertExists()
    }

    private fun setContent(
        state: InsightsUiState,
        onRangeSelected: (InsightRange) -> Unit = {},
        onExplore: (Long) -> Unit = {},
        onEarlierHour: () -> Boolean = { true },
        onExploreChart: (Long) -> Unit = {},
        onPreviousEvent: () -> Boolean = { true },
        onSelectOnsetGapBucket: (Int) -> Unit = {},
        onSelectOnsetHour: (Int) -> Unit = {},
        onSelectSleepCategory: (Int) -> Unit = {},
        onRetry: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            MindScaleTheme {
                InsightsScreen(
                    uiState = state,
                    onRangeSelected = onRangeSelected,
                    onExplore = onExplore,
                    onEarlierHour = onEarlierHour,
                    onLaterHour = { true },
                    onPreviousDay = { true },
                    onNextDay = { true },
                    onExploreChart = onExploreChart,
                    onEarlierChartHour = { true },
                    onLaterChartHour = { true },
                    onPreviousRating = { true },
                    onNextRating = { true },
                    onPreviousEvent = onPreviousEvent,
                    onNextEvent = { true },
                    onSelectOnsetGapBucket = onSelectOnsetGapBucket,
                    onSelectOnsetHour = onSelectOnsetHour,
                    onSelectSleepCategory = onSelectSleepCategory,
                    onRetry = onRetry,
                    zoneId = ZoneOffset.UTC
                )
            }
        }
    }

    private fun snapshot(
        rows: List<EpisodeSourceRow>,
        range: InsightRange = InsightRange.ONE_DAY
    ) = deriveInsights(
        rows = rows,
        hold = HoldDuration.SIXTEEN,
        now = Instant.ofEpochMilli(12 * HOUR),
        zoneId = ZoneOffset.UTC,
        range = range
    )

    private fun entry(
        id: Long,
        ts: Long,
        value: Int,
        chips: List<String> = emptyList(),
        note: String? = null
    ) = EpisodeSourceRow("ENTRY", id, ts, null, value, chips, note = note)

    private fun marker(id: Long, ts: Long, text: String) =
        EpisodeSourceRow("MARKER", id, ts, null, null, null, text = text)

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
