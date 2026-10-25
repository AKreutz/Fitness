package com.akreutz.fitness.data.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeartRateSamplesTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun aRecordingThatBeganAfterTheSessionIsPaddedWithEmptySeconds() {
        val aligned = HeartRateSamples.alignToSession(bytes(100, 110), 1_003L, 1_000L)

        assertArrayEquals(bytes(0, 0, 0, 100, 110), aligned)
    }

    @Test
    fun aRecordingThatBeganBeforeTheSessionLosesTheSecondsBeforeIt() {
        val aligned = HeartRateSamples.alignToSession(bytes(90, 95, 100, 110), 998L, 1_000L)

        assertArrayEquals(bytes(100, 110), aligned)
    }

    @Test
    fun aRecordingThatStartsWithTheSessionIsKeptAsItIs() {
        assertArrayEquals(bytes(80, 82), HeartRateSamples.alignToSession(bytes(80, 82), 1_000L, 1_000L))
    }

    @Test
    fun trailingSecondsWithoutAReadingAreDropped() {
        assertArrayEquals(bytes(80, 0, 82), HeartRateSamples.alignToSession(bytes(80, 0, 82, 0, 0), 1_000L, 1_000L))
    }

    @Test
    fun clocksThatAreFarApartGiveNothing() {
        assertEquals(0, HeartRateSamples.alignToSession(bytes(80), 1_000L + 13 * 3_600, 1_000L).size)
        assertEquals(0, HeartRateSamples.alignToSession(bytes(80), 1_000L - 13 * 3_600, 1_000L).size)
    }

    @Test
    fun aRecordingEntirelyBeforeTheSessionGivesNothing() {
        assertEquals(0, HeartRateSamples.alignToSession(bytes(80, 82), 990L, 1_000L).size)
    }

    @Test
    fun statsAreTheAverageAndHighestOfTheReadings() {
        val stats = HeartRateSamples.stats(bytes(100, 120, 140))

        assertEquals(HeartRateStats(avgBpm = 120, maxBpm = 140), stats)
    }

    @Test
    fun statsSkipSecondsWithoutAReading() {
        assertEquals(HeartRateStats(100, 120), HeartRateSamples.stats(bytes(0, 80, 0, 120, 0)))
    }

    @Test
    fun statsOfAWindowOnlyCountThatStretch() {
        val samples = bytes(60, 100, 120, 200, 70)

        assertEquals(HeartRateStats(110, 120), HeartRateSamples.stats(samples, HeartRateWindow(1, 2)))
    }

    @Test
    fun readingsAbove127AreNotNegative() {
        assertEquals(HeartRateStats(190, 200), HeartRateSamples.stats(bytes(180, 200)))
    }

    @Test
    fun aWindowBeyondTheReadingsHasNoStats() {
        assertNull(HeartRateSamples.stats(bytes(100, 110), HeartRateWindow(5, 9)))
        assertNull(HeartRateSamples.stats(bytes(0, 0)))
    }
}
