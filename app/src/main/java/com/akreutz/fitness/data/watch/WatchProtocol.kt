package com.akreutz.fitness.data.watch

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
     * One piece of the heart rate readings the watch sends after a finished session [sessionId]: piece number [index]
     * of [count], to be put together with the others. [packed] holds three readings in each number (see
     * [WatchProtocol]); [sampleCount] is how many readings there are in all. The readings are one per second from
     * [recordingStartEpochSeconds]. A [count] of 0 says the watch has none.
     */
    data class HeartRateChunk(
        val sessionId: String,
        val recordingStartEpochSeconds: Long,
        val index: Int,
        val count: Int,
        val sampleCount: Int,
        val packed: List<Int>,
    ) : WatchCommand

    /** The watch has (re)started and wants the current state. */
    data object Sync : WatchCommand
}

/**
 * A [command] together with the id the watch gave it, which stays the same if it is sent twice, and the [version] of the
 * state it was pressed on.
 */
data class WatchMessage(val id: Long?, val version: Int?, val command: WatchCommand)

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
 * - `v`: the state's version, which goes up with every change (added by [WatchSessionBridge])
 * - finished only, `p` is [PHASE_FINISHED]: `pr` personal records, `sid` the id the session is saved under, `ok` 0 while
 *   the phone waits for the watch's heart rate readings before saving, and 1 once it has saved the session, and then `hr`
 *   1 if the readings were saved with it
 *
 * When the session is left or discarded the phone sends `{t: "idle"}` instead, and the watch goes back to waiting. A
 * finished session is not signed off, so its summary stays on the watch.
 *
 * Watch to phone, every message has `t`. The ones that change something also have an id `m`, which stays the same if
 * the watch sends it again, and the `v` of the state they were pressed on, so a press on a screen the phone has already
 * left is ignored instead of acting twice:
 * - `adv`, `skip`, `sync`
 * - `rate`: `e` is 0 (easy), 1 (medium) or 2 (hard)
 * - `inc`: `y` is 1 to take the offered weight, 0 to keep the current one
 * - `hl`: the heart rate readings, sent once when a finished state with `ok` 0 says the phone is waiting for them: `sid`
 *   is the session's id, `t0` when the first reading was taken (epoch seconds), `k` how many pieces there are (0 if the
 *   watch has no readings), `i` this piece's number from 0, `n` the number of readings in all, and `d` this piece's
 *   readings: one reading per second, three to a number (the first in the lowest 8 bits), 0 for a second without one
 */
object WatchProtocol {
    const val PHASE_WARM_UP = 0
    const val PHASE_WORKING = 1
    const val PHASE_RESTING = 2
    const val PHASE_RATE = 3
    const val PHASE_WEIGHT_OFFER = 4
    const val PHASE_FINISHED = 5
    const val PHASE_UNAVAILABLE = 6

    // What a heart rate transfer may claim to hold, so a broken message cannot make the phone allocate without end: a
    // day of readings.
    private const val MAX_SAMPLES = 24 * 3_600
    private const val MAX_CHUNKS = MAX_SAMPLES

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
        is WorkoutSessionUiState.WaitingForHeartRate -> mapOf(
            "t" to "state",
            "p" to PHASE_FINISHED,
            "pr" to state.personalRecords,
            "sid" to state.sessionId,
            "ok" to 0,
        )
        is WorkoutSessionUiState.Finished -> mapOf(
            "t" to "state",
            "p" to PHASE_FINISHED,
            "pr" to state.personalRecords,
            "sid" to state.sessionId,
            "ok" to 1,
            "hr" to if (state.heartRateSaved) 1 else 0,
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

    /** Tells the watch the session is over, so it goes back to waiting for the next one. */
    fun encodeSignOff(): Map<String, Any> = mapOf("t" to "idle")

    /** Decodes a message from the watch, or `null` if it is not one this app understands. */
    fun decode(message: Map<String, Any?>): WatchMessage? {
        val id = message["m"].asLong()
        val version = message["v"].asLong()?.toInt()
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
            "hl" -> decodeHeartRate(message)
            else -> null
        } ?: return null
        return WatchMessage(id, version, command)
    }

    private fun decodeHeartRate(message: Map<String, Any?>): WatchCommand.HeartRateChunk? {
        val sessionId = message["sid"] as? String ?: return null
        val count = message["k"].asLong()?.toInt()?.takeIf { it in 0..MAX_CHUNKS } ?: return null
        if (count == 0) return WatchCommand.HeartRateChunk(sessionId, 0, 0, 0, 0, emptyList())
        val index = message["i"].asLong()?.toInt()?.takeIf { it in 0 until count } ?: return null
        val sampleCount = message["n"].asLong()?.toInt()?.takeIf { it in 1..MAX_SAMPLES } ?: return null
        val start = message["t0"].asLong() ?: return null
        val packed = (message["d"] as? List<*>)?.map { (it as? Number)?.toInt() ?: return null } ?: return null
        return WatchCommand.HeartRateChunk(sessionId, start, index, count, sampleCount, packed)
    }

    // Numbers arrive as whichever integer type the SDK picked.
    private fun Any?.asLong(): Long? = (this as? Number)?.toLong()
}

/** Whether the command changes where the session is, so it only makes sense on the state it was pressed on. */
val WatchCommand.changesSession: Boolean
    get() = this is WatchCommand.Advance || this is WatchCommand.Skip || this is WatchCommand.Rate ||
        this is WatchCommand.RespondToWeightIncrease
