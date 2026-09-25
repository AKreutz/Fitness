package com.akreutz.fitness.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.akreutz.fitness.data.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert
    suspend fun insert(exercise: Exercise)

    @Update
    suspend fun update(exercise: Exercise)

    @Update
    suspend fun update(exercises: List<Exercise>)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY position")
    fun observeForWorkout(workoutId: String): Flow<List<Exercise>>

    @Query("SELECT COUNT(*) FROM exercises WHERE workoutId = :workoutId")
    suspend fun countForWorkout(workoutId: String): Int

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: String): Exercise?

    @Query("SELECT * FROM exercises")
    suspend fun getAll(): List<Exercise>

    /** Inserts or updates each of [exercises] by id. Used to write a synced merge back. */
    @Upsert
    suspend fun upsertAll(exercises: List<Exercise>)
}
