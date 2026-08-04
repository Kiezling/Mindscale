package com.kieslingdev.mindscale.data

import androidx.room.TypeConverter

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class HourFormat { TWELVE, TWENTY_FOUR }

val DEFAULT_ONSET_CHIPS: List<String> = listOf(
    "flat", "agitated", "hopeless", "numb", "wired",
    "foggy", "alone", "driving", "work", "poor sleep"
)

class SettingsConverters {
    @TypeConverter
    fun fromThemeMode(value: ThemeMode): String = value.name

    @TypeConverter
    fun toThemeMode(raw: String): ThemeMode = ThemeMode.valueOf(raw)

    @TypeConverter
    fun fromHourFormat(value: HourFormat): String = value.name

    @TypeConverter
    fun toHourFormat(raw: String): HourFormat = HourFormat.valueOf(raw)
}
