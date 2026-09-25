package com.akreutz.fitness.data.model

import androidx.room.Embedded
import androidx.room.Relation
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

/**
 * An [Exercise] together with its [ExercisePerformanceRecord]s, exposed as an
 * [ExercisePerformanceHistory]. Used everywhere an [Exercise] is read, since its performance
 * history lives in its own table rather than as a column on it (see [ExercisePerformanceRecord]).
 */
data class ExerciseWithPerformanceHistory(
    @Embedded
    val exercise: Exercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId",
    )
    val records: List<ExercisePerformanceRecord>,
) {
    val performanceHistory: ExercisePerformanceHistory
        get() = records.associate { it.completedAt to ExercisePerformanceEntry(it.weightKg, it.perceivedEffort) }
}

/**
 * The most recent entry in [ExerciseWithPerformanceHistory.performanceHistory], but only if it
 * was recorded at the exercise's current [Exercise.weightKg]. Once the weight changes (e.g. after
 * a weight-increase offer), the latest entry still reflects how the *old* weight felt, which no
 * longer applies to the new one, so callers that care about "how did this feel most recently"
 * should use this instead of reading [ExerciseWithPerformanceHistory.performanceHistory] directly.
 */
val ExerciseWithPerformanceHistory.lastPerformanceAtCurrentWeight: ExercisePerformanceEntry?
    get() = performanceHistory.maxByOrNull { it.key }
        ?.value
        ?.takeIf { it.weightKg == exercise.weightKg }

/**
 * This exercise's [ExercisePerformanceEntry] recorded for the [WorkoutSession] that completed at
 * [completedAt], or `null` if it wasn't performed (or rated) in that session. Used to show a past
 * session's per-exercise stats, since sessions don't keep their own snapshot of them.
 */
fun ExerciseWithPerformanceHistory.performanceOn(completedAt: Instant): ExercisePerformanceEntry? =
    performanceHistory[completedAt]
