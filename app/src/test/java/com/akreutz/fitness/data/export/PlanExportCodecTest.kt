package com.akreutz.fitness.data.export

import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.ExercisePerformanceRecord
import com.akreutz.fitness.data.model.ExerciseWithPerformanceHistory
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.WeightStack
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.model.WorkoutSession
import com.akreutz.fitness.data.model.WorkoutWithExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class PlanExportCodecTest {

    private val stack = WeightStack(name = "Gym A", levelsKg = listOf(5.0, 10.0, 15.0))

    private fun plan(): TrainingPlanWithWorkouts {
        val trainingPlan = TrainingPlan(name = "Push/Pull")
        val workout = Workout(trainingPlanId = trainingPlan.id, name = "Push", position = 0)
        fun exercise(name: String, position: Int, type: ExerciseType, stackId: String? = null) =
            ExerciseWithPerformanceHistory(
                Exercise(
                    workoutId = workout.id,
                    name = name,
                    type = type,
                    sets = 3,
                    reps = listOf(8, 10, 12),
                    weightKg = 20.0,
                    weightIncrementKg = if (type == ExerciseType.CABLE) 0.0 else 2.5,
                    weightStackId = stackId,
                    position = position,
                ),
                records = emptyList(),
            )
        return TrainingPlanWithWorkouts(
            trainingPlan,
            // Deliberately out of order: encode must sort by position.
            listOf(
                WorkoutWithExercises(
                    workout,
                    listOf(
                        exercise("Fly", 1, ExerciseType.CABLE, stack.id),
                        exercise("Bench", 0, ExerciseType.FREE_WEIGHTS),
                    ),
                ),
            ),
        )
    }

    @Test
    fun roundTripKeepsStructureOrderAndEmbedsWeightStack() {
        val decoded = PlanExportCodec.decode(PlanExportCodec.encode(plan(), listOf(stack)))

        assertEquals("Push/Pull", decoded.name)
        val exercises = decoded.workouts.single().exercises
        assertEquals(listOf("Bench", "Fly"), exercises.map { it.name })
        assertNull(exercises[0].weightStack)
        assertEquals(PlanExportWeightStackDto("Gym A", listOf(5.0, 10.0, 15.0)), exercises[1].weightStack)
        assertEquals(listOf(8, 10, 12), exercises[0].reps)
    }

    @Test
    fun roundTripKeepsSessionsAndPerformanceHistory() {
        val base = plan()
        val workout = base.workouts.single()
        val bench = workout.exercises.first { it.exercise.name == "Bench" }
        val withHistory = base.copy(
            workouts = listOf(
                workout.copy(
                    exercises = workout.exercises.map {
                        if (it !== bench) it else it.copy(
                            records = listOf(
                                ExercisePerformanceRecord(
                                    exerciseId = it.exercise.id,
                                    completedAt = Instant.ofEpochMilli(2_000),
                                    weightKg = 22.5,
                                    perceivedEffort = PerceivedEffort.HARD,
                                ),
                                ExercisePerformanceRecord(
                                    exerciseId = it.exercise.id,
                                    completedAt = Instant.ofEpochMilli(1_000),
                                    weightKg = 20.0,
                                    perceivedEffort = PerceivedEffort.EASY,
                                ),
                            ),
                        )
                    },
                ),
            ),
        )
        val sessions = mapOf(
            workout.workout.id to listOf(
                WorkoutSession(
                    workoutId = workout.workout.id,
                    startedAt = Instant.ofEpochMilli(500),
                    completedAt = Instant.ofEpochMilli(1_000),
                    durationSeconds = 3_600,
                ),
            ),
        )

        val decoded = PlanExportCodec.decode(PlanExportCodec.encode(withHistory, listOf(stack), sessions))

        val exportedWorkout = decoded.workouts.single()
        assertEquals(
            listOf(PlanExportSessionDto(startedAt = 500, completedAt = 1_000, durationSeconds = 3_600)),
            exportedWorkout.sessions,
        )
        // Records come out oldest first.
        assertEquals(
            listOf(
                PlanExportPerformanceDto(1_000, 20.0, "EASY"),
                PlanExportPerformanceDto(2_000, 22.5, "HARD"),
            ),
            exportedWorkout.exercises.first { it.name == "Bench" }.history,
        )
    }

    @Test
    fun decodeAcceptsVersion1FilesWithoutHistory() {
        val v1 = """{"formatVersion":1,"name":"Old","workouts":[{"name":"w","exercises":[
            {"name":"e","type":"FREE_WEIGHTS","sets":3,"reps":[8],"weightKg":1.0,
             "weightIncrementKg":1.0,"restSeconds":90}]}]}"""
        val decoded = PlanExportCodec.decode(v1)
        assertEquals(emptyList<PlanExportSessionDto>(), decoded.workouts.single().sessions)
        assertEquals(emptyList<PlanExportPerformanceDto>(), decoded.workouts.single().exercises.single().history)
    }

    @Test
    fun decodeRejectsGarbage() {
        assertThrows(InvalidPlanExportException::class.java) { PlanExportCodec.decode("not json") }
        assertThrows(InvalidPlanExportException::class.java) { PlanExportCodec.decode("{}") }
    }

    @Test
    fun decodeRejectsNewerFormatAndUnknownType() {
        val newer = """{"formatVersion":99,"name":"x","workouts":[]}"""
        assertThrows(InvalidPlanExportException::class.java) { PlanExportCodec.decode(newer) }

        val badType = """{"name":"x","workouts":[{"name":"w","exercises":[
            {"name":"e","type":"KETTLEBELL","sets":3,"reps":[8],"weightKg":1.0,
             "weightIncrementKg":1.0,"restSeconds":90}]}]}"""
        assertThrows(InvalidPlanExportException::class.java) { PlanExportCodec.decode(badType) }
    }
}
