package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * A named, reusable set of fixed weight levels matching a real cable machine's stack (e.g.
 * `[5.0, 10.0, ..., 100.0]`), assignable to a [ExerciseType.CABLE] [Exercise] via
 * [Exercise.weightStackId] so weight-increase suggestions can offer the stack's actual next level
 * instead of a freely typed one. [levelsKg] is kept sorted ascending. [id] is a client-generated
 * UUID (see [TrainingPlan.id]), and [updatedAt] is when it was last written, for future
 * multi-device sync to merge by.
 */
@Entity(tableName = "weight_stacks")
data class WeightStack(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val levelsKg: List<Double>,
    val updatedAt: Instant = Instant.now(),
) {
    companion object {
        /** The smallest level in [levelsKg] strictly greater than [currentKg], or `null` if
         * [currentKg] is already at or above the stack's top level. */
        fun nextLevelAbove(levelsKg: List<Double>, currentKg: Double): Double? =
            levelsKg.filter { it > currentKg }.minOrNull()
    }
}
