package com.akreutz.fitness.data.watch

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hands a workout session over to the watch and the watch's commands back to it: the session screen
 * publishes its [state] and listens to [commands], while `WatchSyncService` moves both across the
 * Connect IQ link.
 */
class WatchSessionBridge {
    /** Whether a guided workout session is open. The watch connection stays up while it is. */
    val sessionActive = MutableStateFlow(false)

    private val _state = MutableStateFlow<Map<String, Any>?>(null)

    /** The session as the watch should see it (see [WatchProtocol]), or `null` if there is none. */
    val state: StateFlow<Map<String, Any>?> = _state.asStateFlow()

    private val _commands = MutableSharedFlow<WatchCommand>(extraBufferCapacity = COMMAND_BUFFER)

    /** What the watch asks for. */
    val commands: SharedFlow<WatchCommand> = _commands.asSharedFlow()

    private val seenIds = ArrayDeque<Long>()

    fun publishState(state: Map<String, Any>?) {
        _state.value = state
    }

    /** Decodes a raw [message] from the watch and passes it on, unless it is one seen before. */
    fun onWatchMessage(message: Map<String, Any?>) {
        val decoded = WatchProtocol.decode(message) ?: return
        decoded.id?.let { id ->
            if (id in seenIds) return
            seenIds.addLast(id)
            if (seenIds.size > SEEN_IDS_KEPT) seenIds.removeFirst()
        }
        _commands.tryEmit(decoded.command)
    }

    private companion object {
        const val COMMAND_BUFFER = 16
        const val SEEN_IDS_KEPT = 64
    }
}
