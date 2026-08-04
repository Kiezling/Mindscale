package com.kieslingdev.mindscale.track

import com.kieslingdev.mindscale.data.TrackSettings
import com.kieslingdev.mindscale.data.TrackSettingsDao
import com.kieslingdev.mindscale.data.ThemeMode
import com.kieslingdev.mindscale.data.HourFormat
import com.kieslingdev.mindscale.data.HoldDuration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeTrackSettingsDao(initial: TrackSettings = TrackSettings()) : TrackSettingsDao {

    private val settingsFlow = MutableStateFlow(initial)
    val updateCalls = mutableListOf<TrackSettings>()

    override fun observe(): Flow<TrackSettings> = settingsFlow.asStateFlow()

    override suspend fun current(): TrackSettings = settingsFlow.value

    override suspend fun setSleepOn(enabled: Boolean): Int = commit { it.copy(sleepOn = enabled) }

    override suspend fun hasOpenSleepInterval(): Boolean = false

    override suspend fun setAskChips(enabled: Boolean): Int = commit { it.copy(askChips = enabled) }

    override suspend fun setPaused(paused: Boolean): Int = commit { it.copy(paused = paused) }

    override suspend fun recordCheckin(checkinAt: Long, paused: Boolean): Int =
        commit { it.copy(checkinAt = checkinAt, paused = paused) }

    override suspend fun setSleepIntroShown(shown: Boolean): Int = commit { it.copy(sleepIntroShown = shown) }

    override suspend fun setAppearance(mode: ThemeMode): Int = commit { it.copy(themeMode = mode) }

    override suspend fun setHourFormat(format: HourFormat): Int = commit { it.copy(hourFormat = format) }

    override suspend fun setAnchors(anchor2: String, anchor5: String, anchor8: String): Int =
        commit { it.copy(anchor2 = anchor2, anchor5 = anchor5, anchor8 = anchor8) }

    override suspend fun setOnsetChips(chips: List<String>): Int = commit { it.copy(onsetChips = chips) }

    override suspend fun setHideNotes(hidden: Boolean): Int = commit { it.copy(hideNotes = hidden) }

    override suspend fun setAnchorPromptDone(done: Boolean): Int = commit { it.copy(anchorPromptDone = done) }

    override suspend fun setHoldDuration(duration: HoldDuration): Int = commit { it.copy(holdDuration = duration) }

    override suspend fun update(settings: TrackSettings) {
        updateCalls += settings
        settingsFlow.value = settings
    }

    private fun commit(transform: (TrackSettings) -> TrackSettings): Int {
        val updated = transform(settingsFlow.value)
        updateCalls += updated
        settingsFlow.value = updated
        return 1
    }
}
