package com.akreutz.fitness.data.model

import java.time.LocalDate

/** The weight used and how hard it felt for one recorded performance of an [Exercise]. */
data class ExercisePerformanceEntry(
    val weightKg: Double,
    val perceivedEffort: PerceivedEffort,
)

/** An [Exercise]'s performance history: the date of each time it was performed, mapped to how it went. */
typealias ExercisePerformanceHistory = Map<LocalDate, ExercisePerformanceEntry>
