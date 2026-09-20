package com.akreutz.fitness.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.Workout

@Database(
    entities = [TrainingPlan::class, Workout::class, Exercise::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun trainingPlanDao(): TrainingPlanDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        const val DATABASE_NAME = "fitness.db"
    }
}
