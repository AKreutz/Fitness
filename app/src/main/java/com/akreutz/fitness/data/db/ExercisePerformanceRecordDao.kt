package com.akreutz.fitness.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.akreutz.fitness.data.model.ExercisePerformanceRecord
import java.time.Instant

@Dao
interface ExercisePerformanceRecordDao {
    @Insert
    suspend fun insert(record: ExercisePerformanceRecord)

    @Delete
    suspend fun delete(record: ExercisePerformanceRecord)

    @Query(
        "SELECT * FROM exercise_performance_records " +
            "WHERE exerciseId = :exerciseId AND completedAt = :completedAt",
    )
    suspend fun getForExercise(exerciseId: String, completedAt: Instant): ExercisePerformanceRecord?

    @Query("SELECT * FROM exercise_performance_records WHERE exerciseId = :exerciseId")
    suspend fun getAllForExercise(exerciseId: String): List<ExercisePerformanceRecord>

    @Query("SELECT * FROM exercise_performance_records")
    suspend fun getAll(): List<ExercisePerformanceRecord>

    /** Inserts or updates each of [records] by id. Used to write a synced merge back. */
    @Upsert
    suspend fun upsertAll(records: List<ExercisePerformanceRecord>)
}
