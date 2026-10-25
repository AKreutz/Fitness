package com.akreutz.fitness.data.model

import kotlin.math.roundToLong

/**
 * The barbell plates needed per side to load a free-weight exercise to its target weight,
 * assuming a standard [BAR_WEIGHT_KG] barbell and the same plates mirrored on both sides.
 * Only meaningful for [ExerciseType.FREE_WEIGHTS] exercises; cable/machine stacks aren't loaded
 * with plates this way.
 */
object PlateBreakdown {
    const val BAR_WEIGHT_KG: Double = 20.0

    /** Available plate sizes, heaviest first, in KG. */
    val PLATE_SIZES_KG: List<Double> = listOf(20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    /** The smallest change in total weight the plates can make: a pair of the smallest plates. */
    val LOAD_STEP_KG: Double = PLATE_SIZES_KG.min() * 2

    /** [kg] rounded to the closest total weight the plates can make, never below zero. */
    fun nearestLoadable(kg: Double): Double =
        ((kg / LOAD_STEP_KG).roundToLong() * LOAD_STEP_KG).coerceAtLeast(0.0)

    /**
     * [kg] rounded to the closest total weight loadable on a bar of [barWeightKg], i.e. the bar
     * plus a whole number of [LOAD_STEP_KG]s. A bar that isn't a multiple of the step (e.g. 19 lb)
     * makes the plain [nearestLoadable] totals unreachable, hence this variant. Never below the bar.
     */
    fun nearestLoadable(kg: Double, barWeightKg: Double): Double =
        barWeightKg + nearestLoadable(kg - barWeightKg)

    /**
     * The plates needed on one side of the bar to reach [totalWeightKg], largest first, or an
     * empty list if [totalWeightKg] is at or below [barWeightKg] (nothing to load) or the
     * remainder can't be made up exactly from [PLATE_SIZES_KG].
     */
    fun forWeight(totalWeightKg: Double, barWeightKg: Double = BAR_WEIGHT_KG): List<Double> {
        val perSideKg = (totalWeightKg - barWeightKg) / 2.0
        if (perSideKg <= 0.0) return emptyList()

        // Work in hundredths of a KG (integers) to avoid floating-point drift while subtracting
        // repeatedly.
        var remaining = (perSideKg * 100).roundToLong()
        val plates = mutableListOf<Double>()
        for (plateKg in PLATE_SIZES_KG) {
            val plateUnits = (plateKg * 100).roundToLong()
            while (remaining >= plateUnits) {
                plates += plateKg
                remaining -= plateUnits
            }
        }
        return if (remaining == 0L) plates else emptyList()
    }
}
