package com.akreutz.fitness.data.watch

import com.akreutz.fitness.data.model.HeartRateStats
import com.akreutz.fitness.data.model.PerceivedEffort
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchProtocolTest {
    @Test
    fun decodesSimpleCommands() {
        assertEquals(WatchCommand.Advance, WatchProtocol.decode(mapOf("t" to "adv", "m" to 7L))?.command)
        assertEquals(WatchCommand.Skip, WatchProtocol.decode(mapOf("t" to "skip"))?.command)
        assertEquals(WatchCommand.Sync, WatchProtocol.decode(mapOf("t" to "sync"))?.command)
    }

    @Test
    fun decodesTheMessageIdWhateverIntegerTypeItArrivesAs() {
        assertEquals(7L, WatchProtocol.decode(mapOf("t" to "adv", "m" to 7))?.id)
        assertEquals(7L, WatchProtocol.decode(mapOf("t" to "adv", "m" to 7L))?.id)
        assertNull(WatchProtocol.decode(mapOf("t" to "adv"))?.id)
    }

    @Test
    fun decodesRatings() {
        assertEquals(
            WatchCommand.Rate(PerceivedEffort.EASY),
            WatchProtocol.decode(mapOf("t" to "rate", "e" to 0))?.command,
        )
        assertEquals(
            WatchCommand.Rate(PerceivedEffort.MEDIUM),
            WatchProtocol.decode(mapOf("t" to "rate", "e" to 1))?.command,
        )
        assertEquals(
            WatchCommand.Rate(PerceivedEffort.HARD),
            WatchProtocol.decode(mapOf("t" to "rate", "e" to 2))?.command,
        )
        assertNull(WatchProtocol.decode(mapOf("t" to "rate", "e" to 3)))
        assertNull(WatchProtocol.decode(mapOf("t" to "rate")))
    }

    @Test
    fun decodesTheWeightIncreaseAnswer() {
        assertEquals(
            WatchCommand.RespondToWeightIncrease(accept = true),
            WatchProtocol.decode(mapOf("t" to "inc", "y" to 1))?.command,
        )
        assertEquals(
            WatchCommand.RespondToWeightIncrease(accept = false),
            WatchProtocol.decode(mapOf("t" to "inc", "y" to 0))?.command,
        )
    }

    @Test
    fun decodesHeartRate() {
        val message = mapOf("t" to "hr", "id" to "bench", "a" to 120, "x" to 150, "sa" to 110, "sx" to 160)
        assertEquals(
            WatchCommand.HeartRate("bench", HeartRateStats(120, 150), HeartRateStats(110, 160)),
            WatchProtocol.decode(message)?.command,
        )
    }

    @Test
    fun ignoresHeartRateThatCannotBeRight() {
        // Zero means the watch had no reading; a max below the average is impossible.
        assertNull(WatchProtocol.decode(mapOf("t" to "hr", "id" to "a", "a" to 0, "x" to 0, "sa" to 100, "sx" to 120)))
        assertNull(WatchProtocol.decode(mapOf("t" to "hr", "id" to "a", "a" to 130, "x" to 120, "sa" to 100, "sx" to 120)))
        assertNull(WatchProtocol.decode(mapOf("t" to "hr", "a" to 100, "x" to 120, "sa" to 100, "sx" to 120)))
    }

    @Test
    fun ignoresMessagesItDoesNotUnderstand() {
        assertNull(WatchProtocol.decode(mapOf("t" to "nope")))
        assertNull(WatchProtocol.decode(emptyMap()))
    }

    @Test
    fun bridgePassesOnACommandOnlyOnce() = runBlocking {
        val bridge = WatchSessionBridge()
        val received = mutableListOf<WatchCommand>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { bridge.commands.toList(received) }

        bridge.onWatchMessage(mapOf("t" to "adv", "m" to 1L))
        bridge.onWatchMessage(mapOf("t" to "adv", "m" to 1L))
        bridge.onWatchMessage(mapOf("t" to "adv", "m" to 2L))
        bridge.onWatchMessage(mapOf("t" to "sync"))
        bridge.onWatchMessage(mapOf("t" to "sync"))
        yield()

        assertEquals(
            listOf(WatchCommand.Advance, WatchCommand.Advance, WatchCommand.Sync, WatchCommand.Sync),
            received,
        )
        job.cancel()
    }
}
