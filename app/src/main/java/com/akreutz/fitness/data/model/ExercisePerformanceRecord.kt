package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * One recorded performance of an [Exercise]: the weight used and how hard it felt, for the
 * [WorkoutSession] (identified by [completedAt], its [WorkoutSession.completedAt]) it was
 * performed in. [id] is a client-generated UUID (see [TrainingPlan.id]). Together, an exercise's
 * records make up its [ExercisePerformanceHistory]. Kept as its own table (rather than a column
 * on [Exercise]) so two devices logging different performances of the same exercise merge as
 * independent rows instead of one clobbering the other. [updatedAt] is when it was last written,
 * for future multi-device sync to merge by.
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
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val completedAt: Instant,
    val weightKg: Double,
    val perceivedEffort: PerceivedEffort,
    val updatedAt: Instant = Instant.now(),
)
