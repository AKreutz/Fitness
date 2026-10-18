package com.akreutz.fitness.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A [TrainingPlan] together with its ordered [Workout]s, each with its own [Exercise]s, for
 * reading a full plan at once.
 */
data class TrainingPlanWithWorkouts(
    @Embedded
    val trainingPlan: TrainingPlan,
    @Relation(
        entity = Workout::class,
        parentColumn = "id",
        entityColumn = "trainingPlanId",
    )
    val workouts: List<WorkoutWithExercises>,
)

/** Sorts the workouts by [Workout.position] and each workout's exercises by theirs. */
fun TrainingPlanWithWorkouts.sortedByPosition(): TrainingPlanWithWorkouts =
    copy(workouts = workouts.sortedBy { it.workout.position }.map { it.sortedByPosition() })
