package com.akreutz.fitness.data.watch

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** How far the watch's heart rate readings have got: [received] pieces of [total]. */
data class HeartRateProgress(val received: Int, val total: Int)

/**
 * Hands a workout session over to the watch and the watch's commands back to it: the session screen
 * publishes its [state] and listens to [commands], while `WatchSyncService` moves both across the
 * Connect IQ link.
 *
 * Every state gets a version number that goes up with each change. A command from the watch carries the version of the
 * screen it was pressed on, and one for a screen the session has already left is dropped, so a press on a screen that
 * is out of date (the watch is a second or more behind) cannot act twice. The watch is then sent the current state.
 *
 * When the workout is finished the watch sends its heart rate readings, which the bridge puts together so the session
 * can wait for them ([awaitHeartRate]) before it is saved.
 */
class WatchSessionBridge {
    /** Whether a guided workout session is open. The watch connection stays up while it is. */
    val sessionActive = MutableStateFlow(false)

    /** Whether a watch is connected, as reported by the connection. */
    val watchConnected = MutableStateFlow(false)

    private val _state = MutableStateFlow<Map<String, Any>?>(null)

    /** The session as the watch should see it (see [WatchProtocol]), or `null` if there is none. */
    val state: StateFlow<Map<String, Any>?> = _state.asStateFlow()

    private val _commands = MutableSharedFlow<WatchCommand>(extraBufferCapacity = COMMAND_BUFFER)

    /** What the watch asks for. */
    val commands: SharedFlow<WatchCommand> = _commands.asSharedFlow()

    private val _resendRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the watch should be sent the current state again: it asked, or it is out of date. */
    val resendRequests: SharedFlow<Unit> = _resendRequests.asSharedFlow()

    private val heartRateReceiver = HeartRateReceiver()
    private val recordings = MutableSharedFlow<HeartRateReceiver.Recording>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _heartRateProgress = MutableStateFlow<HeartRateProgress?>(null)

    /** How much of the watch's heart rate readings has arrived, while they are on their way. */
    val heartRateProgress: StateFlow<HeartRateProgress?> = _heartRateProgress.asStateFlow()

    private val seenIds = ArrayDeque<Long>()
    private var lastHeartRateChunkAt = 0L
    private var watchSpoke = false

    // Starts from the clock so a new run of the app always numbers above an earlier one, which a watch that missed the
    // sign-off may still be holding on to.
    private var version = (System.currentTimeMillis() / 1000).toInt()
    private var lastPhase: Int? = null
    private var lastSaved = false

    /**
     * Whether the session that just ended had been finished and saved, as opposed to discarded or left, which includes
     * leaving while it still waited for the watch's heart rate.
     */
    val endedFinished: Boolean
        get() = lastPhase == WatchProtocol.PHASE_FINISHED && lastSaved

    fun beginSession() {
        lastPhase = null
        lastSaved = false
        watchSpoke = false
        sessionActive.value = true
    }

    fun endSession() {
        sessionActive.value = false
    }

    /**
     * Whether a finished workout should wait for the watch's heart rate readings: a watch is connected and its app has
     * been in touch during this session, so it is there to send them.
     */
    fun expectsHeartRate(): Boolean = watchConnected.value && watchSpoke

    /**
     * Waits for the watch's heart rate readings for the session [sessionId]. Returns them, or `null` once nothing has
     * arrived for [idleTimeoutMillis] (counted from the start, and from each piece that comes in).
     */
    suspend fun awaitHeartRate(sessionId: String, idleTimeoutMillis: Long): HeartRateReceiver.Recording? {
        lastHeartRateChunkAt = System.currentTimeMillis()
        _heartRateProgress.value = null
        try {
            while (true) {
                val recording = withTimeoutOrNull(POLL_MILLIS) { recordings.first { it.sessionId == sessionId } }
                if (recording != null) return recording
                if (System.currentTimeMillis() - lastHeartRateChunkAt > idleTimeoutMillis) return null
            }
        } finally {
            _heartRateProgress.value = null
        }
    }

    /** Publishes [state]; one that differs from the current one gets the next version number. */
    fun publishState(state: Map<String, Any>?) {
        if (state == null) {
            _state.value = null
            return
        }
        val current = _state.value
        if (current != null && current.filterKeys { it != KEY_VERSION } == state) return
        version++
        _state.value = state + (KEY_VERSION to version)
        (state["p"] as? Int)?.let { lastPhase = it }
        lastSaved = state["ok"] == 1
    }

    /** Decodes a raw [message] from the watch and passes it on, unless it is one seen before or out of date. */
    fun onWatchMessage(message: Map<String, Any?>) {
        val decoded = WatchProtocol.decode(message) ?: return
        decoded.id?.let { id ->
            if (id in seenIds) return
            seenIds.addLast(id)
            if (seenIds.size > SEEN_IDS_KEPT) seenIds.removeFirst()
        }
        watchSpoke = true
        val command = decoded.command
        if (command is WatchCommand.HeartRateChunk) {
            receiveHeartRate(command)
            return
        }
        if (command == WatchCommand.Sync) {
            _resendRequests.tryEmit(Unit)
            return
        }
        if (command.changesSession && decoded.version != null && decoded.version != version) {
            _resendRequests.tryEmit(Unit)
            return
        }
        _commands.tryEmit(command)
    }

    private fun receiveHeartRate(chunk: WatchCommand.HeartRateChunk) {
        lastHeartRateChunkAt = System.currentTimeMillis()
        val recording = heartRateReceiver.onChunk(chunk)
        if (recording != null) {
            _heartRateProgress.value = null
            recordings.tryEmit(recording)
        } else {
            _heartRateProgress.value = HeartRateProgress(heartRateReceiver.receivedPieces, heartRateReceiver.totalPieces)
        }
    }

    private companion object {
        const val COMMAND_BUFFER = 16
        const val SEEN_IDS_KEPT = 64
        const val KEY_VERSION = "v"
        const val POLL_MILLIS = 500L
    }
}
