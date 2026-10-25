package com.akreutz.fitness.data.watch

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRateReceiverTest {
    // Three readings to a number, the first in the lowest 8 bits.
    private fun pack(a: Int, b: Int, c: Int) = a or (b shl 8) or (c shl 16)

    private fun chunk(index: Int, count: Int, sampleCount: Int, vararg packed: Int) =
        WatchCommand.HeartRateChunk("session-1", 1_000L, index, count, sampleCount, packed.toList())

    @Test
    fun putsTogetherThePiecesInTheirOrderWhateverOrderTheyArriveIn() {
        val receiver = HeartRateReceiver()

        assertNull(receiver.onChunk(chunk(1, 2, 5, pack(130, 0, 255))))
        val recording = receiver.onChunk(chunk(0, 2, 5, pack(60, 70, 80)))

        assertEquals("session-1", recording?.sessionId)
        assertEquals(1_000L, recording?.recordingStartEpochSeconds)
        // Pieces 0 and 1 hold three readings each, but only five exist: the last one is padding.
        assertArrayEquals(
            byteArrayOf(60, 70, 80, 130.toByte(), 0),
            recording?.samples,
        )
    }

    @Test
    fun readsAReadingAbove127AsAPositiveBpm() {
        val receiver = HeartRateReceiver()

        val recording = receiver.onChunk(chunk(0, 1, 3, pack(200, 255, 1)))

        assertEquals(listOf(200, 255, 1), recording?.samples?.map { it.toInt() and 0xFF })
    }

    @Test
    fun aPieceSentTwiceIsTakenInOnce() {
        val receiver = HeartRateReceiver()

        assertNull(receiver.onChunk(chunk(0, 2, 6, pack(1, 2, 3))))
        assertNull(receiver.onChunk(chunk(0, 2, 6, pack(1, 2, 3))))
        val recording = receiver.onChunk(chunk(1, 2, 6, pack(4, 5, 6)))

        assertEquals(listOf(1, 2, 3, 4, 5, 6), recording?.samples?.map { it.toInt() })
    }

    @Test
    fun aWatchWithoutReadingsGivesAnEmptyRecording() {
        val receiver = HeartRateReceiver()

        val recording = receiver.onChunk(WatchCommand.HeartRateChunk("session-1", 0, 0, 0, 0, emptyList()))

        assertEquals("session-1", recording?.sessionId)
        assertTrue(recording?.samples?.isEmpty() == true)
    }

    @Test
    fun aNewSessionDropsWhatWasLeftOfTheLastOne() {
        val receiver = HeartRateReceiver()
        assertNull(receiver.onChunk(chunk(0, 2, 6, pack(1, 2, 3))))

        val other = WatchCommand.HeartRateChunk("session-2", 2_000L, 0, 1, 3, listOf(pack(9, 8, 7)))
        val recording = receiver.onChunk(other)

        assertEquals("session-2", recording?.sessionId)
        assertEquals(listOf(9, 8, 7), recording?.samples?.map { it.toInt() })
    }
}
