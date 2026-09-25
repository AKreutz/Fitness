package com.akreutz.fitness.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExercisePerformanceRecord
import com.akreutz.fitness.data.model.PurgedId
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.model.WorkoutSession

@Database(
    entities = [
        TrainingPlan::class,
        Workout::class,
        Exercise::class,
        WorkoutSession::class,
        ExercisePerformanceRecord::class,
        PurgedId::class,
    ],
    version = 13,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun trainingPlanDao(): TrainingPlanDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun exercisePerformanceRecordDao(): ExercisePerformanceRecordDao
    abstract fun purgedIdDao(): PurgedIdDao

    companion object {
        const val DATABASE_NAME = "fitness.db"
    }
}
