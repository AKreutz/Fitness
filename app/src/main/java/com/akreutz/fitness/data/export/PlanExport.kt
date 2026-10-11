package com.akreutz.fitness.data.export

import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.WeightStack
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Thrown by [PlanExportCodec.decode] when a file isn't a valid training-plan export. */
class InvalidPlanExportException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * The portable, self-contained file format for a single training plan: plan -> workouts ->
 * exercises, with no ids, timestamps or history, so importing always creates fresh entities.
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
    val weightStack: PlanExportWeightStackDto? = null,
)

@Serializable
data class PlanExportWeightStackDto(
    val name: String,
    val levelsKg: List<Double>,
)

object PlanExportCodec {
    const val FORMAT_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /** Builds the export for [plan]; [weightStacks] must include every stack its exercises use. */
    fun encode(plan: TrainingPlanWithWorkouts, weightStacks: List<WeightStack>): String {
        val stacksById = weightStacks.associateBy { it.id }
        val dto = PlanExportDto(
            name = plan.trainingPlan.name,
            workouts = plan.workouts
                .sortedBy { it.workout.position }
                .map { workoutWithExercises ->
                    PlanExportWorkoutDto(
                        name = workoutWithExercises.workout.name,
                        exercises = workoutWithExercises.exercises
                            .map { it.exercise }
                            .sortedBy { it.position }
                            .map { exercise ->
                                PlanExportExerciseDto(
                                    name = exercise.name,
                                    type = exercise.type.name,
                                    sets = exercise.sets,
                                    reps = exercise.reps,
                                    weightKg = exercise.weightKg,
                                    weightIncrementKg = exercise.weightIncrementKg,
                                    restSeconds = exercise.restSeconds,
                                    weightStack = exercise.weightStackId
                                        ?.let { stacksById[it] }
                                        ?.let { PlanExportWeightStackDto(it.name, it.levelsKg) },
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
            }
        }
        return dto
    }
}
