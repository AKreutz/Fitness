package com.akreutz.fitness.data.model

/**
 * An in-progress, not-yet-persisted [Exercise], as drafted on the onboarding add-exercises
 * screen.
 */
data class DraftExercise(
    val name: String,
    val type: ExerciseType,
    val sets: Int,
    val reps: List<Int>,
    val weightKg: Double,
    val weightIncrementKg: Double,
)

/**
 * An in-progress, not-yet-persisted [Workout] and the [DraftExercise]s added to it so far, held
 * entirely in memory during onboarding until the whole plan is saved at once.
 */
data class DraftWorkout(
    val name: String,
    val exercises: List<DraftExercise> = emptyList(),
)
