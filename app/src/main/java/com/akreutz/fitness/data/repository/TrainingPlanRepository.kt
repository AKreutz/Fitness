package com.akreutz.fitness.data.repository

import androidx.room.withTransaction
import com.akreutz.fitness.data.db.FitnessDatabase
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
     * [workoutNames] (in order, no exercises yet), as a single transaction, and marks it as the
     * user's active plan.
     */
    suspend fun createTrainingPlan(name: String, workoutNames: List<String>): Long {
        val trainingPlanId = database.withTransaction {
            val id = database.trainingPlanDao().insert(TrainingPlan(name = name))
            workoutNames.forEachIndexed { index, workoutName ->
                database.workoutDao().insert(
                    Workout(
                        trainingPlanId = id,
                        name = workoutName,
                        position = index,
                    ),
                )
            }
            id
        }
        activePlanPreferences.setActiveTrainingPlanId(trainingPlanId)
        return trainingPlanId
    }
}
