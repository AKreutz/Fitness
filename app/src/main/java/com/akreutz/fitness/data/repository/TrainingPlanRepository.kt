package com.akreutz.fitness.data.repository

import androidx.room.withTransaction
import com.akreutz.fitness.data.db.FitnessDatabase
import com.akreutz.fitness.data.model.DraftWorkout
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.prefs.ActivePlanPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Mediates reads/writes of [TrainingPlan]s (and their nested [Workout]s) against Room, and
 * tracks which one the user is currently associated with.
 */
class TrainingPlanRepository(
    private val database: FitnessDatabase,
    private val activePlanPreferences: ActivePlanPreferences,
) {

    fun observeTrainingPlans(): Flow<List<TrainingPlanWithWorkouts>> =
        database.trainingPlanDao().observeAll()

    /** The plan the user is currently associated with, or `null` if none has been created yet. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeActiveTrainingPlan(): Flow<TrainingPlanWithWorkouts?> =
        activePlanPreferences.activeTrainingPlanId.flatMapLatest { id ->
            if (id == null) flowOf(null) else database.trainingPlanDao().observeById(id)
        }

    /**
     * Creates a new [TrainingPlan] with the given [name] and one [Workout] per entry in
     * [workouts] (in order), each with its own drafted exercises, as a single transaction, and
     * marks it as the user's active plan. Used to save the whole onboarding flow (plan name,
     * workouts, and any exercises added to them) at once, since nothing is persisted until the
     * user finishes onboarding.
     */
    suspend fun createTrainingPlan(name: String, workouts: List<DraftWorkout>): Long {
        val trainingPlanId = database.withTransaction {
            val id = database.trainingPlanDao().insert(TrainingPlan(name = name))
            workouts.forEachIndexed { workoutIndex, workout ->
                val workoutId = database.workoutDao().insert(
                    Workout(
                        trainingPlanId = id,
                        name = workout.name,
                        position = workoutIndex,
                    ),
                )
                workout.exercises.forEachIndexed { exerciseIndex, exercise ->
                    database.exerciseDao().insert(
                        Exercise(
                            workoutId = workoutId,
                            name = exercise.name,
                            type = exercise.type,
                            sets = exercise.sets,
                            reps = exercise.reps,
                            weightKg = exercise.weightKg,
                            weightIncrementKg = exercise.weightIncrementKg,
                            position = exerciseIndex,
                        ),
                    )
                }
            }
            id
        }
        activePlanPreferences.setActiveTrainingPlanId(trainingPlanId)
        return trainingPlanId
    }

    /**
     * Adds a new [Exercise] to the workout with [workoutId], placed after its existing
     * exercises.
     */
    suspend fun addExercise(
        workoutId: Long,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
    ) {
        database.withTransaction {
            val position = database.exerciseDao().countForWorkout(workoutId)
            database.exerciseDao().insert(
                Exercise(
                    workoutId = workoutId,
                    name = name,
                    type = type,
                    sets = sets,
                    reps = reps,
                    weightKg = weightKg,
                    weightIncrementKg = weightIncrementKg,
                    position = position,
                ),
            )
        }
    }
}
