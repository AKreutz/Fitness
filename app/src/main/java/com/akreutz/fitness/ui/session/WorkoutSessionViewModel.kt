package com.akreutz.fitness.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.akreutz.fitness.data.model.ExercisePerformanceEntry
import com.akreutz.fitness.data.model.ExerciseType
import com.akreutz.fitness.data.model.ExerciseWithPerformanceHistory
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.WorkoutWithExercises
import com.akreutz.fitness.data.model.lastPerformanceAtCurrentWeight
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

/** A single step within an exercise: the warm-up, then one step per prescribed working set. */
private sealed interface ExerciseStep {
    data object WarmUp : ExerciseStep
    data class WorkingSet(val setIndex: Int) : ExerciseStep
}

/** Where the user is within a guided workout session. */
sealed interface WorkoutSessionUiState {
    data object Loading : WorkoutSessionUiState

    /**
     * Guiding the user through the workout's exercises in order, one set at a time. [exercise]
     * is the one currently shown; [exerciseNumber]/[totalExercises] are 1-based, for progress
     * display (e.g. "2 of 4"). Within [exercise], [currentSet] describes where the user is.
     * [nextExercise] is whichever exercise follows [exercise], or `null` if it's the workout's
     * last. [remainingExercises] are all the ones still to come after [exercise], any of which
     * can be done next instead via [WorkoutSessionViewModel.switchTo] while [currentSet] is
     * still the warm-up, as can any of the [skippedExercises] from earlier in the session.
     * [canSkip] is whether [WorkoutSessionViewModel.skipExercise] currently
     * does anything. [recoveryGoalSeconds] is [exercise]'s prescribed rest duration, for showing recovery
     * progress towards it; it's unrelated to whether the user is currently resting (see
     * [recoverySeconds]). When [exerciseToRate] is non-null, the just-finished exercise is
     * awaiting a perceived-effort rating (via [WorkoutSessionViewModel.rateExercise]) before the
     * session can move on to [exercise]. When [exerciseToOfferWeightIncrease] is non-null (only
     * possible once [exerciseToRate] is `null` again), the user just rated it "easy" effort for
     * the second time in a row and is being asked whether to raise its weight for next time (via
     * [WorkoutSessionViewModel.respondToWeightIncreaseOffer]).
     */
    data class InProgress(
        val workoutName: String,
        val exercise: ExerciseWithPerformanceHistory,
        val exerciseNumber: Int,
        val totalExercises: Int,
        val nextExercise: ExerciseWithPerformanceHistory?,
        val remainingExercises: List<ExerciseWithPerformanceHistory>,
        val skippedExercises: List<ExerciseWithPerformanceHistory>,
        val currentSet: SetProgress,
        val canSkip: Boolean,
        val recoverySeconds: Int?,
        val recoveryGoalSeconds: Int,
        val exerciseToRate: ExerciseWithPerformanceHistory? = null,
        val exerciseToOfferWeightIncrease: ExerciseWithPerformanceHistory? = null,
    ) : WorkoutSessionUiState

    /** The workout had no exercises to guide through, or was deleted mid-session. */
    data object Unavailable : WorkoutSessionUiState

    /**
     * The user has gone through every exercise and the session is done. [personalRecords] is how
     * many of its exercises were done at a heavier weight than ever before.
     */
    data class Finished(val personalRecords: Int) : WorkoutSessionUiState
}

/** Describes the set currently shown on an [WorkoutSessionUiState.InProgress] card. */
sealed interface SetProgress {
    /** The button label for advancing past this set. */
    val nextLabel: String

    /**
     * The warm-up set that starts off every exercise, before its prescribed working sets.
     * Finishing it moves straight to the first working set, with no rest in between.
     */
    data object WarmUp : SetProgress {
        override val nextLabel: String = "Start first set"
    }

