package com.akreutz.fitness.data.watch

import com.akreutz.fitness.data.model.HeartRateStats
import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.data.model.targetReps
import com.akreutz.fitness.ui.session.SetProgress
import com.akreutz.fitness.ui.session.WorkoutSessionUiState

/** What the watch asks the phone to do. */
sealed interface WatchCommand {
    /** Finish the current set, or start the next one while resting. */
    data object Advance : WatchCommand

    /** Skip the current exercise. */
    data object Skip : WatchCommand

    /** Rate the exercise that was just finished. */
    data class Rate(val effort: PerceivedEffort) : WatchCommand

    /** Answer the "raise the weight for next time?" offer. */
    data class RespondToWeightIncrease(val accept: Boolean) : WatchCommand

    /**
     * Heart rate measured so far for the exercise with [exerciseId], and for the whole session.
     * Sent repeatedly, each one replacing the one before.
     */
    data class HeartRate(
        val exerciseId: String,
        val exercise: HeartRateStats,
        val session: HeartRateStats,
    ) : WatchCommand

    /** The watch has (re)started and wants the current state. */
    data object Sync : WatchCommand
}

/** A [command] together with the id the watch gave it, which stays the same if it is sent twice. */
data class WatchMessage(val id: Long?, val command: WatchCommand)

/**
 * The messages between this app and the watch app in `garmin/`, as the dictionaries Connect IQ
 * carries. Keys are kept short since the messages are small and frequent.
 *
 * Phone to watch, one `state` message each time the session changes:
 * - `t`: `"state"`
 * - `p`: the phase, one of the `PHASE_` constants
 * - `w`: workout name; `e`: exercise name; `id`: exercise id; `n`/`N`: exercise number / count
 * - `s`/`S`: set number / count (working and resting only)
 * - `kg`: weight; `r`: target reps
 * - `rg`: rest goal in seconds; `rt`: when the rest began, in epoch seconds (resting only)
 * - `nw`: the weight on offer (weight increase offer only)
 * - `sk`: 1 if the exercise can be skipped
 * - `pr`: personal records (finished only)
 *
 * Watch to phone, every message has `t` and the ones that change something also have an id `m`
 * that stays the same if the watch sends it again:
 * - `adv`, `skip`, `sync`
 * - `rate`: `e` is 0 (easy), 1 (medium) or 2 (hard)
 * - `inc`: `y` is 1 to take the offered weight, 0 to keep the current one
 * - `hr`: `id` is the exercise id, `a`/`x` the exercise's average / max heart rate, `sa`/`sx` the
 *   session's
 */
object WatchProtocol {
    const val PHASE_WARM_UP = 0
    const val PHASE_WORKING = 1
    const val PHASE_RESTING = 2
    const val PHASE_RATE = 3
    const val PHASE_WEIGHT_OFFER = 4
    const val PHASE_FINISHED = 5
    const val PHASE_UNAVAILABLE = 6

    /**
     * Encodes [state], or returns `null` while it is still loading. [restStartedAtEpochSeconds] is
     * when the current rest began and [increasedWeightKg] the weight to offer; each is only used
     * in the phase it belongs to.
     */
    fun encodeState(
        state: WorkoutSessionUiState,
        restStartedAtEpochSeconds: Long?,
        increasedWeightKg: Double?,
    ): Map<String, Any>? = when (state) {
        WorkoutSessionUiState.Loading -> null
        WorkoutSessionUiState.Unavailable -> mapOf("t" to "state", "p" to PHASE_UNAVAILABLE)
        is WorkoutSessionUiState.Finished -> mapOf(
            "t" to "state",
            "p" to PHASE_FINISHED,
            "pr" to state.personalRecords,
        )
        is WorkoutSessionUiState.InProgress -> encodeInProgress(state, restStartedAtEpochSeconds, increasedWeightKg)
    }

    private fun encodeInProgress(
        state: WorkoutSessionUiState.InProgress,
        restStartedAtEpochSeconds: Long?,
        increasedWeightKg: Double?,
    ): Map<String, Any> {
        // A pending prompt is about the exercise just finished, which is still the one shown.
        val exercise = state.exerciseToRate ?: state.exerciseToOfferWeightIncrease ?: state.exercise
        val phase = when {
            state.exerciseToRate != null -> PHASE_RATE
            state.exerciseToOfferWeightIncrease != null -> PHASE_WEIGHT_OFFER
            else -> when (state.currentSet) {
                SetProgress.WarmUp -> PHASE_WARM_UP
                is SetProgress.Working -> PHASE_WORKING
                is SetProgress.Resting -> PHASE_RESTING
            }
        }
        val message = mutableMapOf<String, Any>(
            "t" to "state",
            "p" to phase,
            "w" to state.workoutName,
            "e" to exercise.exercise.name,
            "id" to exercise.exercise.id,
            "n" to state.exerciseNumber,
            "N" to state.totalExercises,
            "kg" to exercise.exercise.weightKg,
            "r" to exercise.targetReps,
            "rg" to exercise.exercise.restSeconds,
            "sk" to if (state.canSkip) 1 else 0,
        )
        val workingSet = when (val set = state.currentSet) {
            is SetProgress.Working -> set
            is SetProgress.Resting -> set.completedSet as? SetProgress.Working
            SetProgress.WarmUp -> null
        }
        if (workingSet != null && phase != PHASE_RATE && phase != PHASE_WEIGHT_OFFER) {
            message["s"] = workingSet.setNumber
            message["S"] = workingSet.totalSets
        }
        if (phase == PHASE_RESTING && restStartedAtEpochSeconds != null) {
            message["rt"] = restStartedAtEpochSeconds
        }
        if (phase == PHASE_WEIGHT_OFFER && increasedWeightKg != null) {
            message["nw"] = increasedWeightKg
        }
        return message
    }

    /** Decodes a message from the watch, or `null` if it is not one this app understands. */
    fun decode(message: Map<String, Any?>): WatchMessage? {
        val id = message["m"].asLong()
        val command = when (message["t"]) {
            "adv" -> WatchCommand.Advance
            "skip" -> WatchCommand.Skip
            "sync" -> WatchCommand.Sync
            "rate" -> when (message["e"].asLong()) {
                0L -> PerceivedEffort.EASY
                1L -> PerceivedEffort.MEDIUM
                2L -> PerceivedEffort.HARD
                else -> null
            }?.let(WatchCommand::Rate)
            "inc" -> message["y"].asLong()?.let { WatchCommand.RespondToWeightIncrease(accept = it == 1L) }
            "hr" -> decodeHeartRate(message)
            else -> null
        } ?: return null
        return WatchMessage(id, command)
    }

    private fun decodeHeartRate(message: Map<String, Any?>): WatchCommand.HeartRate? {
        val exerciseId = message["id"] as? String ?: return null
        val exercise = heartRate(message["a"], message["x"]) ?: return null
        val session = heartRate(message["sa"], message["sx"]) ?: return null
        return WatchCommand.HeartRate(exerciseId, exercise, session)
    }

    private fun heartRate(avg: Any?, max: Any?): HeartRateStats? {
        val avgBpm = avg.asLong()?.toInt() ?: return null
        val maxBpm = max.asLong()?.toInt() ?: return null
        return HeartRateStats(avgBpm, maxBpm).takeIf { avgBpm > 0 && maxBpm >= avgBpm }
    }

    // Numbers arrive as whichever integer type the SDK picked.
    private fun Any?.asLong(): Long? = (this as? Number)?.toLong()
}
