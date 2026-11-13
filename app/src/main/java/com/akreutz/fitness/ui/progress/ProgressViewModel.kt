package com.akreutz.fitness.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.akreutz.fitness.data.model.ExerciseWithPerformanceHistory
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.TrainingPlanWithWorkouts
import com.akreutz.fitness.data.model.repsDoneAt
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

/** One recorded weight for an exercise, at the point in time it was logged. */
data class WeightDataPoint(
    val completedAt: Instant,
    val weightKg: Double,
    val perceivedEffort: PerceivedEffort,
)

/** An exercise's logged weight history, ready to chart, in chronological order. */
data class ExerciseProgress(
    val exerciseId: String,
    val exerciseName: String,
    val dataPoints: List<VolumeDataPoint>,
)

/**
 * One performance's training volume: weight × reps per set × number of sets, at the point in time
 * it was logged.
 */
data class VolumeDataPoint(
    val completedAt: Instant,
    val volumeKg: Double,
    val perceivedEffort: PerceivedEffort,
)

/** One workout's exercises, each with its own chartable history. */
data class WorkoutProgress(
    val workoutId: String,
    val workoutName: String,
    val exercises: List<ExerciseProgress>,
)

/**
 * One session's overall progress: the sum, across every exercise that was performed at
 * [completedAt], of that exercise's percentage weight increase compared to its own first-ever
 * recorded weight. E.g. if two exercises were each performed at 10% above their starting weight
 * in the same session, this is 20.0. [averageEffort] is the average [PerceivedEffort] across
 * those same exercises (rounded up on a tie), for showing how hard the session felt without
 * folding it into [summedPercentIncrease] itself — so a session's position on the chart always
 * reflects progress alone, never how it felt.
 */
data class SummaryDataPoint(
    val completedAt: Instant,
    val summedPercentIncrease: Double,
    val averageEffort: PerceivedEffort,
)

/** Whether the selected training plan's progress is still loading, absent, or loaded. */
sealed interface ProgressUiState {
    data object Loading : ProgressUiState
    data object NoPlan : ProgressUiState
    data class Loaded(
        val workouts: List<WorkoutProgress>,
        val summary: List<SummaryDataPoint>,
    ) : ProgressUiState
}

/**
 * Feeds the progress screen: for the user's active [com.akreutz.fitness.data.model.TrainingPlan],
 * every workout's exercises turned into a chartable weight-over-time series (see
 * [ExerciseWithPerformanceHistory.performanceHistory]), grouped and ordered the same way the plan
 * itself is.
 */
class ProgressViewModel(repository: TrainingPlanRepository) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = repository.observeActiveTrainingPlan()
        .map { plan -> plan.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ProgressUiState.Loading,
        )

    private fun TrainingPlanWithWorkouts?.toUiState(): ProgressUiState {
        if (this == null) return ProgressUiState.NoPlan
        val allExercises = workouts.flatMap { it.exercises }
        return ProgressUiState.Loaded(
            workouts = workouts.map { workout ->
                WorkoutProgress(
                    workoutId = workout.workout.id,
                    workoutName = workout.workout.name,
                    exercises = workout.exercises.map { it.toExerciseProgress() },
                )
            },
            summary = allExercises.toSummary(),
        )
    }

    private fun ExerciseWithPerformanceHistory.toExerciseProgress(): ExerciseProgress =
        ExerciseProgress(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            dataPoints = volumeHistory(),
        )

    /**
     * Each performance's volume (weight × reps per set × sets), in chronological order. Reps aren't
     * stored, so they're replayed per session (see [repsDoneAt]); the set count is the exercise's
     * current one.
     */
    private fun ExerciseWithPerformanceHistory.volumeHistory(): List<VolumeDataPoint> =
        performanceHistory.entries
            .sortedBy { it.key }
            .map { (completedAt, entry) ->
                val reps = repsDoneAt(completedAt) ?: 0
                VolumeDataPoint(completedAt, entry.weightKg * reps * exercise.sets, entry.perceivedEffort)
            }

    /**
     * For every session timestamp any of [this] was performed at, the sum across those exercises
     * of the percentage weight increase compared to each exercise's own first-ever recorded
     * weight, together with their average [PerceivedEffort] (see [SummaryDataPoint]), in
     * chronological order. An exercise with no recorded weight yet (nothing to compare against)
     * is simply excluded from every sum.
     */
    private fun List<ExerciseWithPerformanceHistory>.toSummary(): List<SummaryDataPoint> {
        data class SessionEntry(val percentIncrease: Double, val perceivedEffort: PerceivedEffort)

        val entriesByTimestamp = mutableMapOf<Instant, MutableList<SessionEntry>>()
        forEach { exercise ->
            val history = exercise.volumeHistory()
            val startingVolume = history.firstOrNull()?.volumeKg?.takeIf { it > 0.0 } ?: return@forEach
            history.forEach { point ->
                val percentIncrease = (point.volumeKg - startingVolume) / startingVolume * 100.0
                entriesByTimestamp.getOrPut(point.completedAt) { mutableListOf() } +=
                    SessionEntry(percentIncrease, point.perceivedEffort)
            }
        }
        return entriesByTimestamp.entries
            .sortedBy { it.key }
            .map { (completedAt, entries) ->
                SummaryDataPoint(
                    completedAt = completedAt,
                    summedPercentIncrease = entries.sumOf { it.percentIncrease },
                    averageEffort = entries.map { it.perceivedEffort }.average(),
                )
            }
    }
}

/**
 * The average [PerceivedEffort] across [this] (ordinal-mean, rounded up on a tie — e.g. an even
 * split between Easy and Hard rounds up to Medium rather than down). [this] must not be empty.
 */
private fun List<PerceivedEffort>.average(): PerceivedEffort {
    val averageOrdinal = sumOf { it.ordinal } / size.toDouble()
    return PerceivedEffort.entries[Math.round(averageOrdinal).toInt()]
}

class ProgressViewModelFactory(
    private val repository: TrainingPlanRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(ProgressViewModel::class.java))
        return ProgressViewModel(repository) as T
    }
}
