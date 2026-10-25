package com.akreutz.fitness.data.export

import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.WeightStack
import com.akreutz.fitness.data.model.WorkoutSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Thrown by [PlanExportCodec.decode] when a file isn't a valid training-plan export. */
class InvalidPlanExportException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * The portable, self-contained file format for a single training plan: plan -> workouts ->
 * exercises, plus each workout's completed sessions and each exercise's performance records.
 * It carries no ids (history is nested under the workout/exercise it belongs to), so importing
 * always creates fresh entities. Timestamps are epoch milliseconds. Version 1 files had no
 * history; the history fields default to empty so they still import.
 * A cable exercise's [WeightStack] is embedded by value ([PlanExportExerciseDto.weightStack])
 * rather than by id, since ids mean nothing on another device.
 */
@Serializable
data class PlanExportDto(
    val formatVersion: Int = PlanExportCodec.FORMAT_VERSION,
    val name: String,
    val workouts: List<PlanExportWorkoutDto>,
)

@Serializable
data class PlanExportWorkoutDto(
    val name: String,
    val exercises: List<PlanExportExerciseDto>,
    val sessions: List<PlanExportSessionDto> = emptyList(),
)

@Serializable
data class PlanExportSessionDto(
    val startedAt: Long,
    val completedAt: Long,
    val durationSeconds: Long,
)

@Serializable
data class PlanExportPerformanceDto(
    val completedAt: Long,
    val weightKg: Double,
    val perceivedEffort: String,
)

@Serializable
data class PlanExportExerciseDto(
    val name: String,
    val type: String,
    val sets: Int,
    val reps: List<Int>,
    val weightKg: Double,
    val weightIncrementKg: Double,
    val restSeconds: Int,
    val barWeightKg: Double? = null,
    val weightStack: PlanExportWeightStackDto? = null,
    val history: List<PlanExportPerformanceDto> = emptyList(),
)

@Serializable
data class PlanExportWeightStackDto(
    val name: String,
    val levelsKg: List<Double>,
)

object PlanExportCodec {
    const val FORMAT_VERSION = 2

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * Builds the export for [plan]; [weightStacks] must include every stack its exercises use,
     * and [sessionsByWorkoutId] each workout's completed sessions (keyed by workout id).
     */
    fun encode(
        plan: TrainingPlanWithWorkouts,
        weightStacks: List<WeightStack>,
        sessionsByWorkoutId: Map<String, List<WorkoutSession>> = emptyMap(),
    ): String {
        val stacksById = weightStacks.associateBy { it.id }
        val dto = PlanExportDto(
            name = plan.trainingPlan.name,
            workouts = plan.workouts
                .sortedBy { it.workout.position }
                .map { workoutWithExercises ->
                    PlanExportWorkoutDto(
                        name = workoutWithExercises.workout.name,
                        sessions = sessionsByWorkoutId[workoutWithExercises.workout.id].orEmpty()
                            .sortedBy { it.completedAt }
                            .map {
                                PlanExportSessionDto(
                                    startedAt = it.startedAt.toEpochMilli(),
                                    completedAt = it.completedAt.toEpochMilli(),
                                    durationSeconds = it.durationSeconds,
                                )
                            },
                        exercises = workoutWithExercises.exercises
                            .sortedBy { it.exercise.position }
                            .map { withHistory ->
                                val exercise = withHistory.exercise
                                PlanExportExerciseDto(
                                    name = exercise.name,
                                    type = exercise.type.name,
                                    sets = exercise.sets,
                                    reps = exercise.reps,
                                    weightKg = exercise.weightKg,
                                    weightIncrementKg = exercise.weightIncrementKg,
                                    restSeconds = exercise.restSeconds,
                                    barWeightKg = exercise.barWeightKg,
                                    weightStack = exercise.weightStackId
                                        ?.let { stacksById[it] }
                                        ?.let { PlanExportWeightStackDto(it.name, it.levelsKg) },
                                    history = withHistory.records
                                        .sortedBy { it.completedAt }
                                        .map {
                                            PlanExportPerformanceDto(
                                                completedAt = it.completedAt.toEpochMilli(),
                                                weightKg = it.weightKg,
                                                perceivedEffort = it.perceivedEffort.name,
                                            )
                                        },
                                )
                            },
                    )
                },
        )
        return json.encodeToString(PlanExportDto.serializer(), dto)
    }

    /** Parses and validates [text], throwing [InvalidPlanExportException] if it isn't usable. */
    fun decode(text: String): PlanExportDto {
        val dto = try {
            json.decodeFromString(PlanExportDto.serializer(), text)
        } catch (e: SerializationException) {
            throw InvalidPlanExportException("This file isn't a training plan export.", e)
        } catch (e: IllegalArgumentException) {
            throw InvalidPlanExportException("This file isn't a training plan export.", e)
        }
        if (dto.formatVersion > FORMAT_VERSION) {
            throw InvalidPlanExportException(
                "This plan was exported by a newer version of the app.",
            )
        }
        if (dto.name.isBlank()) {
            throw InvalidPlanExportException("The plan has no name.")
        }
        dto.workouts.forEach { workout ->
            workout.exercises.forEach { exercise ->
                if (ExerciseType.entries.none { it.name == exercise.type }) {
                    throw InvalidPlanExportException(
                        "Unknown exercise type \"${exercise.type}\" in \"${exercise.name}\".",
                    )
                }
                if (exercise.sets < 1 || exercise.reps.isEmpty()) {
                    throw InvalidPlanExportException("\"${exercise.name}\" has no sets or reps.")
                }
                exercise.history.forEach { record ->
                    if (PerceivedEffort.entries.none { it.name == record.perceivedEffort }) {
                        throw InvalidPlanExportException(
                            "Unknown effort \"${record.perceivedEffort}\" in \"${exercise.name}\".",
                        )
                    }
                }
            }
        }
        return dto
    }
}
