package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single exercise within a [Workout]: a name, its equipment [type], and the prescription
 * (sets/reps/target weight) for it. [reps] is a preset rep scheme, one target per set (e.g.
 * `[8, 10, 12]`), chosen from a fixed list of options rather than typed freely. [weightKg] is
 * the starting weight, and [weightIncrementKg] is how much it should go up by between
 * progressions. [position] defines its order within the workout.
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
    val reps: List<Int>,
    val weightKg: Double,
    val weightIncrementKg: Double,
    val position: Int,
)
