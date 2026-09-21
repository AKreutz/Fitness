package com.akreutz.fitness.data.repository

import androidx.room.withTransaction
import com.akreutz.fitness.data.db.FitnessDatabase
import com.akreutz.fitness.data.model.DraftWorkout
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExercisePerformanceEntry
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.model.WorkoutWithExercises
import com.akreutz.fitness.data.prefs.ActivePlanPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate

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

    /** The workout with [workoutId] together with its exercises, or `null` if it doesn't exist. */
    fun observeWorkout(workoutId: Long): Flow<WorkoutWithExercises?> =
        database.workoutDao().observeById(workoutId)

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
     * The [Workout] to offer starting next for [trainingPlan]: the one after whichever was
     * finished last (by plan order), wrapping back to the first; or simply the first workout if
     * none has been finished yet, or the plan has no workouts. Starting a workout without
     * finishing it (e.g. cancelling) doesn't advance this, so it keeps being offered.
     */
    suspend fun nextWorkout(trainingPlan: TrainingPlanWithWorkouts): Workout? {
        val workouts = trainingPlan.workouts.map { it.workout }
        val lastFinishedId = activePlanPreferences.lastFinishedWorkoutId.first()
        return workouts.getOrNull(nextWorkoutIndex(workouts, lastFinishedId))
    }

    /**
     * Like [nextWorkout], but reactive: re-emits the plan's workouts (each still with its
     * exercises) reordered so the one up next comes first, keeping the rest in their predefined
     * order, whenever [trainingPlan] or the last-finished workout changes.
     */
    fun observeWorkoutsNextFirst(
        trainingPlan: Flow<TrainingPlanWithWorkouts>,
    ): Flow<List<WorkoutWithExercises>> =
        combine(trainingPlan, activePlanPreferences.lastFinishedWorkoutId) { plan, lastFinishedId ->
            val nextIndex = nextWorkoutIndex(plan.workouts.map { it.workout }, lastFinishedId)
            if (nextIndex <= 0) plan.workouts else plan.workouts.rotated(nextIndex)
        }

    /**
     * The index within [workouts] (in plan order) of the workout up next: the one after
     * [lastFinishedId], wrapping back to the first; or `0` if [lastFinishedId] wasn't finished or
     * isn't found, or [workouts] is empty (an out-of-range index, consistent with
     * [List.getOrNull]-style lookups).
     */
    private fun nextWorkoutIndex(workouts: List<Workout>, lastFinishedId: Long?): Int {
        if (workouts.isEmpty()) return 0
        val lastFinishedIndex = workouts.indexOfFirst { it.id == lastFinishedId }
        return if (lastFinishedIndex == -1) 0 else (lastFinishedIndex + 1) % workouts.size
    }

    private fun <T> List<T>.rotated(startIndex: Int): List<T> =
        subList(startIndex, size) + subList(0, startIndex)

    /** Records [workoutId] as the most recently finished workout, for [nextWorkout] rotation. */
    suspend fun setLastFinishedWorkout(workoutId: Long) {
        activePlanPreferences.setLastFinishedWorkoutId(workoutId)
    }

    /**
     * Records the user's perceived effort for each exercise in [entries] (exercise id to what to
     * record), all under today's date, as a single transaction. Used when a guided workout
     * session finishes, to save the ratings collected along the way. [entries]' weights are
     * passed in by the caller (the weight actually used at the time each exercise was rated)
     * rather than read fresh from the database, so they aren't affected by a same-session weight
     * increase (see [incrementExerciseWeight]) applied to an earlier-rated exercise afterwards.
     */
    suspend fun recordPerceivedEfforts(entries: Map<Long, ExercisePerformanceEntry>) {
        if (entries.isEmpty()) return
        val today = LocalDate.now()
        database.withTransaction {
            entries.forEach { (exerciseId, entry) ->
                val exercise = database.exerciseDao().getById(exerciseId) ?: return@forEach
                database.exerciseDao().update(
                    exercise.copy(
                        performanceHistory = exercise.performanceHistory + (today to entry),
                    ),
                )
            }
        }
    }

    /**
     * Increases the exercise with [exerciseId]'s prescribed [Exercise.weightKg] by its
     * [Exercise.weightIncrementKg], for next time it's performed. Used when the user opts to
     * raise the weight after rating an exercise "low" effort twice in a row.
     * [Exercise.performanceHistory] is left as-is (for later visualization); readers that
     * consider only the most recent effort should check it was recorded at the current weight,
     * since after this the latest entry no longer was.
     */
    suspend fun incrementExerciseWeight(exerciseId: Long) {
        val exercise = database.exerciseDao().getById(exerciseId) ?: return
        database.exerciseDao().update(
            exercise.copy(weightKg = exercise.weightKg + exercise.weightIncrementKg),
        )
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
