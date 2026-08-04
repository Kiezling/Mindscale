package com.kieslingdev.mindscale

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.kieslingdev.mindscale.log.LogViewModel
import com.kieslingdev.mindscale.track.TrackViewModel
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme

class MainActivity : ComponentActivity() {

    private val database by lazy { (application as MindScaleApplication).container.database }

    private val trackViewModel: TrackViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return TrackViewModel(
                    entryDao = database.entryDao(),
                    sleepDao = database.sleepDao(),
                    markerDao = database.markerDao(),
                    settingsDao = database.trackSettingsDao(),
                    savedStateHandle = extras.createSavedStateHandle()
                ) as T
            }
        }
    }

    private val logViewModel: LogViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return LogViewModel(
                    entryDao = database.entryDao(),
                    sleepDao = database.sleepDao(),
                    markerDao = database.markerDao(),
                    savedStateHandle = extras.createSavedStateHandle()
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MindScaleTheme {
                MindScaleApp(trackViewModel = trackViewModel, logViewModel = logViewModel)
            }
        }
    }
}
