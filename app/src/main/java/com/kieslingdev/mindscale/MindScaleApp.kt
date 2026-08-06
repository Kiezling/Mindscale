package com.kieslingdev.mindscale

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.kieslingdev.mindscale.ui.components.MsCircularHeaderButton
import com.kieslingdev.mindscale.ui.components.MsEyebrow
import com.kieslingdev.mindscale.ui.components.MsHairline
import com.kieslingdev.mindscale.ui.components.MsHeaderRule
import com.kieslingdev.mindscale.ui.components.MsTextAction
import com.kieslingdev.mindscale.ui.components.MsUppercaseText
import com.kieslingdev.mindscale.ui.components.MsWordmark
import com.kieslingdev.mindscale.ui.theme.MsSpacing
import com.kieslingdev.mindscale.ui.theme.ms
import com.kieslingdev.mindscale.log.LogRoute
import com.kieslingdev.mindscale.log.LogViewModel
import com.kieslingdev.mindscale.insights.InsightsRoute
import com.kieslingdev.mindscale.insights.InsightsViewModel
import com.kieslingdev.mindscale.settings.SettingsFocus
import com.kieslingdev.mindscale.settings.SettingsRoute
import com.kieslingdev.mindscale.settings.SettingsViewModel
import com.kieslingdev.mindscale.report.ProfileRoute
import com.kieslingdev.mindscale.report.ReportProfileViewModel
import com.kieslingdev.mindscale.report.ReportRoute
import com.kieslingdev.mindscale.breathing.BreathingCopy
import com.kieslingdev.mindscale.breathing.BreathingRoute
import com.kieslingdev.mindscale.breathing.BreathingViewModel
import com.kieslingdev.mindscale.safety.SafetyCopy
import com.kieslingdev.mindscale.safety.SafetyRoute
import com.kieslingdev.mindscale.safety.SafetyViewModel
import com.kieslingdev.mindscale.track.TrackRoute
import com.kieslingdev.mindscale.track.TrackViewModel

enum class AppDestination { TRACK, LOG, INSIGHTS, PROFILE, REPORT, SETTINGS, SAFETY, BREATHING }

