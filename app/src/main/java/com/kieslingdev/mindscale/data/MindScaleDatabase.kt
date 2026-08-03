package com.kieslingdev.mindscale.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Entry::class], version = 1, exportSchema = true)
@TypeConverters(ChipsConverter::class)
abstract class MindScaleDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
