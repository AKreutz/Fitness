package com.akreutz.fitness.ui.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.akreutz.fitness.data.model.WorkoutSession
import com.akreutz.fitness.data.model.WorkoutSessionWithWorkout
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Whether the completed-workout history is still loading, empty, or loaded. */
sealed interface WorkoutHistoryUiState {
    data object Loading : WorkoutHistoryUiState
    data object Empty : WorkoutHistoryUiState
    data class Loaded(val sessions: List<WorkoutSessionWithWorkout>) : WorkoutHistoryUiState
}

class WorkoutsViewModel(private val repository: TrainingPlanRepository) : ViewModel() {

    /**
     * Completed sessions, restricted to those logged against the *active* training plan's
     * workouts — a session for a plan the user has since switched away from (or deleted) isn't
     * shown here.
     */
    val workoutHistory: StateFlow<WorkoutHistoryUiState> = combine(
        repository.observeWorkoutSessions(),
        repository.observeActiveTrainingPlan(),
    ) { sessions, activePlan ->
        val activeWorkoutIds = activePlan?.workouts?.map { it.workout.id }?.toSet().orEmpty()
        val sessionsForActivePlan = sessions.filter { it.workout.id in activeWorkoutIds }
        if (sessionsForActivePlan.isEmpty()) {
            WorkoutHistoryUiState.Empty
        } else {
            WorkoutHistoryUiState.Loaded(sessionsForActivePlan)
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = WorkoutHistoryUiState.Loading,
        )

    /**
     * Deletes [session] from the completed-workout history, if it's still the active training
     * plan's most recently completed session (see
     * [TrainingPlanRepository.deleteWorkoutSession]) — the only one [WorkoutsScreen] ever offers
     * this for.
     */
    fun deleteWorkoutSession(session: WorkoutSession) {
        viewModelScope.launch {
            val activePlanId = repository.observeActiveTrainingPlan().first()?.trainingPlan?.id
                ?: return@launch
            repository.deleteWorkoutSession(session, activePlanId)
        }
    }
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
