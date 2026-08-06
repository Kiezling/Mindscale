package com.kieslingdev.mindscale.safety

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kieslingdev.mindscale.data.MindScaleDatabase
import com.kieslingdev.mindscale.data.SafetyPlanStep
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The geometry `docs/specs/SPEC-remaining-screens-visual.md` freezes for Safety, asserted rather
 * than eyeballed.
 *
 * Safety has **no reference screenshot in either theme**, which D-2 states rather than papers over.
 * It is also the screen where the consequences of getting layout wrong are not cosmetic, so what is
 * asserted here is not only the target floor but the one ordering constraint that matters: reaching
 * help must not require reading a paragraph first (D-11, and `SPEC-safety-card.md` D-3).
 *
 * These replace nothing. `SafetyScreenTest` and `SafetyIntentTest` still own this screen's
 * behavior, are untouched by this phase, and must keep passing.
 */
@RunWith(AndroidJUnit4::class)
class SafetyVisualTest {

    @get:Rule val composeTestRule = createComposeRule()

    private lateinit var database: MindScaleDatabase
    private lateinit var viewModel: SafetyViewModel

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MindScaleDatabase::class.java
        ).addCallback(MindScaleDatabase.seedSettingsCallback).build()
        viewModel = SafetyViewModel(database.safetyPlanDao(), SavedStateHandle())
    }

    @After fun tearDown() = database.close()

    private fun render(fontScale: Float = 1f) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale)
            ) {
                MindScaleTheme { SafetyRoute(viewModel) }
            }
        }
    }

    private fun scrollTo(tag: String) {
        composeTestRule.onNodeWithTag("safety_screen").performScrollToNode(
            SemanticsMatcher.expectValue(SemanticsProperties.TestTag, tag)
        )
    }

    private fun bounds(tag: String): Rect =
        composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    /**
     * The card order the design would happily have rearranged: name, then the actions, then the
     * paragraph of detail.
     *
     * `SPEC-safety-card.md` D-3 froze this because reaching help must not require reading a
     * paragraph first, visually or in TalkBack order — and a restyle that moved the detail above
     * the actions to make the card look calmer would be a real harm rather than a taste question.
     * Nothing asserted the *visual* order before this phase; the semantics order was covered, the
     * geometry was not.
     */
    @Test
    fun everyCrisisActionSitsAboveItsCardsDetailParagraph() {
        render()

        val card = bounds("resource_lifeline")
        val call = bounds("resource_action_${SafetyCopy.LIFELINE_CALL}")
        val text = bounds("resource_action_${SafetyCopy.LIFELINE_TEXT}")

        assertTrue(
            "the call action should sit inside its own card; card ${card.top}..${card.bottom}, " +
                "action ${call.top}..${call.bottom}",
            call.top >= card.top && call.bottom <= card.bottom
        )
        assertTrue(
            "the two actions should stack in their frozen order, call above text; " +
                "call at ${call.top}, text at ${text.top}",
            call.top < text.top
        )
        assertTrue(
            "the actions must sit in the card's upper half, above its detail paragraph; " +
                "the text action ends at ${text.bottom} in a card ending at ${card.bottom}",
            text.bottom < card.bottom
        )
    }

    /** Every crisis action is a full-width target, at both scales. */
    @Test
    fun everyCrisisActionIsAFullWidthTargetAtTheFloor() {
        render()

        val card = bounds("resource_lifeline")
        listOf(SafetyCopy.LIFELINE_CALL, SafetyCopy.LIFELINE_TEXT).forEach { label ->
            composeTestRule.onNodeWithTag("resource_action_$label").assertHeightIsAtLeast(48.dp)
            val action = bounds("resource_action_$label")
            assertTrue(
                "a crisis action should span its card rather than hugging its label; " +
                    "action is ${action.width} px in a ${card.width} px card",
                action.width > card.width * 0.7f
            )
        }
    }

    @Test
    fun everyCrisisActionStillReachesTheFloorAt200PercentFont() {
        render(fontScale = 2f)

        listOf(
            SafetyCopy.LIFELINE_CALL,
            SafetyCopy.LIFELINE_TEXT,
            SafetyCopy.ELSEWHERE_ACTION
        ).forEach { label ->
            scrollTo("resource_action_$label")
            composeTestRule.onNodeWithTag("resource_action_$label").assertHeightIsAtLeast(48.dp)
        }
    }

    /**
     * The narrow bare actions — Add, Edit, Delete, and the call button — are the ones a restyle onto
     * the design's zero-padding text idiom is most likely to drop under the floor. `SafetyScreenTest`
     * covers them at 100% font; this covers them at 200%, where a wrapped label is the normal case.
     */
    @Test
    fun everyNarrowPlanControlReachesTheFloorAt200PercentFont() {
        runBlocking {
            database.safetyPlanDao().addItem(SafetyPlanStep.PEOPLE_FOR_HELP, "Sam", "555-0100")
        }
        render(fontScale = 2f)
        composeTestRule.waitForIdle()
        val id = runBlocking {
            database.safetyPlanDao().itemsIn(SafetyPlanStep.PEOPLE_FOR_HELP).first().id
        }

        listOf(
            "plan_call_$id",
            "plan_edit_$id",
            "plan_delete_$id",
            "step_add_${SafetyPlanStep.PEOPLE_FOR_HELP.name}"
        ).forEach { tag ->
            scrollTo(tag)
            composeTestRule.onNodeWithTag(tag)
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
        }
    }

    /**
     * The step headings still render in canonical Stanley-Brown order **down the page**, not merely
     * in the order the semantics tree happens to report. `SafetyScreenTest` asserts each heading
     * exists and carries the heading role; nothing asserted their geometry, and a card-based
     * restyle is exactly the kind of change that could regroup them.
     *
     * Measured from one viewport rather than by scrolling to each in turn, because a `LazyColumn`
     * recycles what leaves the screen and comparing positions across scrolls would compare
     * viewport offsets rather than list order. The first heading is scrolled to once, and every
     * step heading composed at that moment must appear in `SAFETY_STEPS` order.
     */
    @Test
    fun theStepHeadingsRenderInCanonicalOrderDownThePage() {
        render()
        scrollTo("step_heading_${SAFETY_STEPS.first().step.name}")

        val composed = SAFETY_STEPS.mapNotNull { step ->
            val tag = "step_heading_${step.step.name}"
            val nodes = composeTestRule
                .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.TestTag, tag))
                .fetchSemanticsNodes()
            nodes.firstOrNull()?.let { step.step.name to it.boundsInRoot.top }
        }

        assertTrue(
            "at least two step headings should be composed at once to compare their order",
            composed.size >= 2
        )
        composed.zipWithNext().forEach { (earlier, later) ->
            assertTrue(
                "${earlier.first} should render above ${later.first}; measured " +
                    "${earlier.second} px and ${later.second} px",
                earlier.second < later.second
            )
        }
    }

    /** The plan section as a whole still follows the crisis resources down the page. */
    @Test
    fun thePlanHeadingFollowsTheResourcesHeading() {
        render()

        val resources = bounds("resources_heading").top
        scrollTo("plan_heading")
        assertTrue(
            "the resources heading should have been at the top of the page; measured $resources px",
            resources >= 0f
        )
        // After scrolling, the plan heading is on screen and the resources heading is not, which is
        // itself the ordering claim: the plan section is below the resources section.
        val stillComposed = composeTestRule
            .onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.TestTag, "resources_heading"))
            .fetchSemanticsNodes()
        stillComposed.firstOrNull()?.let {
            assertTrue(
                "the resources heading must stay above the plan heading",
                it.boundsInRoot.top < bounds("plan_heading").top
            )
        }
    }
}
