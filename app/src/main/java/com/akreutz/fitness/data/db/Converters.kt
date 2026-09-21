package com.akreutz.fitness.data.db

import androidx.room.TypeConverter
import com.akreutz.fitness.data.model.ExercisePerformanceEntry
import com.akreutz.fitness.data.model.ExercisePerformanceHistory
import com.akreutz.fitness.data.model.PerceivedEffort
import java.time.LocalDate

/** Room type converters for column types it can't persist natively. */
class Converters {
    @TypeConverter
    fun fromIntList(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun toIntList(value: String): List<Int> =
        if (value.isEmpty()) emptyList() else value.split(",").map { it.toInt() }

    @TypeConverter
    fun fromPerformanceHistory(value: ExercisePerformanceHistory): String =
        value.entries.joinToString(";") { (date, entry) ->
            "$date:${entry.weightKg}:${entry.perceivedEffort}"
        }

    @TypeConverter
    fun toPerformanceHistory(value: String): ExercisePerformanceHistory =
        if (value.isEmpty()) {
            emptyMap()
        } else {
            value.split(";").associate { record ->
                val (date, weightKg, effort) = record.split(":")
                LocalDate.parse(date) to ExercisePerformanceEntry(
                    weightKg = weightKg.toDouble(),
                    perceivedEffort = PerceivedEffort.valueOf(effort),
                )
            }
        }
}
