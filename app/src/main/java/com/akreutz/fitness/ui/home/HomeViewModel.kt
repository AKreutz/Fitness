package com.akreutz.fitness.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Whether the user's active training plan is still loading, missing, or loaded. */
sealed interface ActiveTrainingPlanUiState {
    data object Loading : ActiveTrainingPlanUiState
    data object NoPlan : ActiveTrainingPlanUiState
    data class Loaded(val trainingPlan: TrainingPlanWithWorkouts) : ActiveTrainingPlanUiState
}

class HomeViewModel(private val repository: TrainingPlanRepository) : ViewModel() {

    val activeTrainingPlan: StateFlow<ActiveTrainingPlanUiState> =
        repository.observeActiveTrainingPlan()
            .map { plan ->
                if (plan == null) {
                    ActiveTrainingPlanUiState.NoPlan
                } else {
                    ActiveTrainingPlanUiState.Loaded(plan)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = ActiveTrainingPlanUiState.Loading,
            )

    /** Adds a new exercise to the workout with [workoutId]. */
    fun addExercise(
        workoutId: Long,
        name: String,
        type: ExerciseType,
        sets: Int,
        reps: List<Int>,
        weightKg: Double,
        weightIncrementKg: Double,
    ) {
        viewModelScope.launch {
            repository.addExercise(
                workoutId = workoutId,
                name = name,
                type = type,
                sets = sets,
                reps = reps,
                weightKg = weightKg,
                weightIncrementKg = weightIncrementKg,
            )
        }
    }
}
