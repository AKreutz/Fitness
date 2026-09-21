package com.akreutz.fitness.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A single exercise within a [Workout]: a name, its equipment [type], and the prescription
 * (sets/reps/target weight) for it. [reps] is a preset rep scheme, one target per set (e.g.
 * `[8, 10, 12]`), chosen from a fixed list of options rather than typed freely. [weightKg] is
 * the starting weight, and [weightIncrementKg] is how much it should go up by between
 * progressions. [restSeconds] is how long to rest between working sets during a guided session.
 * [position] defines its order within the workout. [performanceHistory] records, per date
 * performed, the weight used and the perceived effort of that performance.
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
    val restSeconds: Int = DEFAULT_REST_SECONDS,
    val position: Int,
    val performanceHistory: ExercisePerformanceHistory = emptyMap(),
) {
    companion object {
        /** The rest duration assumed for exercises created before [restSeconds] existed. */
        const val DEFAULT_REST_SECONDS: Int = 90
    }
}

/**
 * The most recent entry in [Exercise.performanceHistory], but only if it was recorded at the
 * exercise's current [Exercise.weightKg]. Once the weight changes (e.g. after a weight-increase
 * offer), the latest entry still reflects how the *old* weight felt, which no longer applies to
 * the new one, so callers that care about "how did this feel most recently" should use this
 * instead of reading [Exercise.performanceHistory] directly.
 */
val Exercise.lastPerformanceAtCurrentWeight: ExercisePerformanceEntry?
    get() = performanceHistory.maxByOrNull { it.key }
        ?.value
        ?.takeIf { it.weightKg == weightKg }

/**
 * This exercise's [ExercisePerformanceEntry] recorded on [date], or `null` if it wasn't performed
 * (or rated) that day. Used to show a past [com.akreutz.fitness.data.model.WorkoutSession]'s
 * per-exercise stats, since sessions don't keep their own snapshot of them; note this is
 * ambiguous if the same workout (and so the same exercise) was completed more than once on
 * [date], since [performanceHistory] only keeps one entry per day.
 */
fun Exercise.performanceOn(date: LocalDate): ExercisePerformanceEntry? = performanceHistory[date]
