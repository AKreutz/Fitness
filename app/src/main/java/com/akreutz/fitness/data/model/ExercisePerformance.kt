package com.akreutz.fitness.data.model

import java.time.Instant

/** The weight used and how hard it felt for one recorded performance of an [Exercise]. */
data class ExercisePerformanceEntry(
    val weightKg: Double,
    val perceivedEffort: PerceivedEffort,
)

/**
 * An [Exercise]'s performance history: the moment (the owning [WorkoutSession]'s
 * [WorkoutSession.completedAt]) of each time it was performed, mapped to how it went. Keyed by
 * the exact instant rather than just the date, so multiple sessions completed on the same day
 * each get their own entry instead of overwriting one another.
 */
typealias ExercisePerformanceHistory = Map<Instant, ExercisePerformanceEntry>
