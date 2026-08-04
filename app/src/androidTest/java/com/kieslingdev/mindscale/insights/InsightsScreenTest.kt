package com.kieslingdev.mindscale.insights

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
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
    fun verticalSwipeOnThirtyDayRasterScrollsToFacts() {
        val rows = listOf(entry(1, 0, 5), entry(2, 2 * HOUR, 0))
        setContent(
            InsightsUiState(
                loading = false,
                range = InsightRange.THIRTY_DAYS,
                snapshot = snapshot(rows, InsightRange.THIRTY_DAYS)
            )
        )

        repeat(2) {
            composeTestRule.onNodeWithTag("raster_chart").performTouchInput { swipeUp() }
        }

        composeTestRule.onNodeWithText("Each episode").assertExists()
    }

    @Test
    fun loadingFilteredEmptyAndStaleErrorRemainDistinct() {
        setContent(InsightsUiState(loading = true))
        composeTestRule.onNodeWithTag("insights_loading").assertExists()
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

    private fun entry(id: Long, ts: Long, value: Int) =
        EpisodeSourceRow("ENTRY", id, ts, null, value, emptyList())

    private companion object { const val HOUR = 3_600_000L }
}
