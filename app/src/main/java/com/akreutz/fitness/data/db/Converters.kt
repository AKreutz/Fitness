package com.akreutz.fitness.data.db

import androidx.room.TypeConverter
import com.akreutz.fitness.data.model.ExercisePerformanceEntry
import com.akreutz.fitness.data.model.ExercisePerformanceHistory
import com.akreutz.fitness.data.model.PerceivedEffort
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Room type converters for column types it can't persist natively. */
class Converters {
    @TypeConverter
    fun fromIntList(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun toIntList(value: String): List<Int> =
        if (value.isEmpty()) emptyList() else value.split(",").map { it.toInt() }

    @TypeConverter
    fun fromPerformanceHistory(value: ExercisePerformanceHistory): String =
        value.entries.joinToString(";") { (completedAt, entry) ->
            "${completedAt.toEpochMilli()}:${entry.weightKg}:${entry.perceivedEffort}"
        }

    @TypeConverter
    fun toPerformanceHistory(value: String): ExercisePerformanceHistory =
        if (value.isEmpty()) {
            emptyMap()
        } else {
            value.split(";").associate { record ->
                val (key, weightKg, effort) = record.split(":")
                parsePerformanceKey(key) to ExercisePerformanceEntry(
                    weightKg = weightKg.toDouble(),
                    perceivedEffort = parsePerceivedEffort(effort),
                )
            }
        }

    /**
     * Parses a [performanceHistory][ExercisePerformanceHistory] entry's key, tolerating the
     * `LocalDate` (e.g. `2026-09-22`) it was previously stored as (keyed by day, before it was
     * changed to key by the exact moment performed) by treating it as midnight in the system
     * timezone, so history recorded before that change still loads.
     */
    private fun parsePerformanceKey(value: String): Instant =
        if (value.toLongOrNull() != null) {
            Instant.ofEpochMilli(value.toLong())
        } else {
            LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant()
        }

    /**
     * Parses a [PerceivedEffort] from its persisted name, tolerating the `LOW`/`HIGH` names it
     * was previously stored under (renamed to [PerceivedEffort.EASY]/[PerceivedEffort.HARD]) so
     * that history recorded before the rename still loads.
     */
    private fun parsePerceivedEffort(value: String): PerceivedEffort = when (value) {
        "LOW" -> PerceivedEffort.EASY
        "HIGH" -> PerceivedEffort.HARD
        else -> PerceivedEffort.valueOf(value)
    }

    @TypeConverter
    fun fromInstant(value: Instant): Long = value.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long): Instant = Instant.ofEpochMilli(value)
}
