package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * The top-level entity a user builds: a named plan made up of one or more [Workout]s.
 * [isPreloaded] marks a plan that shipped with the app (see [com.akreutz.fitness.data.seed.
 * PreloadedTrainingPlans]) rather than one the user created themselves; it's otherwise an
 * ordinary plan the user can rename, edit, or delete like any other. [updatedAt] is when it was
 * last written, for future multi-device sync to merge by.
 */
@Entity(tableName = "training_plans")
data class TrainingPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val isPreloaded: Boolean = false,
    val updatedAt: Instant = Instant.now(),
)
