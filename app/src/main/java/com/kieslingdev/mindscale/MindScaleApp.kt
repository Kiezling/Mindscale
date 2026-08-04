package com.kieslingdev.mindscale

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
import com.kieslingdev.mindscale.track.TrackRoute
import com.kieslingdev.mindscale.track.TrackViewModel

enum class AppDestination { TRACK, LOG, INSIGHTS, PROFILE, REPORT, SETTINGS }

@Composable
fun MindScaleApp(
    trackViewModel: TrackViewModel,
    logViewModel: LogViewModel,
    insightsViewModel: InsightsViewModel,
    settingsViewModel: SettingsViewModel,
    reportProfileViewModel: ReportProfileViewModel,
    eraseRevision: Long = 0
) {
    var destinationStackState by rememberSaveable { mutableStateOf(AppDestination.TRACK.name) }
    var settingsFocusName by rememberSaveable { mutableStateOf(SettingsFocus.TOP.name) }
    var handledEraseRevision by rememberSaveable { mutableLongStateOf(0L) }
    val destinationStack = destinationStackState.split(',')
    val destination = AppDestination.valueOf(destinationStack.last())

    LaunchedEffect(eraseRevision) {
        if (eraseRevision > 0 && eraseRevision != handledEraseRevision) {
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

    Scaffold(
        topBar = {
            Surface(tonalElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (destination in setOf(AppDestination.PROFILE, AppDestination.REPORT, AppDestination.SETTINGS)) {
                        TextButton(onClick = ::navigateBack, modifier = Modifier.testTag("overlay_back")) {
                            Text("Back")
                        }
                    } else {
                        Text("MindScale", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        when (destination) {
                            AppDestination.TRACK -> "Track"
                            AppDestination.LOG -> "Full Log"
                            AppDestination.INSIGHTS -> "Insights"
                            AppDestination.PROFILE -> "Profile"
                            AppDestination.REPORT -> "Clinician summary"
                            AppDestination.SETTINGS -> "Settings"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (destination in setOf(AppDestination.TRACK, AppDestination.LOG, AppDestination.INSIGHTS)) {
                        TextButton(
                            onClick = { openOverlay(AppDestination.PROFILE) },
                            modifier = Modifier.testTag("profile_action")
                                .semantics { contentDescription = "Open Profile" }
                        ) { Text("Profile") }
                    } else {
                        Text("")
                    }
                }
            }
        },
        bottomBar = {
            if (destination in setOf(AppDestination.TRACK, AppDestination.LOG, AppDestination.INSIGHTS)) {
                NavigationBar(modifier = Modifier.testTag("main_navigation")) {
                    NavigationBarItem(
                        selected = destination == AppDestination.TRACK,
                        onClick = { setRoot(AppDestination.TRACK) },
                        icon = { Text("●") },
                        label = { Text("Track") },
                        modifier = Modifier.semantics { contentDescription = "Track tab" }
                    )
                    NavigationBarItem(
                        selected = destination == AppDestination.LOG,
                        onClick = { setRoot(AppDestination.LOG) },
                        icon = { Text("≡") },
                        label = { Text("Log") },
                        modifier = Modifier.semantics { contentDescription = "Log tab" }
                    )
                    NavigationBarItem(
                        selected = destination == AppDestination.INSIGHTS,
                        onClick = { setRoot(AppDestination.INSIGHTS) },
                        icon = { Text("▦") },
                        label = { Text("Insights") },
                        modifier = Modifier
                            .testTag("insights_tab")
                            .semantics { contentDescription = "Insights tab" }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (destination) {
            AppDestination.TRACK -> TrackRoute(
                trackViewModel,
                onOpenSettings = ::openSettings,
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
        }
    }
}
