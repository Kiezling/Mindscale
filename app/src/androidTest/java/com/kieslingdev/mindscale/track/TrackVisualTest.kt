package com.kieslingdev.mindscale.track

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.kieslingdev.mindscale.data.Entry
import com.kieslingdev.mindscale.data.EntryKind
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The geometry `docs/specs/SPEC-track-and-log-visual.md` freezes for Track, asserted rather than
 * eyeballed.
 *
 * These are new assertions on a restyled screen. They replace nothing: `TrackScreenTest` still owns
 * Track's behavior, is untouched by this phase, and must keep passing — a break there would mean
 * behavior changed, which D-1 forbids.
 */
class TrackVisualTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** A pixel and a half of slack, so an odd-width rounding cannot fail an exact-centre claim. */
    private val tolerancePx = 1.5f

    private fun setContent(
        uiState: TrackUiState = TrackUiState(),
        fontScale: Float = 1f
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MindScaleTheme {
                    TrackScreen(uiState = uiState, onEvent = {})
                }
            }
        }
    }

    private fun keyBounds(value: Int): Rect =
        composeTestRule.onNodeWithTag("numpad_key_$value").fetchSemanticsNode().boundsInRoot

    // ── L-1: the final row is centred on the pad's axis ──────────────────────

    /**
     * D-6. The prototype's `order` array leaves a two-thirds-empty final row with `0` in column one
     * and `10` in column three. Centred means the midpoint of the pair equals the midpoint of the
     * grid above it, while both keys keep the grid's column width.
     */
    @Test
    fun theFinalNumpadRowIsCentredOnThePadsAxis() {
        setContent()

        val gridLeft = keyBounds(1).left
        val gridRight = keyBounds(3).right
        val gridCentre = (gridLeft + gridRight) / 2f

        val zero = keyBounds(0)
        val ten = keyBounds(10)
        val pairCentre = (zero.left + ten.right) / 2f

        assertEquals("final row is not centred on the pad's axis", gridCentre, pairCentre, tolerancePx)

        // And it really moved: both keys are inboard of the columns the prototype puts them in.
        assertTrue("0 should sit inboard of column one", zero.left > gridLeft + tolerancePx)
        assertTrue("10 should sit inboard of column three", ten.right < gridRight - tolerancePx)
    }

    /**
     * "Keeping the 3-column rhythm and both keys' size" is the part of L-1 that weighted spacers
     * would have broken: four children at weights 0.5/1/1/0.5 consume three gaps instead of two,
     * which shrinks `0` and `10` relative to `1` through `9`.
     */
    @Test
    fun allTwelveNumpadKeysShareOneWidthAndOneHeight() {
        setContent()

        val bounds = (0..10).map { it to keyBounds(it) }
        val referenceWidth = bounds.first().second.width
        val referenceHeight = bounds.first().second.height
        bounds.forEach { (value, rect) ->
            assertEquals("key $value width", referenceWidth, rect.width, tolerancePx)
            assertEquals("key $value height", referenceHeight, rect.height, tolerancePx)
        }
        // Circular, so width and height agree.
        assertEquals("keys are not circular", referenceWidth, referenceHeight, tolerancePx)
    }

    /** The final row keeps the grid's own gap, not a gap the centring invented. */
    @Test
    fun theFinalNumpadRowKeepsTheGridsGap() {
        setContent()

        val gridGap = keyBounds(2).left - keyBounds(1).right
        val edgeGap = keyBounds(10).left - keyBounds(0).right

        assertEquals("final row gap differs from the grid gap", gridGap, edgeGap, tolerancePx)
    }

    /** Invariant 12's order and grouping, which L-1 must not disturb. */
    @Test
    fun theEdgeGroupStaysBelowTheGridAndInOrder() {
        setContent()

        val gridBottom = (1..9).maxOf { keyBounds(it).bottom }
        val edgeTop = listOf(0, 10).minOf { keyBounds(it).top }

        assertTrue("0/10 must stay below the 1-9 grid", edgeTop >= gridBottom - tolerancePx)
        assertTrue("0 must stay left of 10", keyBounds(0).left < keyBounds(10).left)
    }

    // ── the armed pad (D-5) ──────────────────────────────────────────────────

    /**
     * Both armed rings reserve their space in every state, so arming changes what the pad looks like
     * and never where anything is. A ring that reflowed the pad would move a key under the user's
     * finger between the tap that arms and the tap that records.
     *
     * The ring's *colour* is a border and is absent from the semantics tree, so its appearance is
     * verified by installed-app capture rather than here. Stated rather than implied.
     */
    @Test
    fun armingThePadDoesNotMoveOrResizeAnyKey() {
        var state by mutableStateOf(TrackUiState(armedCapture = null))
        composeTestRule.setContent {
            MindScaleTheme { TrackScreen(uiState = state, onEvent = {}) }
        }

        val unarmed = (0..10).associateWith { keyBounds(it) }

        composeTestRule.runOnIdle { state = TrackUiState(armedCapture = EntryKind.SLEEP) }
        composeTestRule.waitForIdle()

        unarmed.forEach { (value, before) ->
            val after = keyBounds(value)
            assertEquals("key $value left moved on arming", before.left, after.left, tolerancePx)
            assertEquals("key $value top moved on arming", before.top, after.top, tolerancePx)
            assertEquals("key $value width changed on arming", before.width, after.width, tolerancePx)
            assertEquals("key $value height changed on arming", before.height, after.height, tolerancePx)
        }
    }

    // ── touch targets (D-23) ─────────────────────────────────────────────────

    @Test
    fun everyNumpadKeyReachesTheTouchTargetFloor() {
        setContent()

        (0..10).forEach { value ->
            composeTestRule.onNodeWithTag("numpad_key_$value")
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun bothSleepWakeTogglesReachTheTouchTargetFloor() {
        setContent(uiState = TrackUiState(sleepOn = true))

        composeTestRule.onNodeWithTag("sleep_button").assertHeightIsAtLeast(48.dp)
        composeTestRule.onNodeWithTag("wake_button").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun theHelpToggleReachesTheTouchTargetFloor() {
        setContent()

        composeTestRule.onNodeWithTag("help_toggle_button")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    // ── L-2: the entry row's three actions (D-9) ──────────────────────────────

    private fun actionBounds(description: String): Rect =
        composeTestRule.onAllNodes(hasContentDescription(description), useUnmergedTree = true)
            .onFirst()
            .fetchSemanticsNode()
            .boundsInRoot

    private fun assertActionsShareOneBaselineWithEvenGaps() {
        val edit = actionBounds("Edit entry with value 6")
        val note = actionBounds("Edit note for entry with value 6")
        val delete = actionBounds("Delete entry with value 6")

        val centres = listOf(edit, note, delete).map { it.top + it.height / 2f }
        centres.zipWithNext { a, b ->
            assertEquals("the three actions do not share one baseline", a, b, tolerancePx)
        }

        val firstGap = note.left - edit.right
        val secondGap = delete.left - note.right
        assertEquals("the gaps between the actions are uneven", firstGap, secondGap, tolerancePx)

        // Left to right in the order the row declares them.
        assertTrue("Edit must precede Note", edit.left < note.left)
        assertTrue("Note must precede Delete", note.left < delete.left)
    }

    /**
     * L-2. The prototype stacks these vertically with a ragged edge; D-22 requires one baseline,
     * even gaps, and a consistent gutter.
     */
    @Test
    fun theEntryRowActionsShareOneBaselineWithEvenGaps() {
        setContent(uiState = TrackUiState(recentEntries = listOf(SAMPLE_ENTRY), isEmpty = false))

        composeTestRule.onNodeWithTag("track_screen")
            .performScrollToNode(hasContentDescription("Delete entry with value 6"))

        assertActionsShareOneBaselineWithEvenGaps()
    }

    /**
     * The reason L-2's placement is derived rather than copied. At 200% font three 48 dp targets
     * plus gaps are wider than the room the prototype's third grid column would have left, so the
     * actions live on their own full-width line — and that is what has to survive the scale.
     */
    @Test
    fun theEntryRowActionsStillShareOneBaselineAt200PercentFont() {
        setContent(
            uiState = TrackUiState(recentEntries = listOf(SAMPLE_ENTRY), isEmpty = false),
            fontScale = 2f
        )

        composeTestRule.onNodeWithTag("track_screen")
            .performScrollToNode(hasContentDescription("Delete entry with value 6"))

        assertActionsShareOneBaselineWithEvenGaps()
    }

    /** The three actions each stay a 48 dp target. */
    @Test
    fun theEntryRowActionsReachTheTouchTargetFloor() {
        setContent(uiState = TrackUiState(recentEntries = listOf(SAMPLE_ENTRY), isEmpty = false))

        composeTestRule.onNodeWithTag("track_screen")
            .performScrollToNode(hasContentDescription("Delete entry with value 6"))

        val floor = with(composeTestRule.density) { 48.dp.toPx() }
        listOf(
            "Edit entry with value 6",
            "Edit note for entry with value 6",
            "Delete entry with value 6"
        ).forEach { description ->
            val bounds = actionBounds(description)
            assertTrue(
                "$description is ${bounds.width}x${bounds.height} px, below the 48 dp floor",
                bounds.width >= floor - tolerancePx && bounds.height >= floor - tolerancePx
            )
        }
    }

    // ── the toast pill (D-10, D-20) ──────────────────────────────────────────

    /**
     * The design's toast hugs its text. The pre-Phase-16 banner was a full-bleed `Surface`, so
     * "narrower than the screen, and centred" is the assertion that distinguishes them.
     */
    @Test
    fun theToastIsAPillThatHugsItsTextRatherThanAFullBleedBanner() {
        setContent(uiState = TrackUiState(toast = "Event marked"))

        val screen = composeTestRule.onNodeWithTag("track_screen").fetchSemanticsNode().boundsInRoot
        val toast = composeTestRule.onNodeWithTag("toast_banner").fetchSemanticsNode().boundsInRoot

        assertTrue(
            "the toast is ${toast.width} px wide against a ${screen.width} px screen",
            toast.width < screen.width * 0.9f
        )
        val leftSlack = toast.left - screen.left
        val rightSlack = screen.right - toast.right
        assertEquals("the toast is not centred", leftSlack, rightSlack, tolerancePx)
    }

    private companion object {
        val SAMPLE_ENTRY = Entry(
            id = 1L,
            ts = 1_000L,
            value = 6,
            kind = EntryKind.SLEEP,
            chips = listOf("flat")
        )
    }
}
