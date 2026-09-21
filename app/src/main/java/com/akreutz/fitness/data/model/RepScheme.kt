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
     * The single rep target to use for every set of a session, chosen from [reps] by how hard
     * the exercise's most recent performance felt: [PerceivedEffort.HIGH] or no prior
     * performance picks the first (easiest) target, [PerceivedEffort.MEDIUM] the second, and
     * [PerceivedEffort.LOW] the third.
     */
    fun targetReps(reps: List<Int>, lastPerceivedEffort: PerceivedEffort?): Int {
        val index = when (lastPerceivedEffort) {
            PerceivedEffort.LOW -> 2
            PerceivedEffort.MEDIUM -> 1
            PerceivedEffort.HIGH, null -> 0
        }
        return reps[index.coerceIn(reps.indices)]
    }
}
