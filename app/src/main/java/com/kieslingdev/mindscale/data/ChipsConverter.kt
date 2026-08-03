package com.kieslingdev.mindscale.data

import androidx.room.TypeConverter

class ChipsConverter {
    @TypeConverter
    fun fromChips(chips: List<String>): String = chips.joinToString(CHIP_DELIMITER)

    @TypeConverter
    fun toChips(raw: String): List<String> =
        if (raw.isEmpty()) emptyList() else raw.split(CHIP_DELIMITER)

    private companion object {
        // ASCII Unit Separator (0x1F); never user-typed, chosen to avoid a new
        // serialization dependency for the always-empty `chips` forward-compat field.
        const val CHIP_DELIMITER = ""
    }
}
