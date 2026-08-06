package com.kieslingdev.mindscale.designgallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.data.Marker
import com.kieslingdev.mindscale.data.SleepInterval
import com.kieslingdev.mindscale.data.ThemeMode
import com.kieslingdev.mindscale.log.LogScreen
import com.kieslingdev.mindscale.log.LogUiState
import com.kieslingdev.mindscale.log.combineLogItems
import com.kieslingdev.mindscale.log.groupLogItems
import com.kieslingdev.mindscale.track.ReadoutState
import com.kieslingdev.mindscale.track.TrackScreen
import com.kieslingdev.mindscale.track.TrackUiState
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import com.kieslingdev.mindscale.ui.theme.ms
import java.time.LocalDate
import java.time.ZoneId

/**
 * `@Preview` over Track and Full Log in both themes at 100% and 200% font
 * (`docs/specs/SPEC-track-and-log-visual.md`, its UI/ACCESSIBILITY criterion).
 *
 * Debug source set only, like the gallery beside it, and for the same reason: these touch no
 * `ViewModel`, no DAO, and no database. Every value is a literal, so nothing here can read, write,
 * or fabricate user data — `TrackScreen` and `LogScreen` are stateless by design and take their
 * whole state as a parameter.
 *
 * The previews are a fast loop, not the oracle. The installed-app capture against
 * `docs/design/reference/` is what actually proves fidelity, and it is what caught Phase 15's one
 * production defect.
 */

private val PreviewZone: ZoneId = ZoneId.systemDefault()
private val PreviewDay: LocalDate = LocalDate.of(2026, 7, 29)
private val PreviewBaseTs: Long =
    PreviewDay.atTime(9, 0).atZone(PreviewZone).toInstant().toEpochMilli()

private val PreviewEntries = listOf(
    Entry(id = 1, ts = PreviewBaseTs, value = 0, kind = null),
    Entry(id = 2, ts = PreviewBaseTs - 3_600_000, value = 4, kind = EntryKind.WAKE),
    Entry(
        id = 3,
        ts = PreviewBaseTs - 36_000_000,
        value = 6,
        kind = EntryKind.SLEEP,
        chips = listOf("flat"),
        note = "Still there at bedtime."
    ),
    Entry(id = 4, ts = PreviewBaseTs - 40_000_000, value = 5, chips = listOf("flat"))
)

private fun previewTrackState(armed: EntryKind? = null) = TrackUiState(
    recentEntries = PreviewEntries,
    isEmpty = false,
    sleepOn = true,
    armedCapture = armed,
    transientReadout = ReadoutState(
        value = 6,
        band = "moderate",
        expiresAtMillis = Long.MAX_VALUE,
        anchor = "Hard to start anything."
    ),
    showCheckin = true
)

private fun previewLogState(): LogUiState {
    val items = combineLogItems(
        entries = PreviewEntries,
        sleeps = listOf(
            SleepInterval(id = 5, startTs = PreviewBaseTs - 36_100_000, endTs = PreviewBaseTs - 3_700_000),
            SleepInterval(id = 6, startTs = PreviewBaseTs - 1_000, endTs = null)
        ),
        markers = listOf(Marker(id = 7, ts = PreviewBaseTs - 40_100_000, text = "Dose increased"))
    )
    return LogUiState(
        days = groupLogItems(items, PreviewZone),
        recordCount = items.size,
        hasAnyRecords = true
    )
}

/** The window background the chrome paints, so a preview is not judged against white. */
@Composable
private fun OnPage(themeMode: ThemeMode, content: @Composable () -> Unit) {
    MindScaleTheme(themeMode = themeMode) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.ms.bg)) { content() }
    }
}

// ── Track ─────────────────────────────────────────────────────────────────────

@Preview(name = "Track light 100%", heightDp = 1400)
@Composable
private fun TrackLightPreview() = OnPage(ThemeMode.LIGHT) {
    TrackScreen(uiState = previewTrackState(), onEvent = {})
}

@Preview(name = "Track dark 100%", heightDp = 1400)
@Composable
private fun TrackDarkPreview() = OnPage(ThemeMode.DARK) {
    TrackScreen(uiState = previewTrackState(), onEvent = {})
}

@Preview(name = "Track light 200%", heightDp = 2600, fontScale = 2.0f)
@Composable
private fun TrackLightLargeFontPreview() = OnPage(ThemeMode.LIGHT) {
    TrackScreen(uiState = previewTrackState(), onEvent = {})
}

@Preview(name = "Track dark 200%", heightDp = 2600, fontScale = 2.0f)
@Composable
private fun TrackDarkLargeFontPreview() = OnPage(ThemeMode.DARK) {
    TrackScreen(uiState = previewTrackState(), onEvent = {})
}

/** The armed pad, which no screenshot in `docs/design/reference/` covers. */
@Preview(name = "Track armed light 100%", heightDp = 1400)
@Composable
private fun TrackArmedLightPreview() = OnPage(ThemeMode.LIGHT) {
    TrackScreen(uiState = previewTrackState(armed = EntryKind.SLEEP), onEvent = {})
}

@Preview(name = "Track armed dark 100%", heightDp = 1400)
@Composable
private fun TrackArmedDarkPreview() = OnPage(ThemeMode.DARK) {
    TrackScreen(uiState = previewTrackState(armed = EntryKind.SLEEP), onEvent = {})
}

/** The empty state, which no screenshot covers either. */
@Preview(name = "Track empty light 100%", heightDp = 1200)
@Composable
private fun TrackEmptyLightPreview() = OnPage(ThemeMode.LIGHT) {
    TrackScreen(uiState = TrackUiState(recentEntries = emptyList(), isEmpty = true), onEvent = {})
}

// ── Full Log ──────────────────────────────────────────────────────────────────

/**
 * There is **no light Full Log screenshot** in `docs/design/reference/`. This preview is derived
 * from the HTML and from the dark capture, and that is stated rather than implied.
 */
@Preview(name = "Log light 100%", heightDp = 1200)
@Composable
private fun LogLightPreview() = OnPage(ThemeMode.LIGHT) {
    LogScreen(previewLogState(), onEvent = {})
}

@Preview(name = "Log dark 100%", heightDp = 1200)
@Composable
private fun LogDarkPreview() = OnPage(ThemeMode.DARK) {
    LogScreen(previewLogState(), onEvent = {})
}

@Preview(name = "Log light 200%", heightDp = 2400, fontScale = 2.0f)
@Composable
private fun LogLightLargeFontPreview() = OnPage(ThemeMode.LIGHT) {
    LogScreen(previewLogState(), onEvent = {})
}

@Preview(name = "Log dark 200%", heightDp = 2400, fontScale = 2.0f)
@Composable
private fun LogDarkLargeFontPreview() = OnPage(ThemeMode.DARK) {
    LogScreen(previewLogState(), onEvent = {})
}

@Preview(name = "Log empty dark 100%", heightDp = 800)
@Composable
private fun LogEmptyDarkPreview() = OnPage(ThemeMode.DARK) {
    LogScreen(LogUiState(), onEvent = {})
}
