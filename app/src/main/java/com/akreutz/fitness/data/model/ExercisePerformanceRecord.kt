package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * One recorded performance of an [Exercise]: the weight used and how hard it felt, for the
 * [WorkoutSession] (identified by [completedAt], its [WorkoutSession.completedAt]) it was
 * performed in. Together, an exercise's records make up its [ExercisePerformanceHistory]. Kept as
 * its own table (rather than a column on [Exercise]) so two devices logging different performances
 * of the same exercise merge as independent rows instead of one clobbering the other.
 */
@Entity(
    tableName = "exercise_performance_records",
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
data class ExercisePerformanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val exerciseId: Long,
    val completedAt: Instant,
    val weightKg: Double,
    val perceivedEffort: PerceivedEffort,
)
