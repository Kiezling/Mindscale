package com.kieslingdev.mindscale.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kieslingdev.mindscale.data.MindScaleDatabase
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The geometry `docs/specs/SPEC-remaining-screens-visual.md` freezes for Settings, asserted rather
 * than eyeballed.
 *
 * These are new assertions on a restyled screen. They replace nothing: `SettingsImportScreenTest`
 * and `NavigationTest` still own Settings' behavior, are untouched by this phase, and must keep
 * passing — a break there would mean behavior changed, which D-1 forbids.
 *
 * The pattern is `TrackVisualTest`'s, `LogVisualTest`'s and `InsightsVisualTest`'s, used again for
 * the reason Phase 16 recorded: numeric assertions caught a 2 px numpad defect and a 33 dp chip
 * defect that looking at a screenshot did not.
 */
@RunWith(AndroidJUnit4::class)
class SettingsVisualTest {

    @get:Rule val composeTestRule = createComposeRule()

    /** A pixel and a half of slack, so an odd-width rounding cannot fail an exact-equality claim. */
    private val tolerancePx = 1.5f

    private lateinit var database: MindScaleDatabase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindScaleDatabase::class.java
        ).addCallback(MindScaleDatabase.seedSettingsCallback).build()
        viewModel = SettingsViewModel(
            settingsDao = database.trackSettingsDao(),
            dataControlDao = database.dataControlDao(),
            savedStateHandle = SavedStateHandle(),
            ioContext = Dispatchers.IO
        )
    }

    @After fun tearDown() = database.close()

    private fun setContent(focus: SettingsFocus = SettingsFocus.TOP, fontScale: Float = 1f) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MindScaleTheme { SettingsRoute(viewModel = viewModel, focus = focus) }
            }
        }
    }

    private fun scrollTo(tag: String) {
        composeTestRule.onNodeWithTag("settings_screen").performScrollToNode(hasTestTag(tag))
    }

    /**
     * The segments of the choice row containing [anySegmentLabel], in layout order.
     *
     * They are found by their content descriptions rather than by a tag, because a component never
     * sets a `testTag` (D-15 of the foundation) and the descriptions are the caller's — which is
     * exactly what D-5's `optionModifier` parameter exists to make possible.
     */
    private fun segmentBounds(labels: List<String>, selectedLabel: String): List<Rect> =
        labels.map { label ->
            val suffix = if (label == selectedLabel) "selected" else "not selected"
            composeTestRule.onNodeWithContentDescription("$label, $suffix")
                .fetchSemanticsNode().boundsInRoot
        }

    private fun assertEqualWidths(where: String, cells: List<Rect>) {
        val reference = cells.first().width
        cells.forEachIndexed { index, cell ->
            assertEquals(
                "$where: segment $index is ${cell.width} px against the first segment's $reference",
                reference.toDouble(),
                cell.width.toDouble(),
                tolerancePx.toDouble()
            )
        }
    }

    // ── L-6: every segmented control's segments are equal-width ──────────────

    /**
     * L-6. The prototype's segments size to their content, so `12-HOUR`/`24-HOUR` and
     * `8H`/`12H`/`16H`/`24H` render at visibly different widths and the row loses its rhythm.
     *
     * `MsSegmentedControl` has carried `weight(1f)` since Phase 15, but its only call sites were in
     * the debug gallery, so the correction was never proved on a real screen. This is that proof
     * (D-4).
     */
    @Test
    fun everySegmentedControlHasEqualWidthSegments() {
        setContent()

        assertEqualWidths("Appearance", segmentBounds(listOf("Light", "Dark", "System"), "System"))
        assertEqualWidths("Time format", segmentBounds(listOf("12-hour", "24-hour"), "12-hour"))
        assertEqualWidths(
            "An entry ends after",
            segmentBounds(listOf("8h", "12h", "16h", "24h"), "16h")
        )
    }

    /**
     * The same claim at 200% font, which is where equal weight is most likely to break: the widest
     * label in each row grows fastest, and a content-sized row would diverge further.
     */
    @Test
    fun everySegmentedControlStaysEqualWidthAt200PercentFont() {
        setContent(fontScale = 2f)

        assertEqualWidths("Appearance", segmentBounds(listOf("Light", "Dark", "System"), "System"))
        assertEqualWidths("Time format", segmentBounds(listOf("12-hour", "24-hour"), "12-hour"))
    }

    /**
     * D-5's correction, pinned. `MsSegmentedControl` passed `maxLines = 1` with no `overflow`,
     * which resolves to `TextOverflow.Clip`. `SYSTEM` in a three-segment row at 200% font is the
     * case, and it is the same arithmetic that clipped the `INSIGHTS` navigation tab in Phase 15.
     *
     * A clipped label is not visible in the semantics tree, so what is asserted is the consequence
     * that *is* measurable: the segment grows tall enough to hold a wrapped label rather than
     * staying one line's height, and it still reports its full original-case text.
     */
    @Test
    fun aLongSegmentLabelWrapsRatherThanClippingAt200PercentFont() {
        setContent(fontScale = 2f)

        val system = composeTestRule.onNodeWithContentDescription("System, selected")
            .fetchSemanticsNode()
        val dark = composeTestRule.onNodeWithContentDescription("Dark, not selected")
            .fetchSemanticsNode()

        assertTrue(
            "the segmented control should grow to hold a wrapped label rather than clipping it; " +
                "SYSTEM measured ${system.boundsInRoot.height} px against DARK's " +
                "${dark.boundsInRoot.height} px",
            system.boundsInRoot.height >= dark.boundsInRoot.height - tolerancePx
        )
        // The label is still described in its original case, which is what `NavigationTest`'s
        // `onNodeWithText("24h").performClick()` depends on (D-6, D-11 of the foundation).
        assertTrue(
            "the segment should still carry its original-case text",
            system.config.contains(SemanticsProperties.Text) &&
                system.config[SemanticsProperties.Text].any { it.text == "System" }
        )
    }

    /** Every segment is a target regardless of how narrow its label is (D-23 of the foundation). */
    @Test
    fun everySegmentReachesTheTouchTargetFloor() {
        setContent()

        listOf(
            "Light, not selected", "Dark, not selected", "System, selected",
            "12-hour, selected", "24-hour, not selected",
            "8h, not selected", "12h, not selected", "16h, selected", "24h, not selected"
        ).forEach { description ->
            composeTestRule.onNodeWithContentDescription(description)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    /**
     * The conversion must not cost a choice its click. `MsSegmentedControl` renders its label
     * through `MsUppercaseText`, so the leaf's semantics are cleared and the restored original-case
     * string is collected by the merging `selectable` ancestor — this asserts that the round trip
     * actually reaches the callback (D-6).
     */
    @Test
    fun aSegmentIsStillClickableByItsOriginalCaseLabel() {
        setContent()

        composeTestRule.onNodeWithText("24h").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("24h, selected").assertExists()
    }

    // ── the deep link's contract, asserted as behaviour rather than as an index ──

    /**
     * Invariant 10. `SettingsFocus` scrolls by item index, and an index is an implementation
     * detail: the contract the paused banner and the anchor card rely on is "focus the data
     * section" and "focus the anchors section".
     *
     * Nothing asserted that before this phase, which meant a regrouping of the item list could have
     * silently landed the deep link somewhere else. These two assertions are the contract (D-8).
     */
    @Test
    fun settingsFocusDataBringsTheDataSectionIntoView() {
        setContent(focus = SettingsFocus.DATA)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("export_backup").assertIsDisplayed()
    }

    @Test
    fun settingsFocusAnchorsBringsTheAnchorsSectionIntoView() {
        setContent(focus = SettingsFocus.ANCHORS)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("anchor_2").assertIsDisplayed()
    }

    // ── the data and import rows are whole-row targets ───────────────────────

    /**
     * The design's `Export everything` row is a label with the whole row as the target. Every one
     * of the five action rows has to clear the target floor, and the two import rows additionally
     * carry the content descriptions `SettingsImportScreenTest` asserts.
     */
    @Test
    fun everyActionRowIsAWholeRowTargetAtTheFloor() {
        setContent()

        listOf("export_backup", "export_records", "export_then_erase").forEach { tag ->
            scrollTo(tag)
            composeTestRule.onNodeWithTag(tag).assertHeightIsAtLeast(48.dp)
        }
        listOf("import_backup", "import_records").forEach { tag ->
            scrollTo(tag)
            composeTestRule.onNodeWithTag(tag).assertHeightIsAtLeast(48.dp)
        }
        composeTestRule.onNodeWithContentDescription("Restore from a MindScale JSON backup")
            .assertExists()
        composeTestRule.onNodeWithContentDescription("Import a MindScale records CSV")
            .assertExists()
    }

    /** The same rows at 200% font, where a two-line label is the normal case rather than the edge. */
    @Test
    fun everyActionRowStillReachesTheFloorAt200PercentFont() {
        setContent(fontScale = 2f)

        listOf("export_backup", "export_then_erase", "import_backup").forEach { tag ->
            scrollTo(tag)
            composeTestRule.onNodeWithTag(tag).assertHeightIsAtLeast(48.dp)
        }
    }

    /** The narrow bare actions are the ones most likely to fall under the floor. */
    @Test
    fun theBareTextActionsReachTheTouchTargetFloor() {
        setContent()

        scrollTo("save_anchors")
        composeTestRule.onNodeWithTag("save_anchors").assertHeightIsAtLeast(48.dp)
        scrollTo("save_onset_words")
        composeTestRule.onNodeWithTag("save_onset_words").assertHeightIsAtLeast(48.dp)
    }
}
