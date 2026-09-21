package com.akreutz.fitness.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.fitness.data.model.DraftExercise
import com.akreutz.fitness.data.model.DraftWorkout
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.launch

/** Which of the two onboarding screens is currently shown. */
enum class OnboardingStep {
    NAME_PLAN,
    ADD_EXERCISES,
}

/**
 * Drives the mandatory first-run onboarding flow: naming the plan and its workouts, then
 * optionally adding exercises to them. Everything is held here in memory — nothing is written to
 * the database until [finish] is called, so navigating back from the exercises step to the
 * naming step needs no database changes to undo.
 */
class OnboardingViewModel(private val repository: TrainingPlanRepository) : ViewModel() {

    var step by mutableStateOf(OnboardingStep.NAME_PLAN)
        private set

    var planName by mutableStateOf("")
        private set

    var workouts by mutableStateOf(listOf(DraftWorkout(name = "")))
        private set

    fun updatePlanName(name: String) {
        planName = name
    }

    fun updateWorkoutName(index: Int, name: String) {
        workouts = workouts.toMutableList().apply { this[index] = this[index].copy(name = name) }
    }

    fun addWorkout() {
        workouts = workouts + DraftWorkout(name = "")
    }

    fun removeWorkout(index: Int) {
        workouts = workouts.toMutableList().apply { removeAt(index) }
    }

    /** Moves from the plan-naming step to the add-exercises step. */
    fun proceedToAddExercises() {
        step = OnboardingStep.ADD_EXERCISES
    }

    /** Returns from the add-exercises step to the plan-naming step, keeping all drafted data. */
    fun backToNamePlan() {
        step = OnboardingStep.NAME_PLAN
    }

    fun addExercise(
        workoutIndex: Int,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
        restSeconds: Int,
    ) {
        workouts = workouts.toMutableList().apply {
            val workout = this[workoutIndex]
            this[workoutIndex] = workout.copy(
                exercises = workout.exercises + DraftExercise(
                    name = name,
                    type = type,
                    sets = sets,
                    reps = reps,
                    weightKg = weightKg,
                    weightIncrementKg = weightIncrementKg,
                    restSeconds = restSeconds,
                ),
            )
        }
    }

    fun updateExercise(
        workoutIndex: Int,
        exerciseIndex: Int,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
        restSeconds: Int,
    ) {
        workouts = workouts.toMutableList().apply {
            val workout = this[workoutIndex]
            this[workoutIndex] = workout.copy(
                exercises = workout.exercises.toMutableList().apply {
                    this[exerciseIndex] = DraftExercise(
                        name = name,
                        type = type,
                        sets = sets,
                        reps = reps,
                        weightKg = weightKg,
                        weightIncrementKg = weightIncrementKg,
                        restSeconds = restSeconds,
                    )
                },
            )
        }
    }

    /** Moves the exercise at [fromIndex] to [toIndex] within the workout at [workoutIndex]. */
    fun moveExercise(workoutIndex: Int, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        workouts = workouts.toMutableList().apply {
            val workout = this[workoutIndex]
            this[workoutIndex] = workout.copy(
                exercises = workout.exercises.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                },
            )
        }
    }

    fun removeExercise(workoutIndex: Int, exerciseIndex: Int) {
        workouts = workouts.toMutableList().apply {
            val workout = this[workoutIndex]
            this[workoutIndex] = workout.copy(
                exercises = workout.exercises.toMutableList().apply { removeAt(exerciseIndex) },
            )
        }
    }

    /** Saves the drafted plan, its workouts, and their exercises, and marks it as active. */
    fun finish() {
        val trimmedName = planName.trim()
        if (trimmedName.isEmpty()) return
        val trimmedWorkouts = workouts
            .map { it.copy(name = it.name.trim()) }
            .filter { it.name.isNotEmpty() }

        viewModelScope.launch {
            repository.createTrainingPlan(trimmedName, trimmedWorkouts)
        }
    }
}
