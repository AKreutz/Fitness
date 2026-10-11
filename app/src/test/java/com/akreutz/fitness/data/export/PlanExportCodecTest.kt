package com.akreutz.fitness.data.export

import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.ExerciseWithPerformanceHistory
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.WeightStack
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.model.WorkoutWithExercises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

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
