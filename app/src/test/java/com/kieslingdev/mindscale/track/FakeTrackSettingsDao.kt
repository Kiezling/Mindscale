package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.TrackSettingsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeTrackSettingsDao(initial: TrackSettings = TrackSettings()) : TrackSettingsDao {

    private val settingsFlow = MutableStateFlow(initial)
    val updateCalls = mutableListOf<TrackSettings>()

    override fun observe(): Flow<TrackSettings> = settingsFlow.asStateFlow()

    override suspend fun update(settings: TrackSettings) {
        updateCalls += settings
        settingsFlow.value = settings
    }
}
