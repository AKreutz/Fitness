package com.akreutz.fitness.data.sync.drive

import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExercisePerformanceRecord
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.PurgedId
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.model.WorkoutSession
import com.akreutz.fitness.data.sync.FitnessSnapshot
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * The JSON wire format [com.akreutz.fitness.data.sync.drive.GoogleDriveDataSource] stores in
 * Drive's `appDataFolder`, mirroring [FitnessSnapshot]'s tables but with JSON-friendly field types
 * (`String` for [Instant], the enum's name for [ExerciseType]/[PerceivedEffort]) since
 * kotlinx.serialization doesn't handle `java.time` types or Room's own column converters natively.
 */
@Serializable
data class FitnessSnapshotDto(
    val trainingPlans: List<TrainingPlanDto>,
    val workouts: List<WorkoutDto>,
    val exercises: List<ExerciseDto>,
    val workoutSessions: List<WorkoutSessionDto>,
    val performanceRecords: List<ExercisePerformanceRecordDto>,
    val purgedIds: List<PurgedIdDto>,
) {
    fun toSnapshot(): FitnessSnapshot = FitnessSnapshot(
        trainingPlans = trainingPlans.map { it.toEntity() },
        workouts = workouts.map { it.toEntity() },
        exercises = exercises.map { it.toEntity() },
        workoutSessions = workoutSessions.map { it.toEntity() },
        performanceRecords = performanceRecords.map { it.toEntity() },
        purgedIds = purgedIds.map { it.toEntity() },
    )

    companion object {
        fun fromSnapshot(snapshot: FitnessSnapshot): FitnessSnapshotDto = FitnessSnapshotDto(
            trainingPlans = snapshot.trainingPlans.map { TrainingPlanDto.fromEntity(it) },
            workouts = snapshot.workouts.map { WorkoutDto.fromEntity(it) },
            exercises = snapshot.exercises.map { ExerciseDto.fromEntity(it) },
            workoutSessions = snapshot.workoutSessions.map { WorkoutSessionDto.fromEntity(it) },
            performanceRecords = snapshot.performanceRecords.map { ExercisePerformanceRecordDto.fromEntity(it) },
            purgedIds = snapshot.purgedIds.map { PurgedIdDto.fromEntity(it) },
        )
    }
}

@Serializable
data class TrainingPlanDto(
    val id: String,
    val name: String,
    val updatedAt: Long,
) {
    fun toEntity(): TrainingPlan = TrainingPlan(
        id = id,
        name = name,
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    companion object {
        fun fromEntity(entity: TrainingPlan): TrainingPlanDto = TrainingPlanDto(
            id = entity.id,
            name = entity.name,
            updatedAt = entity.updatedAt.toEpochMilli(),
        )
    }
}

@Serializable
data class WorkoutDto(
    val id: String,
    val trainingPlanId: String,
    val name: String,
    val position: Int,
    val updatedAt: Long,
) {
    fun toEntity(): Workout = Workout(
        id = id,
        trainingPlanId = trainingPlanId,
        name = name,
        position = position,
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    companion object {
        fun fromEntity(entity: Workout): WorkoutDto = WorkoutDto(
            id = entity.id,
            trainingPlanId = entity.trainingPlanId,
            name = entity.name,
            position = entity.position,
            updatedAt = entity.updatedAt.toEpochMilli(),
        )
    }
}

@Serializable
data class ExerciseDto(
    val id: String,
    val workoutId: String,
    val name: String,
    val type: String,
    val sets: Int,
    val reps: List<Int>,
    val weightKg: Double,
    val weightIncrementKg: Double,
    val restSeconds: Int,
    val position: Int,
    val updatedAt: Long,
) {
    fun toEntity(): Exercise = Exercise(
        id = id,
        workoutId = workoutId,
        name = name,
        type = ExerciseType.valueOf(type),
        sets = sets,
        reps = reps,
        weightKg = weightKg,
        weightIncrementKg = weightIncrementKg,
        restSeconds = restSeconds,
        position = position,
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    companion object {
        fun fromEntity(entity: Exercise): ExerciseDto = ExerciseDto(
            id = entity.id,
            workoutId = entity.workoutId,
            name = entity.name,
            type = entity.type.name,
            sets = entity.sets,
            reps = entity.reps,
            weightKg = entity.weightKg,
            weightIncrementKg = entity.weightIncrementKg,
            restSeconds = entity.restSeconds,
            position = entity.position,
            updatedAt = entity.updatedAt.toEpochMilli(),
        )
    }
}

@Serializable
data class WorkoutSessionDto(
    val id: String,
    val workoutId: String,
    val startedAt: Long,
    val completedAt: Long,
    val durationSeconds: Long,
    val updatedAt: Long,
) {
    fun toEntity(): WorkoutSession = WorkoutSession(
        id = id,
        workoutId = workoutId,
        startedAt = Instant.ofEpochMilli(startedAt),
        completedAt = Instant.ofEpochMilli(completedAt),
        durationSeconds = durationSeconds,
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    companion object {
        fun fromEntity(entity: WorkoutSession): WorkoutSessionDto = WorkoutSessionDto(
            id = entity.id,
            workoutId = entity.workoutId,
            startedAt = entity.startedAt.toEpochMilli(),
            completedAt = entity.completedAt.toEpochMilli(),
            durationSeconds = entity.durationSeconds,
            updatedAt = entity.updatedAt.toEpochMilli(),
        )
    }
}

@Serializable
data class ExercisePerformanceRecordDto(
    val id: String,
    val exerciseId: String,
    val completedAt: Long,
    val weightKg: Double,
    val perceivedEffort: String,
    val updatedAt: Long,
) {
    fun toEntity(): ExercisePerformanceRecord = ExercisePerformanceRecord(
        id = id,
        exerciseId = exerciseId,
        completedAt = Instant.ofEpochMilli(completedAt),
        weightKg = weightKg,
        perceivedEffort = PerceivedEffort.valueOf(perceivedEffort),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )

    companion object {
        fun fromEntity(entity: ExercisePerformanceRecord): ExercisePerformanceRecordDto = ExercisePerformanceRecordDto(
            id = entity.id,
            exerciseId = entity.exerciseId,
            completedAt = entity.completedAt.toEpochMilli(),
            weightKg = entity.weightKg,
            perceivedEffort = entity.perceivedEffort.name,
            updatedAt = entity.updatedAt.toEpochMilli(),
        )
    }
}

@Serializable
data class PurgedIdDto(
    val id: String,
    val purgedAt: Long,
) {
    fun toEntity(): PurgedId = PurgedId(id = id, purgedAt = Instant.ofEpochMilli(purgedAt))

    companion object {
        fun fromEntity(entity: PurgedId): PurgedIdDto =
            PurgedIdDto(id = entity.id, purgedAt = entity.purgedAt.toEpochMilli())
    }
}
