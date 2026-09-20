package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The top-level entity a user builds: a named plan made up of one or more [Workout]s.
 */
@Entity(tableName = "training_plans")
data class TrainingPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
)
