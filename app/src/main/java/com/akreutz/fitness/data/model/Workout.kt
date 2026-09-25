package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * A single named workout (e.g. "Push Day") belonging to a [TrainingPlan], made up of one or
 * more [Exercise]s. [id] is a client-generated UUID (see [TrainingPlan.id]). [position] defines
 * its order within the plan. [updatedAt] is when it was last written, for future multi-device
 * sync to merge by.
 */
@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = TrainingPlan::class,
            parentColumns = ["id"],
            childColumns = ["trainingPlanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("trainingPlanId")],
)
data class Workout(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val trainingPlanId: String,
    val name: String,
    val position: Int,
    val updatedAt: Instant = Instant.now(),
)
