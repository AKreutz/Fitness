package com.akreutz.fitness.data.repository

import androidx.room.withTransaction
import com.akreutz.fitness.data.db.FitnessDatabase
import com.akreutz.fitness.data.model.DraftWorkout
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExercisePerformanceEntry
import com.akreutz.fitness.data.model.ExercisePerformanceRecord
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.PurgedId
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.model.WorkoutSession
import com.akreutz.fitness.data.model.WorkoutSessionWithWorkout
import com.akreutz.fitness.data.model.WorkoutWithExercises
import com.akreutz.fitness.data.prefs.ActivePlanPreferences
import com.akreutz.fitness.data.seed.PreloadedTrainingPlan
import com.akreutz.fitness.data.seed.PreloadedTrainingPlans
import com.akreutz.fitness.data.sync.LocalChangeTracker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Mediates reads/writes of [TrainingPlan]s (and their nested [Workout]s) against Room, and
 * tracks which one the user is currently associated with. Every write marks [localChangeTracker]
 * dirty, for the sync layer to know local data has changed since the last sync.
 */
class TrainingPlanRepository(
    private val database: FitnessDatabase,
    private val activePlanPreferences: ActivePlanPreferences,
    private val localChangeTracker: LocalChangeTracker,
) {

    fun observeTrainingPlans(): Flow<List<TrainingPlanWithWorkouts>> =
        database.trainingPlanDao().observeAll()

    /** The workout with [workoutId] together with its exercises, or `null` if it doesn't exist. */
    fun observeWorkout(workoutId: String): Flow<WorkoutWithExercises?> =
        database.workoutDao().observeById(workoutId)

    /** Every completed [WorkoutSession], each with the [Workout] it was for, most recent first. */
    fun observeWorkoutSessions(): Flow<List<WorkoutSessionWithWorkout>> =
        database.workoutSessionDao().observeAllMostRecentFirst()

    /**
     * Deletes [session] from the completed-workout history, as a single transaction, but only if
     * it's still the *most recently completed* session overall (across every workout) — undoing
     * an older one could conflict with rating/weight changes a later session already made, so the
     * workouts screen only ever offers this for the latest one. Also undoes that session's effect
     * on each of its workout's exercises: the [ExercisePerformanceRecord] it recorded (keyed by
     * [session]'s [WorkoutSession.completedAt]) is removed, and if a weight-increase
     * offer was accepted for that exercise afterwards (see [setExerciseWeight]), its
     * [Exercise.weightKg] is rolled back to the weight actually used in [session] — the removed
     * entry's own [ExercisePerformanceEntry.weightKg], which is what the exercise was still at at
     * the time of that offer.
     */
    suspend fun deleteWorkoutSession(session: WorkoutSession) {
        database.withTransaction {
            val mostRecent = database.workoutSessionDao().observeAllMostRecentFirst().first()
                .firstOrNull()?.session
            if (mostRecent?.id != session.id) return@withTransaction

            database.workoutSessionDao().delete(session)
            database.purgedIdDao().insert(listOf(PurgedId(id = session.id)))
            val exercises = database.exerciseDao().observeForWorkout(session.workoutId).first()
            val purgedRecordIds = mutableListOf<PurgedId>()
            exercises.forEach { exercise ->
                val record = database.exercisePerformanceRecordDao()
                    .getForExercise(exercise.id, session.completedAt) ?: return@forEach
                database.exercisePerformanceRecordDao().delete(record)
                purgedRecordIds += PurgedId(id = record.id)
                database.exerciseDao().update(
                    exercise.copy(weightKg = record.weightKg, updatedAt = Instant.now()),
                )
            }
            database.purgedIdDao().insert(purgedRecordIds)
        }
        localChangeTracker.markDirty()
    }

    /** The plan the user is currently associated with, or `null` if none has been created yet. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeActiveTrainingPlan(): Flow<TrainingPlanWithWorkouts?> =
        activePlanPreferences.activeTrainingPlanId.flatMapLatest { id ->
            if (id == null) flowOf(null) else database.trainingPlanDao().observeById(id)
        }

    /** The plan with [trainingPlanId], or `null` if it doesn't exist (e.g. was just deleted). */
    fun observeTrainingPlan(trainingPlanId: String): Flow<TrainingPlanWithWorkouts?> =
        database.trainingPlanDao().observeById(trainingPlanId)

    /**
     * Creates a new [TrainingPlan] with the given [name] and one [Workout] per entry in
     * [workouts] (in order), each with its own drafted exercises, as a single transaction, and
     * marks it as the user's active plan. Used to save the whole onboarding flow (plan name,
     * workouts, and any exercises added to them) at once, since nothing is persisted until the
     * user finishes onboarding.
     */
    suspend fun createTrainingPlan(name: String, workouts: List<DraftWorkout>): String {
        val trainingPlan = TrainingPlan(name = name)
        database.withTransaction {
            database.trainingPlanDao().insert(trainingPlan)
            workouts.forEachIndexed { workoutIndex, workout ->
                val workoutEntity = Workout(
                    trainingPlanId = trainingPlan.id,
                    name = workout.name,
                    position = workoutIndex,
                )
                database.workoutDao().insert(workoutEntity)
                workout.exercises.forEachIndexed { exerciseIndex, exercise ->
                    database.exerciseDao().insert(
                        Exercise(
                            workoutId = workoutEntity.id,
                            name = exercise.name,
                            type = exercise.type,
                            sets = exercise.sets,
                            reps = exercise.reps,
                            weightKg = exercise.weightKg,
                            weightIncrementKg = exercise.weightIncrementKg,
                            restSeconds = exercise.restSeconds,
                            position = exerciseIndex,
                        ),
                    )
                }
            }
        }
        localChangeTracker.markDirty()
        activePlanPreferences.setActiveTrainingPlanId(trainingPlan.id)
        return trainingPlan.id
    }

    /**
     * Inserts [plan] as a new [TrainingPlan] (marked [TrainingPlan.isPreloaded]) together with its
     * workouts, exercises, and logged [PreloadedSession]s — seeded as [WorkoutSession]s and, for
     * each exercise a session names, an [ExercisePerformanceRecord] keyed by that session's
     * [WorkoutSession.completedAt] — as a single transaction, unless a preloaded plan
     * with that name already exists, in which case this does nothing. Assumes [plan] has exactly
     * one workout, since every seeded session is logged against it; multi-workout preloaded plans
     * would need each [PreloadedSession] to say which workout it was for. Never touches the active
     * plan or any other existing plan, so it's safe to call unconditionally (e.g. every app start)
     * without ever overriding a plan the user created or duplicating itself. Used to offer plans
     * that ship with the app (see [com.akreutz.fitness.data.seed.PreloadedTrainingPlans]) from the
     * plans screen.
     */
    suspend fun createPreloadedTrainingPlanIfMissing(plan: PreloadedTrainingPlan) {
        val inserted = database.withTransaction {
            val alreadyExists = database.trainingPlanDao().observeAll().first()
                .any { it.trainingPlan.isPreloaded && it.trainingPlan.name == plan.name }
            if (alreadyExists) return@withTransaction false

            val trainingPlan = TrainingPlan(name = plan.name, isPreloaded = true)
            database.trainingPlanDao().insert(trainingPlan)
            plan.workouts.forEachIndexed { workoutIndex, workout ->
                val workoutEntity = Workout(
                    trainingPlanId = trainingPlan.id,
                    name = workout.name,
                    position = workoutIndex,
                )
                database.workoutDao().insert(workoutEntity)

                // Each session's completion instant becomes the key its logged exercises'
                // performanceHistory entries are recorded under, matching how a real guided
                // session records them (see recordPerceivedEfforts) and its own WorkoutSession row.
                val sessionCompletedAt = plan.sessions.associateWith { session ->
                    session.completedOn.atStartOfDay(ZoneId.systemDefault()).toInstant()
                }

                workout.exercises.forEachIndexed { exerciseIndex, exercise ->
                    val exerciseEntity = Exercise(
                        workoutId = workoutEntity.id,
                        name = exercise.name,
                        type = exercise.type,
                        sets = PreloadedTrainingPlans.reps.size,
                        reps = PreloadedTrainingPlans.reps,
                        weightKg = exercise.weightKg,
                        weightIncrementKg = exercise.weightIncrementKg,
                        position = exerciseIndex,
                    )
                    database.exerciseDao().insert(exerciseEntity)

                    plan.sessions.forEach { session ->
                        val performance = session.performances[exercise.name] ?: return@forEach
                        database.exercisePerformanceRecordDao().insert(
                            ExercisePerformanceRecord(
                                exerciseId = exerciseEntity.id,
                                completedAt = sessionCompletedAt.getValue(session),
                                weightKg = performance.weightKg,
                                perceivedEffort = performance.perceivedEffort,
                            ),
                        )
                    }
                }

                if (workoutIndex == 0) {
                    plan.sessions.forEach { session ->
                        val completedAt = sessionCompletedAt.getValue(session)
                        database.workoutSessionDao().insert(
                            WorkoutSession(
                                workoutId = workoutEntity.id,
                                startedAt = completedAt,
                                completedAt = completedAt,
                                durationSeconds = 0L,
                            ),
                        )
                    }
                }
            }
            true
        }
        if (inserted) localChangeTracker.markDirty()
    }

    /**
     * The [Workout] to offer starting next for [trainingPlan]: the one after whichever was
     * finished last (by plan order), wrapping back to the first; or simply the first workout if
     * none has been finished yet, or the plan has no workouts. Starting a workout without
     * finishing it (e.g. cancelling) doesn't advance this, so it keeps being offered. "Finished
     * last" is read straight from the logged [WorkoutSession]s rather than separately tracked
     * state, so deleting the most recent one (see [deleteWorkoutSession]) is reflected here too.
     */
    suspend fun nextWorkout(trainingPlan: TrainingPlanWithWorkouts): Workout? {
        val workouts = trainingPlan.workouts.map { it.workout }
        val lastFinishedId = database.workoutSessionDao()
            .observeLastFinishedWorkoutId(trainingPlan.trainingPlan.id)
            .first()
        return workouts.getOrNull(nextWorkoutIndex(workouts, lastFinishedId))
    }

    /**
     * Like [nextWorkout], but reactive: re-emits the plan's workouts (each still with its
     * exercises) reordered so the one up next comes first, keeping the rest in their predefined
     * order, whenever [trainingPlan] or the last-finished workout changes.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeWorkoutsNextFirst(
        trainingPlan: Flow<TrainingPlanWithWorkouts>,
    ): Flow<List<WorkoutWithExercises>> =
        trainingPlan.flatMapLatest { plan ->
            database.workoutSessionDao().observeLastFinishedWorkoutId(plan.trainingPlan.id)
                .map { lastFinishedId ->
                    val nextIndex = nextWorkoutIndex(plan.workouts.map { it.workout }, lastFinishedId)
                    if (nextIndex <= 0) plan.workouts else plan.workouts.rotated(nextIndex)
                }
        }

    /**
     * The index within [workouts] (in plan order) of the workout up next: the one after
     * [lastFinishedId], wrapping back to the first; or `0` if [lastFinishedId] wasn't finished or
     * isn't found, or [workouts] is empty (an out-of-range index, consistent with
     * [List.getOrNull]-style lookups).
     */
    private fun nextWorkoutIndex(workouts: List<Workout>, lastFinishedId: String?): Int {
        if (workouts.isEmpty()) return 0
        val lastFinishedIndex = workouts.indexOfFirst { it.id == lastFinishedId }
        return if (lastFinishedIndex == -1) 0 else (lastFinishedIndex + 1) % workouts.size
    }

    private fun <T> List<T>.rotated(startIndex: Int): List<T> =
        subList(startIndex, size) + subList(0, startIndex)

    /**
     * Records the user's perceived effort for each exercise in [entries] (exercise id to what to
     * record), keyed under the session's completion instant, and logs a [WorkoutSession] for
     * [workoutId] that began at [startedAt] and finishes now, as a single transaction. Used when a
     * guided workout session finishes, to save the ratings collected along the way together with
     * the session itself. [entries]' weights are passed in by the caller (the weight actually used
     * at the time each exercise was rated) rather than read fresh from the database, so they
     * aren't affected by a same-session weight increase (see [setExerciseWeight]) applied to an
     * earlier-rated exercise afterwards. Keying by the exact completion instant (rather than just
     * the date) means a second workout completed the same day gets its own entry instead of
     * overwriting the first's.
     */
    suspend fun recordPerceivedEfforts(
        workoutId: String,
        entries: Map<String, ExercisePerformanceEntry>,
        startedAt: Instant,
    ) {
        val completedAt = Instant.now()
        database.withTransaction {
            entries.forEach { (exerciseId, entry) ->
                database.exercisePerformanceRecordDao().insert(
                    ExercisePerformanceRecord(
                        exerciseId = exerciseId,
                        completedAt = completedAt,
                        weightKg = entry.weightKg,
                        perceivedEffort = entry.perceivedEffort,
                    ),
                )
            }
            database.workoutSessionDao().insert(
                WorkoutSession(
                    workoutId = workoutId,
                    startedAt = startedAt,
                    completedAt = completedAt,
                    durationSeconds = Duration.between(startedAt, completedAt).seconds,
                ),
            )
        }
        localChangeTracker.markDirty()
    }

    /**
     * Sets the exercise with [exerciseId]'s prescribed [Exercise.weightKg] to [weightKg], for next
     * time it's performed. Used when the user opts to raise the weight after rating an exercise
     * "easy" effort twice in a row: callers compute [weightKg] as
     * `exercise.weightKg + exercise.weightIncrementKg` for most exercises, but let the user
     * choose it directly for [ExerciseType.CABLE] ones, since cable machines' weight levels aren't
     * evenly spaced. Its performance history is left as-is (for later visualization); readers
     * that consider only the most recent effort should check it was recorded at the current
     * weight, since after this the latest entry no longer was.
     */
    suspend fun setExerciseWeight(exerciseId: String, weightKg: Double) {
        val exercise = database.exerciseDao().getById(exerciseId) ?: return
        database.exerciseDao().update(exercise.copy(weightKg = weightKg, updatedAt = Instant.now()))
        localChangeTracker.markDirty()
    }

    /** Switches the plan the user is currently associated with to the one with [id]. */
    suspend fun setActiveTrainingPlan(id: String) {
        activePlanPreferences.setActiveTrainingPlanId(id)
    }

    /**
     * Deletes [trainingPlan] together with its workouts, their exercises, and any workout
     * sessions logged against them (all cascade via foreign keys) — tombstoning every one of
     * those ids (see [PurgedId]) first, since the cascade itself happens below Room and emits no
     * per-row callback to hook into. If it was the active plan, clears that so the app falls back
     * to no plan rather than pointing at a deleted one.
     */
    suspend fun deleteTrainingPlan(trainingPlan: TrainingPlan) {
        val wasActive = activePlanPreferences.activeTrainingPlanId.first() == trainingPlan.id
        database.withTransaction {
            val workouts = database.workoutDao().observeForTrainingPlan(trainingPlan.id).first()
            purgeWorkoutsCascade(workouts.map { it.workout })
            database.purgedIdDao().insert(listOf(PurgedId(id = trainingPlan.id)))
            database.trainingPlanDao().delete(trainingPlan)
        }
        localChangeTracker.markDirty()
        if (wasActive) {
            activePlanPreferences.clearActiveTrainingPlanId()
        }
    }

    /**
     * Tombstones (see [PurgedId]) [workouts] together with their exercises, those exercises'
     * performance records, and any workout sessions logged against them — everything that
     * deleting [workouts] cascades away — without deleting anything itself; callers delete the
     * workouts (or their owning plan) afterwards. Must run inside the same transaction as that
     * delete, so nothing else can insert new children in between reading and tombstoning them.
     */
    private suspend fun purgeWorkoutsCascade(workouts: List<Workout>) {
        val purgedIds = mutableListOf<PurgedId>()
        workouts.forEach { workout ->
            purgedIds += PurgedId(id = workout.id)
            database.exerciseDao().observeForWorkout(workout.id).first().forEach { exercise ->
                purgedIds += PurgedId(id = exercise.id)
                database.exercisePerformanceRecordDao().getAllForExercise(exercise.id).forEach {
                    purgedIds += PurgedId(id = it.id)
                }
            }
            database.workoutSessionDao().getForWorkout(workout.id)
                .forEach { purgedIds += PurgedId(id = it.id) }
        }
        database.purgedIdDao().insert(purgedIds)
    }

    /**
     * Adds a new [Exercise] to the workout with [workoutId], placed after its existing
     * exercises.
     */
    suspend fun addExercise(
        workoutId: String,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
        restSeconds: Int,
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
                    restSeconds = restSeconds,
                    position = position,
                ),
            )
        }
        localChangeTracker.markDirty()
    }

    /** Renames [workout] to [name]. Used from the plan editor. */
    suspend fun renameWorkout(workout: Workout, name: String) {
        database.workoutDao().update(workout.copy(name = name, updatedAt = Instant.now()))
        localChangeTracker.markDirty()
    }

    /**
     * Adds a new, empty [Workout] named [name] to the plan with [trainingPlanId], placed after
     * its existing workouts. Used from the plan editor.
     */
    suspend fun addWorkout(trainingPlanId: String, name: String): String {
        val workout = database.withTransaction {
            val position = database.workoutDao().countForTrainingPlan(trainingPlanId)
            Workout(trainingPlanId = trainingPlanId, name = name, position = position).also {
                database.workoutDao().insert(it)
            }
        }
        localChangeTracker.markDirty()
        return workout.id
    }

    /**
     * Deletes [workout] together with its exercises and any workout sessions logged against it
     * (both cascade via foreign keys, and tombstoned via [PurgedId] first — see
     * [purgeWorkoutsCascade]), then renumbers its remaining sibling workouts so
     * [Workout.position] stays a dense 0..n-1 sequence. Used from the plan editor.
     */
    suspend fun deleteWorkout(workout: Workout) {
        database.withTransaction {
            purgeWorkoutsCascade(listOf(workout))
            database.workoutDao().delete(workout)
            val siblings = database.workoutDao().observeForTrainingPlan(workout.trainingPlanId)
                .first()
            database.workoutDao().update(
                siblings.mapIndexedNotNull { index, sibling ->
                    sibling.workout.takeIf { it.position != index }
                        ?.copy(position = index, updatedAt = Instant.now())
                },
            )
        }
        localChangeTracker.markDirty()
    }

    /**
     * Reorders the plan with [trainingPlanId]'s workouts to match [orderedWorkoutIds] (every
     * workout id in the plan, in the desired order). Used from the plan editor's drag-to-reorder.
     */
    suspend fun reorderWorkouts(trainingPlanId: String, orderedWorkoutIds: List<String>) {
        database.withTransaction {
            val workouts = database.workoutDao().observeForTrainingPlan(trainingPlanId).first()
                .associateBy { it.workout.id }
            database.workoutDao().update(
                orderedWorkoutIds.mapIndexedNotNull { index, id ->
                    workouts[id]?.workout?.takeIf { it.position != index }
                        ?.copy(position = index, updatedAt = Instant.now())
                },
            )
        }
        localChangeTracker.markDirty()
    }

    /** Updates every field of [exercise]. Used from the plan editor. */
    suspend fun updateExercise(
        exercise: Exercise,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
        restSeconds: Int,
    ) {
        database.exerciseDao().update(
            exercise.copy(
                name = name,
                type = type,
                sets = sets,
                reps = reps,
                weightKg = weightKg,
                weightIncrementKg = weightIncrementKg,
                restSeconds = restSeconds,
                updatedAt = Instant.now(),
            ),
        )
        localChangeTracker.markDirty()
    }

    /**
     * Deletes [exercise] together with its performance records (cascading via foreign key, and
     * tombstoned via [PurgedId] first, since the cascade itself happens below Room), then
     * renumbers its remaining sibling exercises so [Exercise.position] stays a dense 0..n-1
     * sequence. Used from the plan editor.
     */
    suspend fun deleteExercise(exercise: Exercise) {
        database.withTransaction {
            val purgedIds = database.exercisePerformanceRecordDao().getAllForExercise(exercise.id)
                .map { PurgedId(id = it.id) } + PurgedId(id = exercise.id)
            database.purgedIdDao().insert(purgedIds)
            database.exerciseDao().delete(exercise)
            val siblings = database.exerciseDao().observeForWorkout(exercise.workoutId).first()
            database.exerciseDao().update(
                siblings.mapIndexedNotNull { index, sibling ->
                    sibling.takeIf { it.position != index }
                        ?.copy(position = index, updatedAt = Instant.now())
                },
            )
        }
        localChangeTracker.markDirty()
    }

    /**
     * Reorders the workout with [workoutId]'s exercises to match [orderedExerciseIds] (every
     * exercise id in the workout, in the desired order). Used from the plan editor's
     * drag-to-reorder.
     */
    suspend fun reorderExercises(workoutId: String, orderedExerciseIds: List<String>) {
        database.withTransaction {
            val exercises = database.exerciseDao().observeForWorkout(workoutId).first()
                .associateBy { it.id }
            database.exerciseDao().update(
                orderedExerciseIds.mapIndexedNotNull { index, id ->
                    exercises[id]?.takeIf { it.position != index }
                        ?.copy(position = index, updatedAt = Instant.now())
                },
            )
        }
        localChangeTracker.markDirty()
    }
}
