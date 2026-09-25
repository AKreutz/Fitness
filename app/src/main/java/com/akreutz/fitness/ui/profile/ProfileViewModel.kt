package com.akreutz.fitness.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Whether the user's training plans are still loading, or loaded (there's always at least one). */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    /**
     * [activePlanId] is the plan the user is currently associated with, or `null` if none is
     * (e.g. right after deleting the active one). [completedSessionCounts] maps each plan's id to
     * how many workout sessions have been completed under it.
     */
    data class Loaded(
        val plans: List<TrainingPlanWithWorkouts>,
        val activePlanId: String?,
        val completedSessionCounts: Map<String, Int>,
    ) : ProfileUiState
}

class ProfileViewModel(private val repository: TrainingPlanRepository) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.observeTrainingPlans(),
        repository.observeActiveTrainingPlan().map { it?.trainingPlan?.id },
        repository.observeWorkoutSessions(),
    ) { plans, activePlanId, sessions ->
        val completedSessionCounts = sessions
            .groupingBy { it.workout.trainingPlanId }
            .eachCount()
        ProfileUiState.Loaded(
            plans = plans,
            activePlanId = activePlanId,
            completedSessionCounts = completedSessionCounts,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ProfileUiState.Loading,
        )

    /** Switches the active plan to the one with [id]. */
    fun selectPlan(id: String) {
        viewModelScope.launch {
            repository.setActiveTrainingPlan(id)
        }
    }

    /** Deletes [plan] together with its workouts and their logged history. */
    fun deletePlan(plan: TrainingPlan) {
        viewModelScope.launch {
            repository.deleteTrainingPlan(plan)
        }
    }
}

class ProfileViewModelFactory(
    private val repository: TrainingPlanRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
        return ProfileViewModel(repository) as T
    }
}
