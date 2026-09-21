package com.akreutz.fitness.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.model.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insert(workout: Workout): Long

    @Update
    suspend fun update(workout: Workout)

    @Update
    suspend fun update(workouts: List<Workout>)

    @Delete
    suspend fun delete(workout: Workout)

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeById(id: Long): Flow<WorkoutWithExercises?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE trainingPlanId = :trainingPlanId ORDER BY position")
    fun observeForTrainingPlan(trainingPlanId: Long): Flow<List<WorkoutWithExercises>>

    @Query("SELECT COUNT(*) FROM workouts WHERE trainingPlanId = :trainingPlanId")
    suspend fun countForTrainingPlan(trainingPlanId: Long): Int
}
