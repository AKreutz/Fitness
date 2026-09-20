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
}
