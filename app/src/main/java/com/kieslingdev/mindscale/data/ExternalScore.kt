package com.kieslingdev.mindscale.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class ExternalInstrument(val visibleLabel: String, val maxTotal: Int) {
    PHQ_8("PHQ-8", 24),
    GAD_7("GAD-7", 21)
}

enum class ExternalScoreProvenance {
    EXTERNALLY_OBTAINED_USER_ENTERED
}

class ExternalScoreConverters {
    @TypeConverter
    fun instrumentToString(value: ExternalInstrument): String = value.name

    @TypeConverter
    fun stringToInstrument(value: String): ExternalInstrument = ExternalInstrument.valueOf(value)

    @TypeConverter
    fun provenanceToString(value: ExternalScoreProvenance): String = value.name

    @TypeConverter
    fun stringToProvenance(value: String): ExternalScoreProvenance =
        ExternalScoreProvenance.valueOf(value)
}

@Entity(
    tableName = "external_scores",
    indices = [Index(value = ["instrument", "assessedEpochDay"], unique = true)]
)
data class ExternalScore(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val instrument: ExternalInstrument,
    val total: Int,
    val assessedEpochDay: Long,
    val provenance: ExternalScoreProvenance =
        ExternalScoreProvenance.EXTERNALLY_OBTAINED_USER_ENTERED,
    val enteredAt: Long
) {
    init {
        require(total in 0..instrument.maxTotal) {
            "${instrument.visibleLabel} total must be between 0 and ${instrument.maxTotal}"
        }
    }
}
