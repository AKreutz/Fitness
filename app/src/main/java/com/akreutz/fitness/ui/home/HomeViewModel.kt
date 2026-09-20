package com.akreutz.fitness.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
}
