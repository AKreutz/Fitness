package com.akreutz.fitness.data.sync

import android.util.Log
import com.akreutz.fitness.data.db.ExerciseDao
import com.akreutz.fitness.data.db.ExercisePerformanceRecordDao
import com.akreutz.fitness.data.db.PurgedIdDao
import com.akreutz.fitness.data.db.TrainingPlanDao
import com.akreutz.fitness.data.db.WorkoutDao
import com.akreutz.fitness.data.db.WorkoutSessionDao
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExercisePerformanceRecord
import com.akreutz.fitness.data.model.PurgedId
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.model.WorkoutSession
import java.time.Instant

private const val TAG = "FitnessSync"

/**
 * Pulls the remote snapshot (if any), merges it with the local one, writes the merged result back
 * to Room, and pushes it back to the remote — retrying the whole cycle on a push conflict (someone
 * else wrote in the meantime). Merge is last-write-wins per row, keyed by id and compared by
 * `updatedAt`; [PurgedId] tombstones are unioned (never dropped) and used to filter any row whose
 * id has been permanently deleted on either side out of the merged result, so a hard delete
 * actually sticks instead of a stale copy reviving it.
 */
class SyncManager(
    private val trainingPlanDao: TrainingPlanDao,
    private val workoutDao: WorkoutDao,
    private val exerciseDao: ExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val exercisePerformanceRecordDao: ExercisePerformanceRecordDao,
    private val purgedIdDao: PurgedIdDao,
    private val remoteDataSource: RemoteDataSource,
) {
    private var lastKnownRemoteVersion: String? = null

    suspend fun sync() {
        val remote = remoteDataSource.pull()
        val local = readLocalSnapshot()
        val merged = if (remote == null) local else mergeSnapshots(local, remote.snapshot)

        applyToLocal(merged, currentLocal = local)

        val expectedVersion = remote?.version ?: lastKnownRemoteVersion
        when (val result = remoteDataSource.push(merged, expectedVersion)) {
            is PushResult.Success -> {
                lastKnownRemoteVersion = result.newVersion
            }
            PushResult.Conflict -> {
                Log.d(TAG, "push conflict, retrying sync")
                sync() // someone else wrote in the meantime; retry with fresh state
            }
        }
    }

    private suspend fun readLocalSnapshot(): FitnessSnapshot = FitnessSnapshot(
        trainingPlans = trainingPlanDao.getAll(),
        workouts = workoutDao.getAll(),
        exercises = exerciseDao.getAll(),
        workoutSessions = workoutSessionDao.getAll(),
        performanceRecords = exercisePerformanceRecordDao.getAll(),
        purgedIds = purgedIdDao.getAll(),
    )

    private suspend fun applyToLocal(merged: FitnessSnapshot, currentLocal: FitnessSnapshot) {
        if (merged === currentLocal) return
        trainingPlanDao.upsertAll(merged.trainingPlans)
        workoutDao.upsertAll(merged.workouts)
        exerciseDao.upsertAll(merged.exercises)
        workoutSessionDao.upsertAll(merged.workoutSessions)
        exercisePerformanceRecordDao.upsertAll(merged.performanceRecords)
        purgedIdDao.insert(merged.purgedIds)
    }

    private fun mergeSnapshots(local: FitnessSnapshot, remote: FitnessSnapshot): FitnessSnapshot {
        val purgedIds = mergeById(local.purgedIds, remote.purgedIds, PurgedId::id, PurgedId::purgedAt)
        val purgedIdSet = purgedIds.mapTo(mutableSetOf()) { it.id }
        return FitnessSnapshot(
            trainingPlans = mergeById(local.trainingPlans, remote.trainingPlans, TrainingPlan::id, TrainingPlan::updatedAt)
                .filterNot { it.id in purgedIdSet },
            workouts = mergeById(local.workouts, remote.workouts, Workout::id, Workout::updatedAt)
                .filterNot { it.id in purgedIdSet },
            exercises = mergeById(local.exercises, remote.exercises, Exercise::id, Exercise::updatedAt)
                .filterNot { it.id in purgedIdSet },
            workoutSessions = mergeById(local.workoutSessions, remote.workoutSessions, WorkoutSession::id, WorkoutSession::updatedAt)
                .filterNot { it.id in purgedIdSet },
            performanceRecords = mergeById(
                local.performanceRecords,
                remote.performanceRecords,
                ExercisePerformanceRecord::id,
                ExercisePerformanceRecord::updatedAt,
            ).filterNot { it.id in purgedIdSet },
            purgedIds = purgedIds,
        )
    }

    private fun <T, K> mergeById(
        local: List<T>,
        remote: List<T>,
        keyOf: (T) -> K,
        updatedAtOf: (T) -> Instant,
    ): List<T> {
        val merged = local.associateBy(keyOf).toMutableMap()
        for (remoteRow in remote) {
            val key = keyOf(remoteRow)
            val localRow = merged[key]
            if (localRow == null || updatedAtOf(remoteRow) > updatedAtOf(localRow)) {
                merged[key] = remoteRow
            }
        }
        return merged.values.toList()
    }
}
