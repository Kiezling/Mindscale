package com.kieslingdev.mindscale

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kieslingdev.mindscale.log.LogRoute
import com.kieslingdev.mindscale.log.LogViewModel
import com.kieslingdev.mindscale.track.TrackRoute
import com.kieslingdev.mindscale.track.TrackViewModel

enum class AppDestination { TRACK, LOG }

@Composable
fun MindScaleApp(trackViewModel: TrackViewModel, logViewModel: LogViewModel) {
    var destinationName by rememberSaveable { mutableStateOf(AppDestination.TRACK.name) }
    val destination = AppDestination.valueOf(destinationName)

    BackHandler(enabled = destination == AppDestination.LOG) {
        destinationName = AppDestination.TRACK.name
    }

    Scaffold(
        bottomBar = {
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
    ) { innerPadding ->
        when (destination) {
            AppDestination.TRACK -> TrackRoute(trackViewModel, Modifier.padding(innerPadding))
            AppDestination.LOG -> LogRoute(logViewModel, Modifier.padding(innerPadding))
        }
    }
}
