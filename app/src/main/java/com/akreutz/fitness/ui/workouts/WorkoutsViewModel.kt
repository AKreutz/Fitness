package com.akreutz.fitness.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.akreutz.fitness.data.model.WorkoutSessionWithWorkout
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Whether the completed-workout history is still loading, empty, or loaded. */
sealed interface WorkoutHistoryUiState {
    data object Loading : WorkoutHistoryUiState
    data object Empty : WorkoutHistoryUiState
    data class Loaded(val sessions: List<WorkoutSessionWithWorkout>) : WorkoutHistoryUiState
}

class WorkoutsViewModel(repository: TrainingPlanRepository) : ViewModel() {

    val workoutHistory: StateFlow<WorkoutHistoryUiState> = repository.observeWorkoutSessions()
        .map { sessions ->
            if (sessions.isEmpty()) {
                WorkoutHistoryUiState.Empty
            } else {
                WorkoutHistoryUiState.Loaded(sessions)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = WorkoutHistoryUiState.Loading,
        )
}

class WorkoutsViewModelFactory(
    private val repository: TrainingPlanRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(WorkoutsViewModel::class.java))
        return WorkoutsViewModel(repository) as T
    }
}
