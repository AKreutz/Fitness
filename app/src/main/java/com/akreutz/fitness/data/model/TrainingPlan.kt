package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * The top-level entity a user builds: a named plan made up of one or more [Workout]s. [id] is a
 * client-generated UUID rather than a database-assigned sequence, so two devices creating plans
 * offline never collide once synced. [isPreloaded] marks a plan that shipped with the app (see
 * [com.akreutz.fitness.data.seed.PreloadedTrainingPlans]) rather than one the user created
 * themselves; it's otherwise an ordinary plan the user can rename, edit, or delete like any other.
 * [updatedAt] is when it was last written, for future multi-device sync to merge by.
 */
@Entity(tableName = "training_plans")
data class TrainingPlan(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isPreloaded: Boolean = false,
    val updatedAt: Instant = Instant.now(),
)
