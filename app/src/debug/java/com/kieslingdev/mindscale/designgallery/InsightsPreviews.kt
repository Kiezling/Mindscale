package com.kieslingdev.mindscale.designgallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kieslingdev.mindscale.data.EpisodeSourceRow
import com.kieslingdev.mindscale.data.HoldDuration
import com.kieslingdev.mindscale.data.ThemeMode
import com.kieslingdev.mindscale.insights.InsightRange
import com.kieslingdev.mindscale.insights.InsightsScreen
import com.kieslingdev.mindscale.insights.InsightsUiState
import com.kieslingdev.mindscale.insights.deriveInsights
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import com.kieslingdev.mindscale.ui.theme.ms
import java.time.Instant
import java.time.ZoneOffset

/**
 * `@Preview` over Insights in both themes at 100% and 200% font
 * (`docs/specs/SPEC-insights-visual.md`, its UI/ACCESSIBILITY criterion).
 *
 * Debug source set only, like the gallery beside it, and for the same reason: these touch no
 * `ViewModel`, no DAO, and no database. `deriveInsights` is a pure function over literal rows, and
 * `InsightsScreen` is stateless by design and takes its whole state as a parameter, so nothing here
 * can read, write, or fabricate user data.
 *
 * The previews are a fast loop, not the oracle. The installed-app capture against
 * `docs/design/reference/` is what actually proves fidelity, and it is what found Phase 15's and
 * Phase 16's production defects.
 */

private const val HOUR = 3_600_000L
private val PreviewNow: Instant = Instant.ofEpochMilli(30 * 24 * HOUR)

private fun entry(id: Long, ts: Long, value: Int, chips: List<String> = emptyList()) =
    EpisodeSourceRow("ENTRY", id, ts, null, value, chips)

private fun sleep(id: Long, start: Long, end: Long?) =
    EpisodeSourceRow("SLEEP", id, start, end, null, null)

private fun marker(id: Long, ts: Long, text: String) =
    EpisodeSourceRow("MARKER", id, ts, null, null, null, text = text)

/**
 * Six onsets and two completed sleep periods: enough for every section to render eligible rather
 * than refusing, which is the state the design's screenshots show.
 */
private val PreviewRows: List<EpisodeSourceRow> = buildList {
    var id = 1L
    listOf(2L, 6L, 11L, 17L, 22L, 27L).forEachIndexed { index, day ->
        val onset = day * 24 * HOUR + (9 + index) * HOUR
        add(entry(id++, onset, 4 + index % 6, chips = listOf("flat")))
        add(entry(id++, onset + 3 * HOUR, 7))
        add(entry(id++, onset + 9 * HOUR, 0))
    }
    add(sleep(id++, 24 * 24 * HOUR, 24 * 24 * HOUR + 8 * HOUR))
    add(sleep(id++, 25 * 24 * HOUR, 25 * 24 * HOUR + 2 * HOUR))
    add(marker(id++, 22 * 24 * HOUR + 12 * HOUR, "dose change"))
}

private fun previewInsightsState(
    rows: List<EpisodeSourceRow> = PreviewRows,
    selectedGapBucket: Int? = null,
    selectedHour: Int? = null,
    selectedSleepCategory: Int? = null,
    error: String? = null
) = InsightsUiState(
    range = InsightRange.THIRTY_DAYS,
    loading = false,
    snapshot = deriveInsights(
        rows = rows,
        hold = HoldDuration.SIXTEEN,
        now = PreviewNow,
        zoneId = ZoneOffset.UTC,
        range = InsightRange.THIRTY_DAYS
    ),
    selectedOnsetGapBucketIndex = selectedGapBucket,
    selectedOnsetHour = selectedHour,
    selectedSleepCategoryIndex = selectedSleepCategory,
    error = error
)

@Composable
private fun OnInsightsPage(themeMode: ThemeMode, content: @Composable () -> Unit) {
    MindScaleTheme(themeMode = themeMode) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.ms.bg)) { content() }
    }
}

@Composable
private fun PreviewInsights(state: InsightsUiState) {
    InsightsScreen(
        uiState = state,
        onRangeSelected = {},
        onExplore = {},
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

@Preview(name = "Insights light 100%", heightDp = 2600)
@Composable
private fun InsightsLightPreview() = OnInsightsPage(ThemeMode.LIGHT) {
    PreviewInsights(previewInsightsState())
}

@Preview(name = "Insights dark 100%", heightDp = 2600)
@Composable
private fun InsightsDarkPreview() = OnInsightsPage(ThemeMode.DARK) {
    PreviewInsights(previewInsightsState())
}

@Preview(name = "Insights light 200%", heightDp = 4200, fontScale = 2.0f)
@Composable
private fun InsightsLightLargeFontPreview() = OnInsightsPage(ThemeMode.LIGHT) {
    PreviewInsights(previewInsightsState())
}

@Preview(name = "Insights dark 200%", heightDp = 4200, fontScale = 2.0f)
@Composable
private fun InsightsDarkLargeFontPreview() = OnInsightsPage(ThemeMode.DARK) {
    PreviewInsights(previewInsightsState())
}

/** Every histogram and count cell inverted to its ink fill at once, which no capture shows. */
@Preview(name = "Insights selected cells dark", heightDp = 2600)
@Composable
private fun InsightsSelectedCellsPreview() = OnInsightsPage(ThemeMode.DARK) {
    PreviewInsights(
        previewInsightsState(
            selectedGapBucket = 3,
            selectedHour = 9,
            selectedSleepCategory = 0
        )
    )
}

/** The refusal panels, which need a range too sparse for any histogram to be eligible. */
@Preview(name = "Insights refusals light", heightDp = 1600)
@Composable
private fun InsightsRefusalPreview() = OnInsightsPage(ThemeMode.LIGHT) {
    PreviewInsights(
        previewInsightsState(
            rows = listOf(
                entry(1, 27 * 24 * HOUR, 5),
                entry(2, 27 * 24 * HOUR + 4 * HOUR, 0)
            )
        )
    )
}

/** The empty state, which is a terminal state rather than a placeholder. */
@Preview(name = "Insights empty dark", heightDp = 900)
@Composable
private fun InsightsEmptyPreview() = OnInsightsPage(ThemeMode.DARK) {
    PreviewInsights(previewInsightsState(rows = emptyList()))
}

/** The stale-snapshot banner, which sits above a snapshot rather than replacing it. */
@Preview(name = "Insights stale error light", heightDp = 1600)
@Composable
private fun InsightsErrorPreview() = OnInsightsPage(ThemeMode.LIGHT) {
    PreviewInsights(
        previewInsightsState(
            error = "Could not read Insights. Your records are still on this device."
        )
    )
}
