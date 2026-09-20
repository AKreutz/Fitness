package com.akreutz.fitness.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingPlanDao {
    @Insert
    suspend fun insert(trainingPlan: TrainingPlan): Long

    @Update
    suspend fun update(trainingPlan: TrainingPlan)

    @Delete
    suspend fun delete(trainingPlan: TrainingPlan)

    @Transaction
    @Query("SELECT * FROM training_plans WHERE id = :id")
    fun observeById(id: Long): Flow<TrainingPlanWithWorkouts?>

    @Transaction
    @Query("SELECT * FROM training_plans ORDER BY name")
    fun observeAll(): Flow<List<TrainingPlanWithWorkouts>>
}
