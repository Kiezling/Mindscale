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
import androidx.compose.runtime.getValue
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
import com.kieslingdev.mindscale.settings.SettingsFocus
import com.kieslingdev.mindscale.settings.SettingsRoute
import com.kieslingdev.mindscale.settings.SettingsViewModel
import com.kieslingdev.mindscale.track.TrackRoute
import com.kieslingdev.mindscale.track.TrackViewModel

enum class AppDestination { TRACK, LOG, SETTINGS }

@Composable
fun MindScaleApp(
    trackViewModel: TrackViewModel,
    logViewModel: LogViewModel,
    settingsViewModel: SettingsViewModel
) {
    var destinationName by rememberSaveable { mutableStateOf(AppDestination.TRACK.name) }
    var priorDestinationName by rememberSaveable { mutableStateOf(AppDestination.TRACK.name) }
    var settingsFocusName by rememberSaveable { mutableStateOf(SettingsFocus.TOP.name) }
    val destination = AppDestination.valueOf(destinationName)

    fun openSettings(focus: SettingsFocus) {
        priorDestinationName = if (destination == AppDestination.SETTINGS) {
            priorDestinationName
        } else destination.name
        settingsFocusName = focus.name
        destinationName = AppDestination.SETTINGS.name
    }

    fun navigateBack() {
        destinationName = when (destination) {
            AppDestination.SETTINGS -> priorDestinationName
            AppDestination.LOG -> AppDestination.TRACK.name
            AppDestination.TRACK -> AppDestination.TRACK.name
        }
    }

    BackHandler(enabled = destination != AppDestination.TRACK, onBack = ::navigateBack)

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
                    if (destination == AppDestination.SETTINGS) {
                        TextButton(onClick = ::navigateBack, modifier = Modifier.testTag("settings_back")) {
                            Text("Back")
                        }
                    } else {
                        Text("MindScale", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        when (destination) {
                            AppDestination.TRACK -> "Track"
                            AppDestination.LOG -> "Full Log"
                            AppDestination.SETTINGS -> "Settings"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (destination != AppDestination.SETTINGS) {
                        TextButton(
                            onClick = { openSettings(SettingsFocus.TOP) },
                            modifier = Modifier.testTag("settings_action")
                                .semantics { contentDescription = "Open Settings" }
                        ) { Text("Settings") }
                    } else {
                        Text("")
                    }
                }
            }
        },
        bottomBar = {
            if (destination != AppDestination.SETTINGS) {
                NavigationBar(modifier = Modifier.testTag("main_navigation")) {
                    NavigationBarItem(
                        selected = destination == AppDestination.TRACK,
                        onClick = { destinationName = AppDestination.TRACK.name },
                        icon = { Text("●") },
                        label = { Text("Track") },
                        modifier = Modifier.semantics { contentDescription = "Track tab" }
                    )
                    NavigationBarItem(
                        selected = destination == AppDestination.LOG,
                        onClick = { destinationName = AppDestination.LOG.name },
                        icon = { Text("≡") },
                        label = { Text("Log") },
                        modifier = Modifier.semantics { contentDescription = "Log tab" }
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
            AppDestination.SETTINGS -> SettingsRoute(
                settingsViewModel,
                focus = SettingsFocus.valueOf(settingsFocusName),
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
