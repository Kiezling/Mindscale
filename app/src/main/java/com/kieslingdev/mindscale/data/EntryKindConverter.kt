package com.kieslingdev.mindscale.data

import androidx.room.TypeConverter

class EntryKindConverter {
    @TypeConverter
    fun fromKind(kind: EntryKind?): String? = kind?.name

    @TypeConverter
    fun toKind(raw: String?): EntryKind? = raw?.let { EntryKind.valueOf(it) }
}
