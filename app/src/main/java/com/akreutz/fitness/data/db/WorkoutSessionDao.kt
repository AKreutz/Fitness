package com.akreutz.fitness.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.akreutz.fitness.data.model.WorkoutSession
import com.akreutz.fitness.data.model.WorkoutSessionWithWorkout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Insert
    suspend fun insert(session: WorkoutSession): Long

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY completedAt DESC")
    fun observeAllMostRecentFirst(): Flow<List<WorkoutSessionWithWorkout>>
}