    /**
     * One of the exercise's prescribed working sets. [setNumber] is 1-based. [isLastSet] is true
     * once this is the exercise's final set, at which point finishing it moves on to the next
     * exercise (or ends the workout) instead of resting for another set.
     */
    data class Working(
        val setNumber: Int,
        val totalSets: Int,
        val isLastSet: Boolean,
    ) : SetProgress {
        override val nextLabel: String = if (isLastSet) "Finish exercise" else "Finish set"
    }

    /**
     * Resting between working sets, after finishing one that wasn't the exercise's last.
     * Advancing here stops the rest timer and starts the next set.
     */
    data class Resting(val completedSet: SetProgress) : SetProgress {
        override val nextLabel: String = "Start next set"
    }
}

private const val RECOVERY_TICK_MILLIS = 1_000L

/**
 * Drives a single guided workout session: walks the user through [workoutId]'s exercises in
 * order, and within each exercise through a warm-up followed by its prescribed working sets, via
 * [advance]. Finishing the warm-up moves straight to the first working set with no rest;
 * finishing any working set but the exercise's last starts a rest timer (counting up past zero
 * once it elapses) that [advance] both stops and steps past. Finishing an exercise's last set
 * prompts for a perceived-effort rating (via [rateExercise]); rating it "easy" for the second
 * session in a row then also prompts whether to raise its weight for next time (via
 * [respondToWeightIncreaseOffer]). Once the workout's last exercise is rated (and any such offer
 * answered), persists all of the session's ratings under today's date and finishes the session.
 * When a rest period reaches its exercise's prescribed rest duration, [restTimerAlerter] is fired
 * once (not on every later tick) to notify the user that rest is over.
 */
