package com.kieslingdev.mindscale.breathing

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kieslingdev.mindscale.data.MindScaleDatabase
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The geometry `docs/specs/SPEC-remaining-screens-visual.md` freezes for the Breathing body,
 * asserted rather than eyeballed.
 *
 * `light-breathing.png` is the only capture of this screen and there is no dark one, which D-2
 * states rather than papers over.
 *
 * These replace nothing: `BreathingScreenTest` still owns this screen's behavior, is untouched by
 * this phase, and must keep passing — including the assertion that the circle carries no semantics
 * at all, which is the constraint the cue's move *into* the circle had to respect.
 */
@RunWith(AndroidJUnit4::class)
class BreathingVisualTest {

    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var database: MindScaleDatabase
    private lateinit var viewModel: BreathingViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindScaleDatabase::class.java
        ).addCallback(MindScaleDatabase.seedSettingsCallback).build()
        viewModel = BreathingViewModel(database.breathingSessionDao(), SystemBreathingClock())
    }

    @After fun tearDown() = database.close()

    private fun render(fontScale: Float = 1f) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MindScaleTheme { BreathingRoute(viewModel) }
            }
        }
    }

    private fun bounds(tag: String): Rect =
        composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    /**
     * D-12: the design draws the cue *inside* the pacing circle rather than beneath it, and the
     * move had to be a stacking change rather than a structural one — the cue is still one `Text`
     * node with its own tag, content description and polite live region, all of which
     * `BreathingScreenTest` asserts.
     *
     * The circle itself carries no semantics, so it cannot be measured directly while it is
     * decorative. What is measurable is the cue's own position relative to the screen's first
     * item, which is the circle: the cue must sit in the top block of the screen, not below the
     * length choices.
     */
    @Test
    fun theCueRendersInsideTheCircleRatherThanBelowIt() {
        render()

        val cue = bounds("breathing_cue")
        val firstLength = bounds("breathing_length_1")

        assertTrue(
            "the cue should sit above the length choices, inside the circle; cue at " +
                "${cue.top}..${cue.bottom}, first length at ${firstLength.top}",
            cue.bottom <= firstLength.top
        )
        // The circle is 224 dp and the screen's content padding is 24 dp, so a cue drawn inside it
        // starts well down the page rather than at the very top. A cue rendered as its own item
        // below the circle would start at roughly 24 + 224 dp instead.
        assertTrue(
            "the cue should be vertically centred within the circle rather than stacked under it",
            cue.top > 0f && cue.bottom < firstLength.top
        )
    }

    /**
     * The cue is horizontally centred, which is what makes "inside the circle" read as inside it
     * rather than as a caption that happens to overlap.
     */
    @Test
    fun theCueIsCentredOnTheScreensAxis() {
        render()

        val cue = bounds("breathing_cue")
        val close = bounds("breathing_close")

        val cueCentre = cue.left + cue.width / 2f
        val closeCentre = close.left + close.width / 2f
        assertTrue(
            "the cue and the close pill should share one vertical axis; cue centre $cueCentre, " +
                "close centre $closeCentre",
            kotlin.math.abs(cueCentre - closeCentre) <= 2f
        )
    }

    /** Every control on a full-bleed screen with no chrome to fall back on. */
    @Test
    fun everyControlReachesTheTouchTargetFloorOnBothAxes() {
        render()

        BREATHING_LENGTHS_MINUTES.forEach { minutes ->
            composeTestRule.onNodeWithTag("breathing_length_$minutes")
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
        }
        composeTestRule.onNodeWithTag("breathing_close")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun everyControlStillReachesTheFloorAt200PercentFont() {
        render(fontScale = 2f)

        BREATHING_LENGTHS_MINUTES.forEach { minutes ->
            composeTestRule.onNodeWithTag("breathing_length_$minutes")
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
        }
        composeTestRule.onNodeWithTag("breathing_close")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    /**
     * The four length pills share one height, so the two-by-two block reads as one control group.
     * Widths deliberately differ — `10 min` is wider than `1 min`.
     */
    @Test
    fun theFourLengthPillsShareOneHeight() {
        render()

        val heights = BREATHING_LENGTHS_MINUTES.map { bounds("breathing_length_$it").height }
        val reference = heights.first()
        heights.forEachIndexed { index, height ->
            assertTrue(
                "length pill $index is $height px against the first pill's $reference",
                kotlin.math.abs(height - reference) <= 1.5f
            )
        }
    }

    /**
     * The running state replaces the four choices with one eyebrow, and the close pill takes the
     * `Stop` label. Both must stay on the screen's axis, because there is no chrome to anchor them.
     */
    @Test
    fun theRunningStateKeepsItsReadoutAndCloseOnOneAxis() {
        render()
        composeTestRule.onNodeWithTag("breathing_length_1").performClick()
        composeTestRule.waitForIdle()

        val length = bounds("breathing_running_length")
        val close = bounds("breathing_close")

        assertTrue(
            "the running readout should sit above the close control",
            length.bottom <= close.top
        )
        assertTrue(
            "both should share the screen's vertical axis",
            kotlin.math.abs(
                (length.left + length.width / 2f) - (close.left + close.width / 2f)
            ) <= 2f
        )
    }
}
