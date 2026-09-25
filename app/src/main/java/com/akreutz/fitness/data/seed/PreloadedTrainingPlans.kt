package com.akreutz.fitness.data.seed

import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.RepScheme
import java.time.LocalDate

/**
 * A preloaded [com.akreutz.fitness.data.model.Exercise], ready to insert once its `workoutId` is
 * known. [name] must match a key of each [PreloadedSession.performances] that logs this exercise.
 */
data class PreloadedExercise(
    val name: String,
    val type: ExerciseType,
    val weightKg: Double,
    val weightIncrementKg: Double,
)

/** A preloaded workout: a name and its ordered [exercises]. */
data class PreloadedWorkout(
    val name: String,
    val exercises: List<PreloadedExercise>,
)

/** How one exercise went in a [PreloadedSession]: the weight used and how it felt. */
data class PreloadedPerformance(
    val weightKg: Double,
    val perceivedEffort: PerceivedEffort,
)

/**
 * A completed, logged occurrence of a [PreloadedWorkout], seeded as a
 * [com.akreutz.fitness.data.model.WorkoutSession] on [completedOn]. [performances] maps each
 * logged exercise's name to how it went; an exercise in the workout with no entry here (e.g. a
 * cable exercise whose numbers aren't comparable across gyms, see [PreloadedTrainingPlans.fullBody])
 * is seeded with a current weight only, no history.
 */
data class PreloadedSession(
    val completedOn: LocalDate,
    val performances: Map<String, PreloadedPerformance>,
)

/** A preloaded training plan: a name, its ordered [workouts], and their logged [PreloadedSession]s. */
data class PreloadedTrainingPlan(
    val name: String,
    val workouts: List<PreloadedWorkout>,
    val sessions: List<PreloadedSession> = emptyList(),
)

/**
 * Training plans that ship with the app, offered to the user from the plans screen rather than
 * created automatically, so they never override or get mixed up with a plan the user makes
 * themselves. See [com.akreutz.fitness.data.repository.TrainingPlanRepository.
 * createPreloadedTrainingPlanIfMissing].
 */
object PreloadedTrainingPlans {

    /** The rep scheme every preloaded exercise uses (see [RepScheme.options]). */
    val reps: List<Int> = RepScheme.options.first()

    private const val SQUAT = "Squat"
    private const val DEADLIFT = "Deadlift"
    private const val BENCH_PRESS = "Bench Press"
    private const val LAT_PULLDOWN = "Lat Pulldown"
    private const val ROW = "Row"
    private const val OVERHEAD_PRESS = "Overhead Press"

    /**
     * A full-body plan of one workout (squat, deadlift, bench, lat pulldown, row, and overhead
     * press), seeded with eight weeks of logged sessions. The two cable exercises (lat pulldown,
     * row) are seeded with only their most recent weight, no session history: their numbers
     * aren't comparable week to week since they were logged across different gyms whose cable
     * machines aren't calibrated the same way. The final logged session is excluded because it
     * wasn't completed (only squat, deadlift, and bench were done that day).
     */
    val fullBody: PreloadedTrainingPlan = PreloadedTrainingPlan(
        name = "Full Body",
        workouts = listOf(
            PreloadedWorkout(
                name = "Full Body",
                exercises = listOf(
                    PreloadedExercise(SQUAT, ExerciseType.FREE_WEIGHTS, weightKg = 50.0, weightIncrementKg = 2.5),
                    PreloadedExercise(DEADLIFT, ExerciseType.FREE_WEIGHTS, weightKg = 65.0, weightIncrementKg = 2.5),
                    PreloadedExercise(BENCH_PRESS, ExerciseType.FREE_WEIGHTS, weightKg = 35.0, weightIncrementKg = 2.5),
                    PreloadedExercise(LAT_PULLDOWN, ExerciseType.CABLE, weightKg = 35.0, weightIncrementKg = 5.0),
                    PreloadedExercise(ROW, ExerciseType.CABLE, weightKg = 39.0, weightIncrementKg = 5.0),
                    PreloadedExercise(OVERHEAD_PRESS, ExerciseType.FREE_WEIGHTS, weightKg = 20.0, weightIncrementKg = 1.0),
                ),
            ),
        ),
        sessions = listOf(
            session(
                2026, 7, 29,
                SQUAT to (30.0 to PerceivedEffort.MEDIUM),
                DEADLIFT to (50.0 to PerceivedEffort.HARD),
                BENCH_PRESS to (25.0 to PerceivedEffort.EASY),
                OVERHEAD_PRESS to (20.0 to PerceivedEffort.HARD),
            ),
            session(
                2026, 8, 5,
                SQUAT to (35.0 to PerceivedEffort.MEDIUM),
                DEADLIFT to (50.0 to PerceivedEffort.MEDIUM),
                BENCH_PRESS to (27.5 to PerceivedEffort.EASY),
                OVERHEAD_PRESS to (20.0 to PerceivedEffort.MEDIUM),
            ),
            session(
                2026, 8, 13,
                SQUAT to (35.0 to PerceivedEffort.EASY),
                DEADLIFT to (50.0 to PerceivedEffort.EASY),
                BENCH_PRESS to (30.0 to PerceivedEffort.EASY),
                OVERHEAD_PRESS to (20.0 to PerceivedEffort.MEDIUM),
            ),
            session(
                2026, 8, 19,
                SQUAT to (40.0 to PerceivedEffort.HARD),
                DEADLIFT to (55.0 to PerceivedEffort.MEDIUM),
                BENCH_PRESS to (35.0 to PerceivedEffort.HARD),
                OVERHEAD_PRESS to (20.0 to PerceivedEffort.MEDIUM),
            ),
            session(
                2026, 8, 26,
                SQUAT to (50.0 to PerceivedEffort.HARD),
                DEADLIFT to (60.0 to PerceivedEffort.MEDIUM),
                BENCH_PRESS to (35.0 to PerceivedEffort.HARD),
                OVERHEAD_PRESS to (20.0 to PerceivedEffort.HARD),
            ),
            session(
                2026, 9, 2,
                SQUAT to (50.0 to PerceivedEffort.HARD),
                DEADLIFT to (60.0 to PerceivedEffort.EASY),
                BENCH_PRESS to (35.0 to PerceivedEffort.MEDIUM),
                OVERHEAD_PRESS to (20.0 to PerceivedEffort.MEDIUM),
            ),
            session(
                2026, 9, 9,
                SQUAT to (50.0 to PerceivedEffort.MEDIUM),
                DEADLIFT to (65.0 to PerceivedEffort.HARD),
                BENCH_PRESS to (35.0 to PerceivedEffort.MEDIUM),
                OVERHEAD_PRESS to (20.0 to PerceivedEffort.MEDIUM),
            ),
        ),
    )

    /** All plans bundled with the app. */
    val all: List<PreloadedTrainingPlan> = listOf(fullBody)

    private fun session(
        year: Int,
        month: Int,
        day: Int,
        vararg performances: Pair<String, Pair<Double, PerceivedEffort>>,
    ): PreloadedSession = PreloadedSession(
        completedOn = LocalDate.of(year, month, day),
        performances = performances.associate { (name, weightAndEffort) ->
            val (weightKg, perceivedEffort) = weightAndEffort
            name to PreloadedPerformance(weightKg, perceivedEffort)
        },
    )
}
