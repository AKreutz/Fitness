package com.akreutz.fitness.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A [Workout] together with its ordered [Exercise]s (each with its performance history), for
 * reading a full workout at once.
 */
data class WorkoutWithExercises(
    @Embedded
    val workout: Workout,
    @Relation(
        entity = Exercise::class,
        parentColumn = "id",
        entityColumn = "workoutId",
    )
    val exercises: List<ExerciseWithPerformanceHistory>,
)
