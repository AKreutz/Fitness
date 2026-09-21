package com.akreutz.fitness.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.akreutz.fitness.data.model.Exercise
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.Workout
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Whether the plan being edited is still loading, was deleted out from under the editor, or loaded. */
sealed interface PlanEditorUiState {
    data object Loading : PlanEditorUiState
    data object NotFound : PlanEditorUiState
    data class Loaded(val plan: TrainingPlanWithWorkouts) : PlanEditorUiState
}

/**
 * Drives the plan editor: unlike [com.akreutz.fitness.ui.home.OnboardingViewModel], which drafts
 * a whole plan in memory before saving it at once, every action here writes straight through to
 * the database via [TrainingPlanRepository], since [trainingPlanId] already exists and other
 * screens observe it live.
 */
class PlanEditorViewModel(
    private val repository: TrainingPlanRepository,
    private val trainingPlanId: Long,
) : ViewModel() {

    val uiState: StateFlow<PlanEditorUiState> = repository.observeTrainingPlan(trainingPlanId)
        .map { plan -> if (plan == null) PlanEditorUiState.NotFound else PlanEditorUiState.Loaded(plan) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = PlanEditorUiState.Loading,
        )

    fun renameWorkout(workout: Workout, name: String) {
        viewModelScope.launch { repository.renameWorkout(workout, name) }
    }

    fun addWorkout(name: String) {
        viewModelScope.launch { repository.addWorkout(trainingPlanId, name) }
    }

    fun deleteWorkout(workout: Workout) {
        viewModelScope.launch { repository.deleteWorkout(workout) }
    }

    fun moveWorkout(workouts: List<Workout>, fromIndex: Int, toIndex: Int) {
        val reordered = workouts.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        viewModelScope.launch {
            repository.reorderWorkouts(trainingPlanId, reordered.map { it.id })
        }
    }

    fun addExercise(
        workoutId: Long,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
        restSeconds: Int,
    ) {
        viewModelScope.launch {
            repository.addExercise(
                workoutId,
                name,
                type,
                sets,
                reps,
                weightKg,
                weightIncrementKg,
                restSeconds,
            )
        }
    }

    fun updateExercise(
        exercise: Exercise,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
        restSeconds: Int,
    ) {
        viewModelScope.launch {
            repository.updateExercise(
                exercise,
                name,
                type,
                sets,
                reps,
                weightKg,
                weightIncrementKg,
                restSeconds,
            )
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch { repository.deleteExercise(exercise) }
    }

    fun moveExercise(exercises: List<Exercise>, fromIndex: Int, toIndex: Int) {
        val reordered = exercises.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        viewModelScope.launch {
            repository.reorderExercises(reordered.first().workoutId, reordered.map { it.id })
        }
    }
}

class PlanEditorViewModelFactory(
    private val repository: TrainingPlanRepository,
    private val trainingPlanId: Long,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(PlanEditorViewModel::class.java))
        return PlanEditorViewModel(repository, trainingPlanId) as T
    }
}
