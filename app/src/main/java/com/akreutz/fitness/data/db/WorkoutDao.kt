package com.akreutz.fitness.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.model.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insert(workout: Workout)

    @Update
    suspend fun update(workout: Workout)

    @Update
    suspend fun update(workouts: List<Workout>)

    @Delete
    suspend fun delete(workout: Workout)

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeById(id: String): Flow<WorkoutWithExercises?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE trainingPlanId = :trainingPlanId ORDER BY position")
    fun observeForTrainingPlan(trainingPlanId: String): Flow<List<WorkoutWithExercises>>

    @Query("SELECT COUNT(*) FROM workouts WHERE trainingPlanId = :trainingPlanId")
    suspend fun countForTrainingPlan(trainingPlanId: String): Int

    @Query("SELECT * FROM workouts")
    suspend fun getAll(): List<Workout>

    /** Inserts or updates each of [workouts] by id. Used to write a synced merge back. */
    @Upsert
    suspend fun upsertAll(workouts: List<Workout>)
}
