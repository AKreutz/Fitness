package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * The top-level entity a user builds: a named plan made up of one or more [Workout]s. [id] is a
 * client-generated UUID rather than a database-assigned sequence, so two devices creating plans
 * offline never collide once synced. [updatedAt] is when it was last written, for future
 * multi-device sync to merge by.
 */
@Entity(tableName = "training_plans")
data class TrainingPlan(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val updatedAt: Instant = Instant.now(),
)
