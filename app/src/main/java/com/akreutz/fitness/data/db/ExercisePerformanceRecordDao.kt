package com.akreutz.fitness.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
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
}
