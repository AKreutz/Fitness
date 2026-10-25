package com.akreutz.fitness.data.model

/** Average and highest heart rate, in beats per minute, measured by the watch over some period. */
data class HeartRateStats(
    val avgBpm: Int,
    val maxBpm: Int,
)

/** Both of a stored average and max, or `null` if either is missing. */
fun heartRateStats(avgBpm: Int?, maxBpm: Int?): HeartRateStats? =
    if (avgBpm != null && maxBpm != null) HeartRateStats(avgBpm, maxBpm) else null

/** A stretch of a session, in seconds since it began, from [startSeconds] to [endSeconds]. */
data class HeartRateWindow(val startSeconds: Int, val endSeconds: Int)

/**
 * The readings the watch took for a session: one per second from [startEpochSeconds], each a heart rate in beats per
 * minute, 0 for a second without one.
 */
class HeartRateRecording(val startEpochSeconds: Long, val samples: ByteArray)

/**
 * The watch's heart rate readings for a session: one byte per second since the session began, 0 where the watch had no
 * reading.
 */
object HeartRateSamples {
    /** A recording that starts further than this from the session means the clocks are wrong; it is not used. */
    private const val MAX_OFFSET_SECONDS = 12 * 3_600L

    /**
     * Puts a watch recording that began at [recordingStartEpochSeconds] on the session's own clock, so that index 0 is
     * the second [sessionStartEpochSeconds] the session began. Returns an empty array when nothing is left.
     */
    fun alignToSession(
        samples: ByteArray,
        recordingStartEpochSeconds: Long,
        sessionStartEpochSeconds: Long,
    ): ByteArray {
        val offset = recordingStartEpochSeconds - sessionStartEpochSeconds
        if (offset > MAX_OFFSET_SECONDS || offset < -MAX_OFFSET_SECONDS) return ByteArray(0)
        val aligned = if (offset >= 0) {
            ByteArray(offset.toInt() + samples.size).also { samples.copyInto(it, offset.toInt()) }
        } else {
            val skip = (-offset).toInt()
            if (skip >= samples.size) ByteArray(0) else samples.copyOfRange(skip, samples.size)
        }
        val last = aligned.indexOfLast { it.toInt() != 0 }
        return if (last < 0) ByteArray(0) else aligned.copyOf(last + 1)
    }

    /**
     * The average and highest of the readings in [window] (all of [samples] when `null`), or `null` if there are none.
     */
    fun stats(samples: ByteArray, window: HeartRateWindow? = null): HeartRateStats? {
        val from = (window?.startSeconds ?: 0).coerceAtLeast(0)
        val to = minOf(window?.endSeconds ?: (samples.size - 1), samples.size - 1)
        var sum = 0
        var count = 0
        var max = 0
        for (index in from..to) {
            val bpm = samples[index].toInt() and 0xFF
            if (bpm == 0) continue
            sum += bpm
            count++
            if (bpm > max) max = bpm
        }
        return if (count == 0) null else HeartRateStats((sum + count / 2) / count, max)
    }
}
