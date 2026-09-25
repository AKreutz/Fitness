package com.akreutz.fitness.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A [WorkoutSession] together with the [Workout] it was for and that workout's [Exercise]s (each
 * with its performance history). Each exercise's own
 * [ExerciseWithPerformanceHistory.performanceHistory] can be read against
 * [WorkoutSession.completedAt] to show that session's per-exercise stats, since sessions don't
 * otherwise keep their own snapshot of them.
 */
data class WorkoutSessionWithWorkout(
    @Embedded
    val session: WorkoutSession,
    @Relation(
        parentColumn = "workoutId",
        entityColumn = "id",
    )
    val workout: Workout,
    @Relation(
        entity = Exercise::class,
        parentColumn = "workoutId",
        entityColumn = "workoutId",
    )
    val exercises: List<ExerciseWithPerformanceHistory>,
)
