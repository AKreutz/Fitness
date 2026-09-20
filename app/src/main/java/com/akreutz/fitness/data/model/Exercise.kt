package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single exercise within a [Workout]: a name, its equipment [type], and the prescription
 * (sets/reps/target weight) for it. [position] defines its order within the workout.
 */
@Entity(
    tableName = "exercises",
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
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val workoutId: Long,
    val name: String,
    val type: ExerciseType,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val position: Int,
)
