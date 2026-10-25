package com.akreutz.fitness.data.model

/** Average and highest heart rate, in beats per minute, measured by the watch over some period. */
data class HeartRateStats(
    val avgBpm: Int,
    val maxBpm: Int,
)

/** Both of a stored average and max, or `null` if either is missing. */
fun heartRateStats(avgBpm: Int?, maxBpm: Int?): HeartRateStats? =
    if (avgBpm != null && maxBpm != null) HeartRateStats(avgBpm, maxBpm) else null
