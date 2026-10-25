package com.akreutz.fitness.data.watch

import com.akreutz.fitness.data.model.PerceivedEffort
import com.akreutz.fitness.ui.session.WorkoutSessionUiState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun decodesTheStateVersionACommandWasPressedOn() {
        assertEquals(12, WatchProtocol.decode(mapOf("t" to "adv", "v" to 12))?.version)
        assertNull(WatchProtocol.decode(mapOf("t" to "adv"))?.version)
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
    fun decodesAPieceOfTheHeartRateReadings() {
        val message = mapOf(
            "t" to "hl",
            "sid" to "session-1",
            "t0" to 1_792_931_000L,
            "k" to 3,
            "i" to 1,
            "n" to 7,
            "d" to listOf(1, 2, 3),
        )
        assertEquals(
            WatchCommand.HeartRateChunk("session-1", 1_792_931_000L, 1, 3, 7, listOf(1, 2, 3)),
            WatchProtocol.decode(message)?.command,
        )
    }

    @Test
    fun decodesAWatchWithoutHeartRateReadings() {
        assertEquals(
            WatchCommand.HeartRateChunk("session-1", 0, 0, 0, 0, emptyList()),
            WatchProtocol.decode(mapOf("t" to "hl", "sid" to "session-1", "k" to 0))?.command,
        )
    }

    @Test
    fun ignoresHeartRateReadingsThatCannotBeRight() {
        val valid = mapOf<String, Any>(
            "t" to "hl", "sid" to "s", "t0" to 1L, "k" to 2, "i" to 0, "n" to 5, "d" to listOf(1, 2),
        )
        assertTrue(WatchProtocol.decode(valid) != null)
        assertNull(WatchProtocol.decode(valid - "sid"))
        assertNull(WatchProtocol.decode(valid + ("i" to 2)))
        assertNull(WatchProtocol.decode(valid + ("n" to 0)))
        assertNull(WatchProtocol.decode(valid + ("k" to -1)))
        assertNull(WatchProtocol.decode(valid + ("d" to listOf("a"))))
        assertNull(WatchProtocol.decode(valid - "d"))
    }

    @Test
    fun ignoresMessagesItDoesNotUnderstand() {
        assertNull(WatchProtocol.decode(mapOf("t" to "nope")))
        assertNull(WatchProtocol.decode(emptyMap()))
    }

    @Test
    fun signOffIsAnIdleMessage() {
        assertEquals(mapOf<String, Any>("t" to "idle"), WatchProtocol.encodeSignOff())
    }

    @Test
    fun theWatchIsToldWhenTheSessionIsWaitingForItsHeartRate() {
        val waiting = WorkoutSessionUiState.WaitingForHeartRate(
            personalRecords = 2,
            sessionId = "session-1",
            receivedPieces = 0,
            totalPieces = 0,
        )

        assertEquals(
            mapOf<String, Any>("t" to "state", "p" to WatchProtocol.PHASE_FINISHED, "pr" to 2, "sid" to "session-1", "ok" to 0),
            WatchProtocol.encodeState(waiting, null, null),
        )
    }

    @Test
    fun theWatchIsToldWhenTheSessionIsSavedAndWhetherItsHeartRateWas() {
        assertEquals(
            mapOf<String, Any>(
                "t" to "state", "p" to WatchProtocol.PHASE_FINISHED, "pr" to 1, "sid" to "session-1", "ok" to 1, "hr" to 1,
            ),
            WatchProtocol.encodeState(WorkoutSessionUiState.Finished(1, "session-1", heartRateSaved = true), null, null),
        )
        assertEquals(
            0,
            WatchProtocol.encodeState(WorkoutSessionUiState.Finished(1, "session-1", heartRateSaved = false), null, null)
                ?.get("hr"),
        )
    }

    @Test
    fun bridgeWaitsForTheWatchOnlyWhenItIsConnectedAndHasBeenInTouch() {
        val bridge = WatchSessionBridge()
        bridge.beginSession()
        assertFalse(bridge.expectsHeartRate())

        bridge.watchConnected.value = true
        assertFalse(bridge.expectsHeartRate())

        bridge.onWatchMessage(mapOf("t" to "sync"))
        assertTrue(bridge.expectsHeartRate())

        bridge.watchConnected.value = false
        assertFalse(bridge.expectsHeartRate())

        // A new session starts without anything the watch said in the last one.
        bridge.watchConnected.value = true
        bridge.beginSession()
        assertFalse(bridge.expectsHeartRate())
    }

    @Test
    fun bridgeHandsOverTheHeartRateReadingsOnceTheyAreAllHere() = runBlocking {
        val bridge = WatchSessionBridge()
        val waiting = async { bridge.awaitHeartRate("session-1", idleTimeoutMillis = 5_000) }

        val first = mapOf("t" to "hl", "sid" to "session-1", "t0" to 500L, "k" to 2, "i" to 0, "n" to 4, "d" to listOf(0x030201))
        val second = mapOf("t" to "hl", "sid" to "session-1", "t0" to 500L, "k" to 2, "i" to 1, "n" to 4, "d" to listOf(0x000004))
        bridge.onWatchMessage(second)
        assertEquals(HeartRateProgress(received = 1, total = 2), bridge.heartRateProgress.value)
        bridge.onWatchMessage(first)

        val recording = waiting.await()
        assertEquals(500L, recording?.recordingStartEpochSeconds)
        assertEquals(listOf(1, 2, 3, 4), recording?.samples?.map { it.toInt() })
        assertNull(bridge.heartRateProgress.value)
    }

    @Test
    fun bridgeGivesUpOnTheHeartRateReadingsWhenTheyStopComing() = runBlocking {
        val bridge = WatchSessionBridge()

        assertNull(bridge.awaitHeartRate("session-1", idleTimeoutMillis = 100))
    }

    @Test
    fun bridgeIgnoresReadingsForAnotherSession() = runBlocking {
        val bridge = WatchSessionBridge()
        val waiting = async { bridge.awaitHeartRate("session-2", idleTimeoutMillis = 700) }

        bridge.onWatchMessage(mapOf("t" to "hl", "sid" to "session-1", "k" to 0))

        assertNull(waiting.await())
    }

    @Test
    fun aWatchWithoutReadingsEndsTheWaitAtOnce() = runBlocking {
        val bridge = WatchSessionBridge()
        val waiting = async { bridge.awaitHeartRate("session-1", idleTimeoutMillis = 5_000) }

        bridge.onWatchMessage(mapOf("t" to "hl", "sid" to "session-1", "k" to 0))

        assertEquals(0, waiting.await()?.samples?.size)
    }

    @Test
    fun bridgeRemembersWhetherTheSessionWasFinished() {
        val bridge = WatchSessionBridge()
        bridge.beginSession()
        bridge.publishState(mapOf("t" to "state", "p" to WatchProtocol.PHASE_WORKING))
        assertFalse(bridge.endedFinished)

        // Still waiting for the watch's heart rate: leaving now is leaving, not finishing.
        bridge.publishState(mapOf("t" to "state", "p" to WatchProtocol.PHASE_FINISHED, "ok" to 0))
        assertFalse(bridge.endedFinished)

        bridge.publishState(mapOf("t" to "state", "p" to WatchProtocol.PHASE_FINISHED, "ok" to 1, "hr" to 1))
        // Clearing the state when the screen goes away must not make a finished session look discarded.
        bridge.publishState(null)
        bridge.endSession()
        assertTrue(bridge.endedFinished)

        bridge.beginSession()
        assertFalse(bridge.endedFinished)
    }

    @Test
    fun bridgePassesOnACommandOnlyOnce() = runBlocking {
        val bridge = WatchSessionBridge()
        val received = mutableListOf<WatchCommand>()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { bridge.commands.toList(received) }

        bridge.onWatchMessage(mapOf("t" to "adv", "m" to 1L))
        bridge.onWatchMessage(mapOf("t" to "adv", "m" to 1L))
        bridge.onWatchMessage(mapOf("t" to "adv", "m" to 2L))
        yield()

        assertEquals(listOf(WatchCommand.Advance, WatchCommand.Advance), received)
        job.cancel()
    }

    @Test
    fun bridgeAnswersASyncRequestByAskingForAResend() = runBlocking {
        val bridge = WatchSessionBridge()
        val commands = mutableListOf<WatchCommand>()
        val resends = mutableListOf<Unit>()
        val commandsJob = launch(start = CoroutineStart.UNDISPATCHED) { bridge.commands.toList(commands) }
        val resendsJob = launch(start = CoroutineStart.UNDISPATCHED) { bridge.resendRequests.toList(resends) }

        bridge.onWatchMessage(mapOf("t" to "sync"))
        yield()

        assertEquals(1, resends.size)
        assertTrue(commands.isEmpty())
        commandsJob.cancel()
        resendsJob.cancel()
    }

    @Test
    fun bridgeDropsACommandPressedOnAnOutOfDateScreen() = runBlocking {
        val bridge = WatchSessionBridge()
        bridge.publishState(mapOf("t" to "state", "p" to WatchProtocol.PHASE_WARM_UP))
        val commands = mutableListOf<WatchCommand>()
        val resends = mutableListOf<Unit>()
        val commandsJob = launch(start = CoroutineStart.UNDISPATCHED) { bridge.commands.toList(commands) }
        val resendsJob = launch(start = CoroutineStart.UNDISPATCHED) { bridge.resendRequests.toList(resends) }
        val firstVersion = bridge.state.value?.get("v") as Int
        bridge.publishState(mapOf("t" to "state", "p" to WatchProtocol.PHASE_WORKING))
        val secondVersion = bridge.state.value?.get("v") as Int

        bridge.onWatchMessage(mapOf("t" to "adv", "m" to 1L, "v" to firstVersion))
        bridge.onWatchMessage(mapOf("t" to "adv", "m" to 2L, "v" to secondVersion))
        // Heart rate readings do not depend on where the session is, so they are never out of date.
        bridge.onWatchMessage(mapOf("t" to "hl", "sid" to "a", "k" to 0, "v" to firstVersion))
        yield()

        assertEquals(listOf(WatchCommand.Advance), commands)
        assertEquals(1, resends.size)
        commandsJob.cancel()
        resendsJob.cancel()
    }

    @Test
    fun publishingTheSameStateAgainKeepsItsVersion() {
        val bridge = WatchSessionBridge()
        bridge.publishState(mapOf("t" to "state", "p" to WatchProtocol.PHASE_WORKING))
        val version = bridge.state.value?.get("v")

        bridge.publishState(mapOf("t" to "state", "p" to WatchProtocol.PHASE_WORKING))
        assertEquals(version, bridge.state.value?.get("v"))

        bridge.publishState(mapOf("t" to "state", "p" to WatchProtocol.PHASE_RESTING))
        assertEquals((version as Int) + 1, bridge.state.value?.get("v"))
    }
}