@Composable
fun MindScaleApp(
    trackViewModel: TrackViewModel,
    logViewModel: LogViewModel,
    insightsViewModel: InsightsViewModel,
    settingsViewModel: SettingsViewModel,
    reportProfileViewModel: ReportProfileViewModel,
    safetyViewModel: SafetyViewModel,
    breathingViewModel: BreathingViewModel,
    eraseRevision: Long = 0
) {
    var destinationStackState by rememberSaveable { mutableStateOf(AppDestination.TRACK.name) }
    var settingsFocusName by rememberSaveable { mutableStateOf(SettingsFocus.TOP.name) }
    var handledEraseRevision by rememberSaveable { mutableLongStateOf(0L) }
    val destinationStack = destinationStackState.split(',')
    val destination = AppDestination.valueOf(destinationStack.last())

    LaunchedEffect(eraseRevision) {
        if (eraseRevision > 0 && eraseRevision != handledEraseRevision) {
            // An erase or restore navigates back to Track from wherever the user was. A
            // session running at that moment is dropped without being written: the user
            // just asked for everything to be deleted, so completing a pending write would
            // put a row back into a table the transaction has already cleared.
            breathingViewModel.discardSession()
            destinationStackState = AppDestination.TRACK.name
            settingsFocusName = SettingsFocus.TOP.name
            handledEraseRevision = eraseRevision
        }
    }

    fun setRoot(root: AppDestination) {
        destinationStackState = root.name
    }

    fun openOverlay(overlay: AppDestination) {
        if (destination == overlay) return
        destinationStackState = (destinationStack + overlay.name).joinToString(",")
    }

    fun openSettings(focus: SettingsFocus) {
        settingsFocusName = focus.name
        openOverlay(AppDestination.SETTINGS)
    }

    fun navigateBack() {
        // Leaving the pacer ends it. This is wired to navigation rather than to
        // `onDispose` or `ON_STOP`, both of which also fire on a rotation and would end a
        // session because the user turned the phone (`SPEC-paced-breathing.md`, D-6).
        if (destination == AppDestination.BREATHING) breathingViewModel.leaveScreen()
        if (destinationStack.size > 1) {
            destinationStackState = destinationStack.dropLast(1).joinToString(",")
        } else {
            when (AppDestination.valueOf(destinationStack.last())) {
                AppDestination.LOG, AppDestination.INSIGHTS -> setRoot(AppDestination.TRACK)
                else -> Unit
            }
        }
    }

    BackHandler(enabled = destination != AppDestination.TRACK) { navigateBack() }

    val isRoot = destination in setOf(
        AppDestination.TRACK,
        AppDestination.LOG,
        AppDestination.INSIGHTS
    )

    Scaffold(
        containerColor = MaterialTheme.ms.bg,
        topBar = {
            // The pacer renders full-bleed: no top bar and no bottom navigation
            // (`docs/specs/SPEC-visual-foundation.md`, D-18). This is safe only because the exit
            // affordance does not live in the chrome — the screen has its own `breathing_close`
            // pill, and system Back still works through the BackHandler above.
            if (destination != AppDestination.BREATHING) {
                MindScaleHeader(
                    isRoot = isRoot,
                    title = when (destination) {
                        AppDestination.TRACK -> "Track"
                        AppDestination.LOG -> "Full Log"
                        AppDestination.INSIGHTS -> "Insights"
                        AppDestination.PROFILE -> "Profile"
                        AppDestination.REPORT -> "Clinician summary"
                        AppDestination.SETTINGS -> "Settings"
                        AppDestination.SAFETY -> SafetyCopy.TOP_BAR_TITLE
                        AppDestination.BREATHING -> BreathingCopy.TOP_BAR_TITLE
                    },
                    onBack = ::navigateBack,
                    onOpenProfile = { openOverlay(AppDestination.PROFILE) }
                )
            }
        },
        bottomBar = {
            if (isRoot) {
                MindScaleBottomNavigation(
                    destination = destination,
                    onSelect = ::setRoot
                )
            }
        }
    ) { innerPadding ->
        when (destination) {
            AppDestination.TRACK -> TrackRoute(
                trackViewModel,
                onOpenSettings = ::openSettings,
                onOpenSafety = { openOverlay(AppDestination.SAFETY) },
                onOpenBreathing = { openOverlay(AppDestination.BREATHING) },
                modifier = Modifier.padding(innerPadding)
            )
            AppDestination.LOG -> LogRoute(logViewModel, Modifier.padding(innerPadding))
            AppDestination.INSIGHTS -> InsightsRoute(
                insightsViewModel,
                Modifier.padding(innerPadding),
                onOpenReport = { openOverlay(AppDestination.REPORT) }
            )
            AppDestination.PROFILE -> ProfileRoute(
                viewModel = reportProfileViewModel,
                onOpenReport = { openOverlay(AppDestination.REPORT) },
                onOpenSettings = { openSettings(SettingsFocus.TOP) },
                onOpenSafety = { openOverlay(AppDestination.SAFETY) },
                modifier = Modifier.padding(innerPadding)
            )
            AppDestination.REPORT -> ReportRoute(
                viewModel = reportProfileViewModel,
                onRangeSelected = insightsViewModel::selectRange,
                modifier = Modifier.padding(innerPadding)
            )
            AppDestination.SETTINGS -> SettingsRoute(
                settingsViewModel,
                focus = SettingsFocus.valueOf(settingsFocusName),
                modifier = Modifier.padding(innerPadding)
            )
            AppDestination.SAFETY -> SafetyRoute(
                safetyViewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppDestination.BREATHING -> BreathingRoute(
                breathingViewModel,
                modifier = Modifier
                    .padding(innerPadding)
                    // The pacer has no top bar, so `BreathingCopy.TOP_BAR_TITLE` would otherwise
                    // stop being rendered anywhere. Rather than leave a frozen string dead, it
                    // becomes the accessibility pane title and is still announced on entry
                    // (`docs/specs/SPEC-visual-foundation.md`, D-18).
                    .semantics { paneTitle = BreathingCopy.TOP_BAR_TITLE }
            )
        }
    }
}

/**
 * The design's header (`docs/specs/SPEC-visual-foundation.md`, D-16): flat on the page with no
 * divider and no tonal elevation, a centred title in the wordmark treatment — uppercase, weight
 * 500, tracked to 0.542 em — with the 22 x 1 dp gold rule beneath it.
 *
 * All three of the app's existing header slots survive, because removing any of them would be a
 * removal of on-screen content and D-1 forbids that. The prototype's header carries a single
 * centred wordmark because that app has an initials avatar and no destination title; MindScale
 * has a destination title and a Profile text action, so it keeps a three-cell bar. The cells are
 * weighted 1/2/1 rather than absolutely positioned so the title stays optically centred and the
 * bar reflows instead of overlapping at 200% font.
 *
 * The prototype's initials avatar is not adopted: it is a different Profile entry point, and
 * changing entry points is a navigation change (D-1).
 */
@Composable
private fun MindScaleHeader(
    isRoot: Boolean,
    title: String,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.ms.bg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = MsSpacing.mdPlus, vertical = MsSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (isRoot) {
                MsEyebrow("MindScale")
            } else {
                MsCircularHeaderButton(
                    label = "‹",
                    onClick = onBack,
                    modifier = Modifier
                        .testTag("overlay_back")
                        .semantics { contentDescription = "Back" }
                )
            }
        }
        Column(
            modifier = Modifier.weight(2f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MsSpacing.sm)
        ) {
            MsWordmark(title)
            MsHeaderRule()
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (isRoot) {
                MsTextAction(
                    text = "Profile",
                    onClick = onOpenProfile,
                    modifier = Modifier
                        .testTag("profile_action")
                        .semantics { contentDescription = "Open Profile" }
                )
            }
        }
    }
}

