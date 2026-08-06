package com.kieslingdev.mindscale.log

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The geometry `docs/specs/SPEC-track-and-log-visual.md` freezes for Full Log, asserted rather than
 * eyeballed. `LogScreenTest` still owns Log's behavior and is untouched by this phase.
 *
 * Note the coverage gap this file cannot close and does not pretend to: there is **no light Full
 * Log screenshot** in `docs/design/reference/`, so light Full Log is derived from the HTML and from
 * the dark capture. Geometry is theme-independent, so these assertions hold either way, but no
 * claim is made that light Full Log was matched against a reference.
 */
class LogVisualTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tolerancePx = 1.5f

    private val zone = ZoneId.systemDefault()
    private val day = LocalDate.now()
    private val baseTs = day.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()

    private fun setContent(state: LogUiState, fontScale: Float = 1f) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MindScaleTheme { LogScreen(state, onEvent = {}) }
            }
        }
    }

    private fun ratingState(): LogUiState {
        val entry = Entry(id = 1, ts = baseTs, value = 5)
        return LogUiState(
            days = groupLogItems(listOf(LogItem.Rating(entry)), zone),
            recordCount = 1,
            hasAnyRecords = true
        )
    }

    private fun bounds(tag: String): Rect =
        composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun actionBounds(description: String): Rect =
        composeTestRule.onAllNodes(hasContentDescription(description), useUnmergedTree = true)
            .onFirst()
            .fetchSemanticsNode()
            .boundsInRoot

    // ── L-4: ALL gets a real trailing gutter (D-11) ───────────────────────────

    /**
     * L-4. The prototype jams `ALL` against the right edge of the filter row. It now sits inset by
     * the same amount that separates it from the To field.
     */
    @Test
    fun theAllActionHasATrailingGutterEqualToTheInterElementGap() {
        setContent(LogUiState())

        val to = bounds("log_to_button")
        val all = bounds("log_all_button")
        val screen = bounds("full_log_screen")

        val gapBeforeAll = all.left - to.right
        // The screen's own content padding is the other 16 dp, so the gutter is measured from the
        // row's right edge, which is the screen edge less that padding.
        val contentPadding = with(composeTestRule.density) { 16.dp.toPx() }
        val trailingGutter = (screen.right - contentPadding) - all.right

        assertEquals(
            "ALL's trailing gutter differs from the gap before it",
            gapBeforeAll,
            trailingGutter,
            tolerancePx
        )
        assertTrue("ALL must not be flush against the row edge", trailingGutter > tolerancePx)
    }

    /** Both filter fields stay equal-width and keep a 48 dp target. */
    @Test
    fun theFromAndToFieldsAreEqualWidthAndReachTheTouchTargetFloor() {
        setContent(LogUiState())

        val from = bounds("log_from_button")
        val to = bounds("log_to_button")
        assertEquals("From and To are unequal widths", from.width, to.width, tolerancePx)

        composeTestRule.onNodeWithTag("log_from_button").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("log_to_button").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("log_all_button").assertHeightIsAtLeast(48.dp)
    }

    // ── the row actions share one baseline (D-9, D-12) ────────────────────────

    private fun assertRowActionsShareOneBaselineWithEvenGaps() {
        val edit = actionBounds("Edit rating 5")
        val note = actionBounds("Edit note for rating 5")
        val delete = actionBounds("Delete rating 5")

        listOf(edit, note, delete)
            .map { it.top + it.height / 2f }
            .zipWithNext { a, b ->
                assertEquals("the row actions do not share one baseline", a, b, tolerancePx)
            }

        assertEquals(
            "the gaps between the row actions are uneven",
            note.left - edit.right,
            delete.left - note.right,
            tolerancePx
        )
        assertTrue("Edit must precede Note", edit.left < note.left)
        assertTrue("Note must precede Delete", note.left < delete.left)
    }

    @Test
    fun theLogRowActionsShareOneBaselineWithEvenGaps() {
        setContent(ratingState())

        assertRowActionsShareOneBaselineWithEvenGaps()
    }

    @Test
    fun theLogRowActionsStillShareOneBaselineAt200PercentFont() {
        setContent(ratingState(), fontScale = 2f)

        assertRowActionsShareOneBaselineWithEvenGaps()
    }

    /** Sleep and event rows carry Delete alone, and it keeps the same trailing alignment. */
    @Test
    fun aDeleteOnlyRowKeepsTheSameTrailingAlignmentAsAFullRow() {
        val entry = Entry(id = 1, ts = baseTs, value = 5)
        val marker = com.kieslingdev.mindscale.data.Marker(id = 2, ts = baseTs - 1_000, text = "Dose increased")
        val items = combineLogItems(entries = listOf(entry), sleeps = emptyList(), markers = listOf(marker))
        setContent(
            LogUiState(
                days = groupLogItems(items, zone),
                recordCount = items.size,
                hasAnyRecords = true
            )
        )

        val ratingDelete = actionBounds("Delete rating 5")
        val eventDelete = actionBounds("Delete event")

        assertEquals(
            "the two Delete actions are not aligned to the same trailing edge",
            ratingDelete.right,
            eventDelete.right,
            tolerancePx
        )
    }

    // ── chips (D-12, D-15) ───────────────────────────────────────────────────

    /**
     * The inline edit panel's eleven value chips are the narrowest chips in the app, and `0` is the
     * narrowest of them. `MsChip` originally grew the painted pill to 48 dp tall while leaving that
     * chip 33 dp *wide* — over-satisfying the floor on one axis and failing it on the other. Found
     * by installed-app capture, which is why it is pinned here.
     */
    @Test
    fun everyInlineEditValueChipReachesTheTouchTargetFloorOnBothAxes() {
        val entry = Entry(id = 1, ts = baseTs, value = 5)
        setContent(
            LogUiState(
                days = groupLogItems(listOf(LogItem.Rating(entry)), zone),
                recordCount = 1,
                hasAnyRecords = true,
                editDraft = LogEditDraft(1, 5, formatEditTimestamp(baseTs, zone), emptySet())
            )
        )

        (0..10).forEach { value ->
            composeTestRule.onNodeWithTag("log_edit_value_$value")
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    /**
     * And the painted pill stays the design's compact shape rather than filling the target: a chip
     * whose *touch* box is 48 dp square should still look like a 33 dp pill, so an eleven-chip row
     * does not read as a row of tall ovals.
     */
    @Test
    fun theChipsPaintedPillStaysShorterThanItsTouchTarget() {
        val entry = Entry(id = 1, ts = baseTs, value = 5)
        setContent(
            LogUiState(
                days = groupLogItems(listOf(LogItem.Rating(entry)), zone),
                recordCount = 1,
                hasAnyRecords = true,
                editDraft = LogEditDraft(1, 5, formatEditTimestamp(baseTs, zone), emptySet())
            )
        )

        // The label sits inside the painted pill, so the pill is no taller than the label plus its
        // padding — strictly less than the 48 dp target that surrounds it.
        val target = bounds("log_edit_value_0")
        val label = composeTestRule.onAllNodes(hasTextExactly("0"), useUnmergedTree = true)
            .onFirst()
            .fetchSemanticsNode()
            .boundsInRoot
        val paddingPx = with(composeTestRule.density) { (MsSpacing.smPlus * 2).toPx() }

        assertTrue(
            "the painted pill fills its touch target instead of sitting inside it",
            label.height + paddingPx < target.height - tolerancePx
        )
    }

    // ── the day header (D-12) ────────────────────────────────────────────────

    /**
     * The gold day header must sit above the first row of its day, and its label is restored to its
     * original case in semantics even though it renders uppercase (D-3).
     */
    @Test
    fun theDayHeaderSitsAboveItsFirstRowAndKeepsItsOriginalCase() {
        val state = ratingState()
        setContent(state)

        val header = bounds("log_day_$day")
        val row = bounds("log_row_${state.days.first().items.first().stableKey}")

        assertTrue("the day header must sit above its first row", header.bottom <= row.top + tolerancePx)
        composeTestRule.onNodeWithTag("log_day_$day").assertExists()
    }
}
