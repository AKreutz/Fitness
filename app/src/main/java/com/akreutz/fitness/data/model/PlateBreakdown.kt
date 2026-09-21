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

    /**
     * The plates needed on one side of the bar to reach [totalWeightKg], largest first, or an
     * empty list if [totalWeightKg] is at or below [BAR_WEIGHT_KG] (nothing to load) or the
     * remainder can't be made up exactly from [PLATE_SIZES_KG].
     */
    fun forWeight(totalWeightKg: Double): List<Double> {
        val perSideKg = (totalWeightKg - BAR_WEIGHT_KG) / 2.0
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
