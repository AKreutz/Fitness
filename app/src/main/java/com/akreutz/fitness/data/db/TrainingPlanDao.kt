package com.akreutz.fitness.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingPlanDao {
    @Insert
    suspend fun insert(trainingPlan: TrainingPlan)

    @Update
    suspend fun update(trainingPlan: TrainingPlan)

    @Delete
    suspend fun delete(trainingPlan: TrainingPlan)

    @Transaction
    @Query("SELECT * FROM training_plans WHERE id = :id")
    fun observeById(id: String): Flow<TrainingPlanWithWorkouts?>

    @Transaction
    @Query("SELECT * FROM training_plans ORDER BY name")
    fun observeAll(): Flow<List<TrainingPlanWithWorkouts>>

    @Query("SELECT * FROM training_plans")
    suspend fun getAll(): List<TrainingPlan>

    /** Inserts or updates each of [trainingPlans] by id. Used to write a synced merge back. */
    @Upsert
    suspend fun upsertAll(trainingPlans: List<TrainingPlan>)
}