class WorkoutSessionViewModel(
    private val repository: TrainingPlanRepository,
    private val workoutId: String,
    private val restTimerAlerter: RestTimerAlerter,
) : ViewModel() {

    /** When this session began, for computing its duration once it finishes. */
    private val startedAt = Instant.now()

    private val exerciseIndex = MutableStateFlow(0)
    private val step = MutableStateFlow<ExerciseStep>(ExerciseStep.WarmUp)
    private val resting = MutableStateFlow(false)

    /** Ids of exercises skipped via [skipExercise] and not yet gone back to. */
    private val skippedIds = MutableStateFlow<Set<String>>(emptySet())
    private val recoverySeconds = MutableStateFlow<Int?>(null)
    private var recoveryJob: Job? = null

    /** The exercise awaiting a rating, or `null` if none is currently pending one. */
    private val exerciseToRate = MutableStateFlow<ExerciseWithPerformanceHistory?>(null)

    /**
     * The exercise awaiting a response to "raise the weight for next time?", or `null` if none
     * is currently pending one.
     */
    private val exerciseToOfferWeightIncrease = MutableStateFlow<ExerciseWithPerformanceHistory?>(null)

    /**
     * Ratings collected so far this session, by exercise id, saved once the workout finishes.
     * Each entry's weight is the one actually used at rating time, so a later weight increase
     * (see [respondToWeightIncreaseOffer]) doesn't retroactively change what gets recorded.
     */
    private val collectedRatings = mutableMapOf<String, ExercisePerformanceEntry>()

    /** How many exercises were done at a new heaviest weight; set just before [readyToFinish]. */
    private var personalRecords = 0

    /** Whether every exercise has been rated and the session is ready to finish. */
    private val readyToFinish = MutableStateFlow(false)

    /** The latest exercise list, kept up to date independently of [uiState]'s own subscribers,
     * so [advance] can read the current exercise's set count without relying on [uiState] having
     * been collected. */
    private var latestExercises: List<ExerciseWithPerformanceHistory>? = null

    /**
     * The order the user has chosen for this session as exercise ids, or empty while the plan's
     * own order applies. Exercises missing from it (shouldn't happen) go last, in plan order.
     */
    private val sessionOrder = MutableStateFlow<List<String>>(emptyList())

    private val workout: Flow<WorkoutWithExercises?> =
        combine(repository.observeWorkout(workoutId), sessionOrder) { workout, order ->
            workout?.copy(exercises = workout.exercises.inSessionOrder(order))
        }

    private fun List<ExerciseWithPerformanceHistory>.inSessionOrder(
        order: List<String>,
    ): List<ExerciseWithPerformanceHistory> =
        if (order.isEmpty()) {
            this
        } else {
            sortedBy { order.indexOf(it.exercise.id).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
        }

    /** Everything about session progress except the loaded [workout] itself. */
    private data class Progress(
        val exerciseIndex: Int,
        val step: ExerciseStep,
        val resting: Boolean,
        val recoverySeconds: Int?,
        val exerciseToRate: ExerciseWithPerformanceHistory?,
        val exerciseToOfferWeightIncrease: ExerciseWithPerformanceHistory?,
        val readyToFinish: Boolean,
        val skippedIds: Set<String>,
    )

    private val setState: Flow<Pair<ExerciseStep, Boolean>> = combine(step, resting, ::Pair)
    private val prompts: Flow<Pair<ExerciseWithPerformanceHistory?, ExerciseWithPerformanceHistory?>> =
        combine(exerciseToRate, exerciseToOfferWeightIncrease, ::Pair)

    private val finishAndSkipped: Flow<Pair<Boolean, Set<String>>> = combine(readyToFinish, skippedIds, ::Pair)

    private val progress: Flow<Progress> = combine(
        exerciseIndex,
        setState,
        recoverySeconds,
        prompts,
        finishAndSkipped,
    ) { index: Int, (currentStep, isResting), seconds: Int?, (toRate, toOfferIncrease), (finished, skipped) ->
        Progress(index, currentStep, isResting, seconds, toRate, toOfferIncrease, finished, skipped)
    }

    val uiState: StateFlow<WorkoutSessionUiState> =
        combine(workout, progress) { workout: WorkoutWithExercises?, progress: Progress ->
            val exercises = workout?.exercises
            when {
                exercises.isNullOrEmpty() -> WorkoutSessionUiState.Unavailable
                progress.readyToFinish -> WorkoutSessionUiState.Finished(personalRecords)
                progress.exerciseIndex >= exercises.size -> WorkoutSessionUiState.Unavailable
                else -> {
                    val exercise = exercises[progress.exerciseIndex]
                    val setProgress = progress.step.toSetProgress(exercise.exercise.sets, progress.resting)
                    WorkoutSessionUiState.InProgress(
                        workoutName = workout.workout.name,
                        exercise = exercise,
                        exerciseNumber = progress.exerciseIndex + 1,
                        totalExercises = exercises.size,
                        nextExercise = exercises.getOrNull(progress.exerciseIndex + 1),
                        remainingExercises = exercises.drop(progress.exerciseIndex + 1),
                        skippedExercises = exercises.take(progress.exerciseIndex)
                            .filter { it.exercise.id in progress.skippedIds },
                        currentSet = setProgress,
                        canSkip = canSkipCurrentExercise(),
                        recoverySeconds = progress.recoverySeconds.takeIf { progress.resting },
                        recoveryGoalSeconds = exercise.exercise.restSeconds,
                        exerciseToRate = progress.exerciseToRate,
                        exerciseToOfferWeightIncrease = progress.exerciseToOfferWeightIncrease,
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = WorkoutSessionUiState.Loading,
        )

    init {
        workout.onEach { latestExercises = it?.exercises }.launchIn(viewModelScope)
    }

    private fun ExerciseStep.toSetProgress(totalSets: Int, isResting: Boolean): SetProgress {
        val base = when (this) {
            ExerciseStep.WarmUp -> SetProgress.WarmUp
            is ExerciseStep.WorkingSet -> SetProgress.Working(
                setNumber = setIndex + 1,
                totalSets = totalSets,
                isLastSet = setIndex == totalSets - 1,
            )
        }
        return if (isResting) SetProgress.Resting(base) else base
    }

    /**
     * Advances from wherever the user currently is:
     * - Resting: stops the timer and moves on to the next set.
     * - Warm-up: moves straight to the first working set, no rest.
     * - Mid working set, not the exercise's last: starts resting.
     * - Mid working set, the exercise's last: prompts to rate the exercise (via [rateExercise])
     *   instead of moving on directly.
     */
    fun advance() {
        if (resting.value) {
            stopRecoveryTimer()
            step.value = nextStep(step.value)
            return
        }

        val currentStep = step.value
        if (currentStep is ExerciseStep.WarmUp) {
            step.value = nextStep(currentStep)
            return
        }

        val currentSetCount = latestExercises?.getOrNull(exerciseIndex.value)?.exercise?.sets ?: 0
        val isLastSet = (currentStep as ExerciseStep.WorkingSet).setIndex == currentSetCount - 1
        if (isLastSet) {
            exerciseToRate.value = latestExercises?.getOrNull(exerciseIndex.value)
        } else {
            startRecoveryTimer()
        }
    }

    /**
     * Does the not-yet-done or skipped exercise [exerciseId] now instead of the current one,
     * which moves back to just after it. Only possible at the current exercise's warm-up, before any of its
     * sets are done, so no partial progress is lost; otherwise ignored.
     */
    fun switchTo(exerciseId: String) {
        if (step.value != ExerciseStep.WarmUp || resting.value) return
        if (exerciseToRate.value != null || exerciseToOfferWeightIncrease.value != null) return
        val ids = latestExercises?.map { it.exercise.id } ?: return
        val index = exerciseIndex.value
        val isRemaining = exerciseId in ids.drop(index + 1)
        val isSkipped = exerciseId in skippedIds.value && exerciseId in ids.take(index)
        if (!isRemaining && !isSkipped) return
        val newOrder = ids.toMutableList().apply {
            remove(exerciseId)
            add(indexOf(ids[index]), exerciseId)
        }
        // Order first, then index: in between, the current exercise is still the one shown.
        sessionOrder.value = newOrder
        exerciseIndex.value = newOrder.indexOf(exerciseId)
        skippedIds.value -= exerciseId
    }

    /**
     * Skips the current exercise at any point in it: it gets no rating and isn't recorded, and
     * the session moves on to the next exercise (or finishes, if this was the last). Ignored
     * while a rating or weight-increase prompt is pending, and for the last exercise when nothing
     * else has been done this session, since there'd be nothing to save.
     */
    fun skipExercise() {
        if (exerciseToRate.value != null || exerciseToOfferWeightIncrease.value != null) return
        if (!canSkipCurrentExercise()) return
        stopRecoveryTimer()
        latestExercises?.getOrNull(exerciseIndex.value)?.let { skippedIds.value += it.exercise.id }
        proceedAfterRating()
    }

    private fun canSkipCurrentExercise(): Boolean =
        exerciseIndex.value < (latestExercises?.lastIndex ?: -1) || collectedRatings.isNotEmpty()

    /**
     * Records [effort] as the just-finished exercise's rating. If [effort] is
     * [PerceivedEffort.EASY] and so was that exercise's most recent prior performance, pauses on
     * an offer to raise its weight for next time (via [respondToWeightIncreaseOffer]) before
     * moving on; otherwise moves on directly (see [proceedAfterRating]).
     */
    fun rateExercise(effort: PerceivedEffort) {
        val rated = exerciseToRate.value ?: return
        collectedRatings[rated.exercise.id] = ExercisePerformanceEntry(
            weightKg = rated.exercise.weightKg,
            perceivedEffort = effort,
        )
        exerciseToRate.value = null

        val lastPerceivedEffort = rated.lastPerformanceAtCurrentWeight?.perceivedEffort
        if (effort == PerceivedEffort.EASY && lastPerceivedEffort == PerceivedEffort.EASY) {
            exerciseToOfferWeightIncrease.value = rated
        } else {
            proceedAfterRating()
        }
    }

    /**
     * Responds to the "raise the weight for next time?" offer: if [newWeightKg] is non-null,
     * persists it as the exercise's new prescribed weight (see
     * [TrainingPlanRepository.setExerciseWeight]) before moving on either way (see
     * [proceedAfterRating]). For [ExerciseType.CABLE] exercises, the caller is expected to have
     * asked the user for [newWeightKg] directly, since [Exercise.weightIncrementKg] doesn't apply;
     * for others, it's expected to be `exercise.weightKg + exercise.weightIncrementKg`.
     */
    fun respondToWeightIncreaseOffer(newWeightKg: Double?) {
        val exercise = exerciseToOfferWeightIncrease.value ?: return
        exerciseToOfferWeightIncrease.value = null
        if (newWeightKg != null) {
            viewModelScope.launch {
                repository.setExerciseWeight(exercise.exercise.id, newWeightKg)
                proceedAfterRating()
            }
        } else {
            proceedAfterRating()
        }
    }

    /**
     * Moves on from a just-rated exercise (and any weight-increase offer that followed): to the
     * next exercise, or, if that was the workout's last exercise, persists every rating collected
     * this session together with a log of the session itself (see
     * [TrainingPlanRepository.recordPerceivedEfforts] — logging it is also what marks this workout
     * as the most recently *finished* one, so [TrainingPlanRepository.nextWorkout] advances the
     * rotation only now, not when the workout was merely started), and finishes.
     */
    private fun proceedAfterRating() {
        val isLastExercise = exerciseIndex.value >= (latestExercises?.lastIndex ?: -1)
        if (isLastExercise) {
            viewModelScope.launch {
                // Counted before recording, while the history still only holds earlier sessions. A
                // first-ever performance has nothing to beat, so it does not count.
                personalRecords = collectedRatings.count { (exerciseId, entry) ->
                    val previous = latestExercises?.firstOrNull { it.exercise.id == exerciseId }?.records
                    !previous.isNullOrEmpty() && entry.weightKg > previous.maxOf { it.weightKg }
                }
                repository.recordPerceivedEfforts(workoutId, collectedRatings, startedAt)
                readyToFinish.value = true
            }
        } else {
            exerciseIndex.value += 1
            step.value = ExerciseStep.WarmUp
        }
    }

    private fun nextStep(current: ExerciseStep): ExerciseStep = when (current) {
        ExerciseStep.WarmUp -> ExerciseStep.WorkingSet(setIndex = 0)
        is ExerciseStep.WorkingSet -> ExerciseStep.WorkingSet(setIndex = current.setIndex + 1)
    }

    private fun startRecoveryTimer() {
        resting.value = true
        recoverySeconds.value = 0
        val goalSeconds = latestExercises?.getOrNull(exerciseIndex.value)?.exercise?.restSeconds
        var alerted = false
        recoveryJob = viewModelScope.launch {
            while (true) {
                delay(RECOVERY_TICK_MILLIS)
                val seconds = (recoverySeconds.value ?: 0) + 1
                recoverySeconds.value = seconds
                if (!alerted && goalSeconds != null && seconds >= goalSeconds) {
                    alerted = true
                    restTimerAlerter.onRestGoalReached()
                }
            }
        }
    }

    private fun stopRecoveryTimer() {
        recoveryJob?.cancel()
        recoveryJob = null
        resting.value = false
        recoverySeconds.value = null
    }

    override fun onCleared() {
        recoveryJob?.cancel()
    }
}

class WorkoutSessionViewModelFactory(
    private val repository: TrainingPlanRepository,
    private val workoutId: String,
    private val restTimerAlerter: RestTimerAlerter,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(WorkoutSessionViewModel::class.java))
        return WorkoutSessionViewModel(repository, workoutId, restTimerAlerter) as T
    }
}
