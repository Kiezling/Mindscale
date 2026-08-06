package com.kieslingdev.mindscale.report

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kieslingdev.mindscale.data.ExternalInstrument
import com.kieslingdev.mindscale.data.MindScaleDatabase
import com.kieslingdev.mindscale.insights.InsightRange
import com.kieslingdev.mindscale.insights.InsightsUiState
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The geometry `docs/specs/SPEC-remaining-screens-visual.md` freezes for Profile and Report,
 * asserted rather than eyeballed.
 *
 * Neither screen has a reference screenshot in either theme, which is stated in D-2 rather than
 * papered over. That makes these numeric assertions the *only* objective check on this geometry,
 * so they carry more weight here than on Settings.
 *
 * They replace nothing: `ProfileReportScreenTest` and `NavigationTest` still own both screens'
 * behavior, are untouched by this phase, and must keep passing.
 */
@RunWith(AndroidJUnit4::class)
class ProfileReportVisualTest {

    @get:Rule val composeTestRule = createComposeRule()

    /** A pixel and a half of slack, so an odd-width rounding cannot fail an exact-equality claim. */
    private val tolerancePx = 1.5f

    private lateinit var database: MindScaleDatabase
    private lateinit var viewModel: ReportProfileViewModel

    /**
     * A real `ReportProfileViewModel` over an in-memory database, exactly as `SafetyScreenTest`
     * builds its own. The state each test renders is passed to `ProfileScreen` directly, so the
     * ViewModel here supplies callbacks rather than data — these are geometry assertions, and
     * driving the real read path would make them depend on timing they are not about.
     */
    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindScaleDatabase::class.java
        ).addCallback(MindScaleDatabase.seedSettingsCallback).build()
        viewModel = ReportProfileViewModel(
            profileDao = database.profileDao(),
            dataControlDao = database.dataControlDao(),
            insightsState = MutableStateFlow(InsightsUiState())
        )
    }

    @After fun tearDown() = database.close()

    private fun setProfile(state: ReportProfileUiState, fontScale: Float = 1f) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MindScaleTheme {
                    ProfileScreen(
                        uiState = state,
                        viewModel = viewModel,
                        onOpenReport = {},
                        onOpenSettings = {}
                    )
                }
            }
        }
    }

    private fun setReport(state: ReportProfileUiState, fontScale: Float = 1f) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MindScaleTheme {
                    ReportScreen(
                        uiState = state,
                        onRangeSelected = {},
                        onCopy = {},
                        onShare = {},
                        onSave = {},
                        onDiscardPendingSave = {},
                        onRetry = {}
                    )
                }
            }
        }
    }

    private fun bounds(tag: String): Rect =
        composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    // ── Invariant 11: Profile's two navigation rows stay above the fold ──────

    /**
     * `NavigationTest.settingsOpenedFromInsights_returnsToInsights`,
     * `NavigationTest.settingsOpens_hidesBottomNavigation_andBackReturnsToPriorDestination` and
     * `ProfileReportScreenTest.clinicianSummaryShowsPrivacyCopy…` all click `profile_open_settings`
     * or `profile_open_report` **without scrolling first**. That is a geometric constraint on this
     * screen's top, and it was never asserted before this phase — it simply happened to hold.
     *
     * Cards and padding are exactly what would break it, so it is pinned here (D-9, Invariant 11).
     */
    @Test
    fun bothProfileNavigationRowsAreReachableWithoutScrolling() {
        setProfile(ReportProfileUiState())

        composeTestRule.onNodeWithTag("profile_open_report").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_open_settings").assertIsDisplayed()
    }

    /** The same two rows are whole-row targets at the floor, at both scales. */
    @Test
    fun everyProfileNavigationRowReachesTheTouchTargetFloor() {
        setProfile(ReportProfileUiState())

        listOf("profile_open_report", "profile_open_settings", "profile_open_safety").forEach { tag ->
            composeTestRule.onNodeWithTag("profile_screen").performScrollToNode(hasTestTag(tag))
            composeTestRule.onNodeWithTag(tag).assertHeightIsAtLeast(48.dp)
        }
    }

    // ── L-5: the external-total form's action row ────────────────────────────

    /**
     * L-5, on the screen that actually owns the row.
     *
     * `SPEC-visual-foundation.md` D-22 records the flaw as "**Settings'** DATE / PHQ-8 / GAD-7 /
     * ADD row is crowded at the right". The prototype puts that row on its Settings page;
     * MindScale puts external totals on **Profile**, and `SettingsScreen.kt` has no date field, no
     * PHQ-8 control and no `ADD`. Moving the row to match the screenshot would be a navigation
     * change, so D-3 amends the record and the correction lands here.
     *
     * The correction itself is D-22's: even gaps, and a real trailing gutter on the last action
     * rather than letting it sit flush against the card's inner edge. The gutter is on a wrapper
     * `Box`, not on the action's own modifier, so the control's reported bounds stay its touch area
     * and the gutter is measurable from outside — Phase 16's L-4 technique.
     */
    @Test
    fun theExternalTotalActionRowHasEvenGapsAndATrailingGutter() {
        setProfile(
            ReportProfileUiState(
                editingScoreId = 7L,
                scoreInstrument = ExternalInstrument.PHQ_8
            )
        )
        composeTestRule.onNodeWithTag("profile_screen").performScrollToNode(hasTestTag("score_save"))

        val save = bounds("score_save")
        val cancel = bounds("score_edit_cancel")
        val form = bounds("score_form")

        val innerGap = cancel.left - save.right
        val trailingGutter = form.right - cancel.right

        assertTrue(
            "the two actions should not overlap; save ends at ${save.right}, cancel starts at ${cancel.left}",
            innerGap >= 0f
        )
        assertTrue(
            "L-5: the trailing action must not sit flush against the row's edge — the gutter " +
                "measured $trailingGutter px",
            trailingGutter > tolerancePx
        )
        assertTrue(
            "L-5: the trailing gutter ($trailingGutter px) should be at least the inter-element " +
                "gap ($innerGap px), so the row is not crowded at the right",
            trailingGutter + tolerancePx >= innerGap
        )
    }

    /** Both instrument chips are targets on both axes — the `MsChip` floor Phase 16 corrected. */
    @Test
    fun bothInstrumentChipsReachTheTouchTargetFloorOnBothAxes() {
        setProfile(ReportProfileUiState())
        composeTestRule.onNodeWithTag("profile_screen").performScrollToNode(hasTestTag("score_form"))

        ExternalInstrument.entries.forEach { instrument ->
            composeTestRule.onNodeWithTag("score_instrument_${instrument.name}")
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
        }
    }

    /** The narrow bare actions, at 200% font, where a wrapped label is the normal case. */
    @Test
    fun profilesBareActionsStillReachTheFloorAt200PercentFont() {
        setProfile(ReportProfileUiState(), fontScale = 2f)

        composeTestRule.onNodeWithTag("profile_screen")
            .performScrollToNode(hasTestTag("profile_name_save"))
        composeTestRule.onNodeWithTag("profile_name_save").assertHeightIsAtLeast(48.dp)
    }

    // ── Report ───────────────────────────────────────────────────────────────

    /**
     * D-6: Report's six ranges stay a scrolling chip row rather than becoming a segmented control,
     * because six equal segments at 200% font would leave about 55 dp for a label like `90 days`.
     * What that costs is nothing, and what it must keep is the target floor on every chip.
     */
    @Test
    fun everyReportRangeChipReachesTheTouchTargetFloorOnBothAxes() {
        setReport(ReportProfileUiState())

        InsightRange.entries.forEach { range ->
            composeTestRule.onNodeWithTag("report_screen")
                .performScrollToNode(hasTestTag("report_range_${range.name}"))
            composeTestRule.onNodeWithTag("report_range_${range.name}")
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
        }
    }

    /**
     * The chips all share one height, so the row reads as one control rather than as six pills of
     * assorted sizes. Widths deliberately differ — the labels do.
     */
    @Test
    fun everyReportRangeChipSharesOneHeight() {
        setReport(ReportProfileUiState())

        val heights = InsightRange.entries.map { bounds("report_range_${it.name}").height }
        val reference = heights.first()
        heights.forEachIndexed { index, height ->
            assertEquals(
                "range chip $index is $height px against the first chip's $reference",
                reference.toDouble(),
                height.toDouble(),
                tolerancePx.toDouble()
            )
        }
    }
}
