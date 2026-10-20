package com.akreutz.fitness.data.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Pound conversions for weight entry. Everything stored and shown by the app stays in kg; lbs are
 * only ever typed in and converted straight away.
 */
const val KG_PER_LB: Double = 0.45359237

fun lbToKg(lb: Double): Double = lb * KG_PER_LB

fun kgToLb(kg: Double): Double = kg / KG_PER_LB

/** A weight with up to two decimals and no trailing zeros, e.g. "60", "62.5" or "1.25". */
fun formatWeight(value: Double): String =
    String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

/** [kg] rounded to 0.1, for weights that aren't loaded with plates. */
fun roundToTenthKg(kg: Double): Double = (kg * 10).roundToLong() / 10.0

/** The entry in [levels] closest to [kg], or [kg] rounded to 0.1 if [levels] is empty. */
fun nearestLevelKg(kg: Double, levels: List<Double>): Double =
    levels.minByOrNull { abs(it - kg) } ?: roundToTenthKg(kg)
