package com.akreutz.fitness.data.model

/**
 * The fixed set of rep schemes a user can choose from when adding an [Exercise]. Each is a
 * list of per-set rep targets, shown to the user as e.g. "8, 10, 12".
 */
object RepScheme {
    val options: List<List<Int>> = listOf(
        listOf(8, 10, 12),
    )

    /** Renders a rep scheme the way it's shown to the user, e.g. "8-10-12". */
    fun format(reps: List<Int>): String = reps.joinToString("-")

    /**
     * The index into a scheme of [stepCount] targets that the next session should use, given the
     * [efforts] rated in earlier sessions at the current weight (oldest first). Starts at the first
     * (lowest) step; each [PerceivedEffort.HARD] rating moves one step down, [PerceivedEffort.MEDIUM]
     * keeps the step, and [PerceivedEffort.EASY] moves one step up, never leaving the scheme.
     */
    fun targetStep(stepCount: Int, efforts: List<PerceivedEffort>): Int =
        efforts.fold(0) { step, effort ->
            when (effort) {
                PerceivedEffort.HARD -> step - 1
                PerceivedEffort.MEDIUM -> step
                PerceivedEffort.EASY -> step + 1
            }.coerceIn(0, stepCount - 1)
        }

    /**
     * The single rep target to use for every set of a session, chosen from [reps] by the step
     * reached after the [efforts] rated at the current weight (see [targetStep]).
     */
    fun targetReps(reps: List<Int>, efforts: List<PerceivedEffort>): Int =
        reps[targetStep(reps.size, efforts)]
}
