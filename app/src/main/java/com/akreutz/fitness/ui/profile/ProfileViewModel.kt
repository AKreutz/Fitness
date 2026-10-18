package com.akreutz.fitness.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.akreutz.fitness.data.model.TrainingPlan
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import com.akreutz.fitness.data.export.InvalidPlanExportException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** Whether the user's training plans are still loading, or loaded (there's always at least one). */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    /**
     * [activePlanId] is the plan the user is currently associated with, or `null` if none is
     * (e.g. right after deleting the active one). [completedSessionCounts] maps each plan's id to
     * how many workout sessions have been completed under it. [lastTrainedAt] maps a plan's id to
     * when its most recent session was completed; plans without sessions have no entry.
     */
    data class Loaded(
        val plans: List<TrainingPlanWithWorkouts>,
        val activePlanId: String?,
        val completedSessionCounts: Map<String, Int>,
        val lastTrainedAt: Map<String, Instant>,
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
        val lastTrainedAt = sessions
            .groupBy { it.workout.trainingPlanId }
            .mapValues { (_, planSessions) -> planSessions.maxOf { it.session.completedAt } }
        ProfileUiState.Loaded(
            plans = plans,
            activePlanId = activePlanId,
            completedSessionCounts = completedSessionCounts,
            lastTrainedAt = lastTrainedAt,
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

    private val _messages = Channel<String>(Channel.BUFFERED)

    /** One-off, user-facing results of export/import (success or why it failed), to show once. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    /** Serializes the plan with [id] and hands the JSON to [onReady], ready to be written out. */
    fun exportPlan(id: String, onReady: (json: String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportTrainingPlan(id)
            if (json == null) _messages.send("That plan no longer exists.") else onReady(json)
        }
    }

    /** Creates a new plan from [json], the contents of a file produced by [exportPlan]. */
    fun importPlan(json: String) {
        viewModelScope.launch {
            val message = try {
                repository.importTrainingPlan(json)
                "Plan imported."
            } catch (e: InvalidPlanExportException) {
                e.message ?: "Couldn't import this file."
            }
            _messages.send(message)
        }
    }

    /** Reports a failure reading or writing the chosen file. */
    fun reportFileError() {
        _messages.trySend("Couldn't read or write the file.")
    }

    /** Reports that [name] was exported successfully. */
    fun reportExported(name: String) {
        _messages.trySend("Exported \"$name\".")
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
