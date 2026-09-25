package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * A single completed occurrence of a [Workout]: recorded once a guided workout session (see
 * [com.akreutz.fitness.ui.session.WorkoutSessionViewModel]) finishes. [id] is a client-generated
 * UUID (see [TrainingPlan.id]). [startedAt] is when the session began and [completedAt] when it
 * finished; [durationSeconds] is the elapsed time between them. If the workout it was for is
 * later deleted, the session is deleted with it. [updatedAt] is when it was last written, for
 * future multi-device sync to merge by.
 */
@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class WorkoutSession(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val startedAt: Instant,
    val completedAt: Instant,
    val durationSeconds: Long,
    val updatedAt: Instant = Instant.now(),
)
