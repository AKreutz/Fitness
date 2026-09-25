package com.akreutz.fitness.data.sync

import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExercisePerformanceRecord
import com.akreutz.fitness.data.model.PurgedId
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.model.WorkoutSession

/**
 * A point-in-time copy of every Room table [SyncManager] merges, decoupled from Room itself so
 * [com.akreutz.fitness.data.sync.drive.GoogleDriveDataSource] (or any other [RemoteDataSource])
 * doesn't need to depend on it directly.
 */
data class FitnessSnapshot(
    val trainingPlans: List<TrainingPlan>,
    val workouts: List<Workout>,
    val exercises: List<Exercise>,
    val workoutSessions: List<WorkoutSession>,
    val performanceRecords: List<ExercisePerformanceRecord>,
    val purgedIds: List<PurgedId>,
)