/**
 * The design's navigation (D-17): three flush text tabs on the page above a hairline top border,
 * the selected one in gold at weight 600 and the rest at the faintest compliant emphasis level.
 * There are no icons.
 *
 * `NavigationBar` cannot render an item without an icon slot, so this is a plain [Row] of
 * selectable cells. What had to survive the swap, and is asserted by `MindScaleChromeTest`: the
 * `main_navigation` and `insights_tab` tags, the three `… tab` content descriptions, clickable
 * nodes findable by the original-case strings `Track`, `Log`, and `Insights` — `NavigationTest`
 * does `onNodeWithText("Log").performClick()` — the selected state, the `setRoot` callbacks, and
 * a 48 dp target.
 *
 * The three glyphs the old bar carried (`●`, `≡`, `▦`) are dropped. That is a removal of
 * on-screen marks, so it is stated rather than smuggled: they are unlabelled decorations that no
 * test asserts and no content description names, and every tab keeps both its text label and its
 * content description, so nothing that was announced stops being announced.
 *
 * Selection is never colour alone: gold *and* weight 600 (D-23).
 */
@Composable
private fun MindScaleBottomNavigation(
    destination: AppDestination,
    onSelect: (AppDestination) -> Unit
) {
    val palette = MaterialTheme.ms
    Column(modifier = Modifier.testTag("main_navigation")) {
        MsHairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.bg)
                .navigationBarsPadding()
        ) {
            NavigationTab(
                label = "Track",
                contentDescription = "Track tab",
                selected = destination == AppDestination.TRACK,
                onClick = { onSelect(AppDestination.TRACK) },
                modifier = Modifier.weight(1f)
            )
            NavigationTab(
                label = "Log",
                contentDescription = "Log tab",
                selected = destination == AppDestination.LOG,
                onClick = { onSelect(AppDestination.LOG) },
                modifier = Modifier.weight(1f)
            )
            NavigationTab(
                label = "Insights",
                contentDescription = "Insights tab",
                selected = destination == AppDestination.INSIGHTS,
                onClick = { onSelect(AppDestination.INSIGHTS) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("insights_tab")
            )
        }
    }
}

@Composable
private fun NavigationTab(
    label: String,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.ms
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = MsSpacing.minTouchTarget)
            .selectable(selected = selected, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = MsSpacing.xxs, vertical = MsSpacing.lgPlus),
        contentAlignment = Alignment.Center
    ) {
        // Deliberately unbounded lines. At 200% font `INSIGHTS` is wider than a third of the
        // screen, and a single-line label clipped its last glyph at the edge. Wrapping is not
        // pretty at that scale, but D-23 requires reflow without clipping and a label that has
        // lost a letter is worse than one that has taken two lines.
        MsUppercaseText(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (selected) palette.goldText else palette.inkQuaternary,
            textAlign = TextAlign.Center
        )
    }
}
