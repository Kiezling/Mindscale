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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.kieslingdev.mindscale.log.LogViewModel
import com.kieslingdev.mindscale.insights.InsightsViewModel
import com.kieslingdev.mindscale.settings.SettingsViewModel
import com.kieslingdev.mindscale.report.ReportProfileViewModel
import com.kieslingdev.mindscale.safety.SafetyViewModel
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
                    savedStateHandle = extras.createSavedStateHandle(),
                    episodeSourceDao = database.episodeSourceDao()
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
                    savedStateHandle = extras.createSavedStateHandle(),
                    settingsDao = database.trackSettingsDao()
                ) as T
            }
        }
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return SettingsViewModel(
                    settingsDao = database.trackSettingsDao(),
                    dataControlDao = database.dataControlDao(),
                    savedStateHandle = extras.createSavedStateHandle(),
                    onEraseCompleted = { reportProfileViewModel.clearAfterErase() },
                    // A restore replaces the Profile row and every external total, so the
                    // same sensitive draft/id state the erase path clears is stale here too.
                    onDataReplaced = { reportProfileViewModel.clearAfterErase() }
                ) as T
            }
        }
    }

    private val insightsViewModel: InsightsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return InsightsViewModel(
                    sourceDao = database.episodeSourceDao(),
                    settingsDao = database.trackSettingsDao(),
                    savedStateHandle = extras.createSavedStateHandle()
                ) as T
            }
        }
    }

    private val reportProfileViewModel: ReportProfileViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return ReportProfileViewModel(
                    profileDao = database.profileDao(),
                    dataControlDao = database.dataControlDao(),
                    insightsState = insightsViewModel.uiState,
                    savedStateHandle = extras.createSavedStateHandle()
                ) as T
            }
        }
    }

    private val safetyViewModel: SafetyViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                // Only the safety plan. No entry, sleep, marker, episode, or settings
                // source is wired in, so the card cannot become inferential (Phase 13, D-11).
                return SafetyViewModel(
                    planDao = database.safetyPlanDao(),
                    savedStateHandle = extras.createSavedStateHandle()
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            MindScaleTheme(themeMode = settingsState.settings.themeMode) {
                MindScaleApp(
                    trackViewModel = trackViewModel,
                    logViewModel = logViewModel,
                    insightsViewModel = insightsViewModel,
                    settingsViewModel = settingsViewModel,
                    reportProfileViewModel = reportProfileViewModel,
                    safetyViewModel = safetyViewModel,
                    eraseRevision = settingsState.eraseRevision
                )
            }
        }
    }
}
