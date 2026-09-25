package com.akreutz.fitness.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.WorkoutWithExercises
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Whether the user's active training plan is still loading, missing, or loaded. */
sealed interface ActiveTrainingPlanUiState {
    data object Loading : ActiveTrainingPlanUiState
    data object NoPlan : ActiveTrainingPlanUiState

    /** [workoutsNextFirst] holds the plan's workouts reordered so the one up next comes first. */
    data class Loaded(
        val trainingPlan: TrainingPlanWithWorkouts,
        val workoutsNextFirst: List<WorkoutWithExercises>,
    ) : ActiveTrainingPlanUiState
}

class HomeViewModel(private val repository: TrainingPlanRepository) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeTrainingPlan: StateFlow<ActiveTrainingPlanUiState> =
        repository.observeActiveTrainingPlan()
            .flatMapLatest { plan ->
                if (plan == null) {
                    flowOf(ActiveTrainingPlanUiState.NoPlan)
                } else {
                    repository.observeWorkoutsNextFirst(flowOf(plan))
                        .map { ordered -> ActiveTrainingPlanUiState.Loaded(plan, ordered) }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = ActiveTrainingPlanUiState.Loading,
            )

    private val _startWorkoutEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)

    /** Emits the id of a [com.akreutz.fitness.data.model.Workout] each time one is started. */
    val startWorkoutEvents: SharedFlow<String> = _startWorkoutEvents

    /**
     * Starts the next workout in the active plan's rotation (see
     * [TrainingPlanRepository.nextWorkout]) and emits it via [startWorkoutEvents]. No-ops if the
     * plan isn't loaded yet or has no workouts. The rotation itself only advances once that
     * workout is actually finished, not just started (see
     * [com.akreutz.fitness.ui.session.WorkoutSessionViewModel]), so starting the same workout
     * again (e.g. after cancelling) keeps offering it.
     */
    fun startNextWorkout() {
        val state = activeTrainingPlan.value
        if (state !is ActiveTrainingPlanUiState.Loaded) return
        viewModelScope.launch {
            val workout = repository.nextWorkout(state.trainingPlan) ?: return@launch
            _startWorkoutEvents.emit(workout.id)
        }
    }

    /** Adds a new exercise to the workout with [workoutId]. */
    fun addExercise(
        workoutId: String,
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
                workoutId = workoutId,
                name = name,
                type = type,
                sets = sets,
                reps = reps,
                weightKg = weightKg,
                weightIncrementKg = weightIncrementKg,
                restSeconds = restSeconds,
            )
        }
    }
}
