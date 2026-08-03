package com.kieslingdev.mindscale

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.kieslingdev.mindscale.data.MindScaleDatabase
import com.kieslingdev.mindscale.track.TrackRoute
import com.kieslingdev.mindscale.track.TrackViewModel
import com.kieslingdev.mindscale.ui.theme.MindScaleTheme

class MainActivity : ComponentActivity() {

    // Manual singleton construction of the Room database (no DI framework, per
    // Invariant 11 / D-3). Held per-Activity-instance, which is sufficient for
    // this single-Activity app; the database itself is process-safe.
    private val database: MindScaleDatabase by lazy { MindScaleDatabase.build(applicationContext) }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MindScaleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TrackRoute(
                        viewModel = trackViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
