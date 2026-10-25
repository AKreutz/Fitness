package com.akreutz.fitness.data.model

/**
 * The weight offered when the user raises this exercise's weight for next time (see
 * `WorkoutSessionViewModel.respondToWeightIncreaseOffer`): the next level of its [WeightStack]
 * above the current weight for cable exercises, +2.5 kg for cable ones without a usable stack, and
 * [Exercise.weightIncrementKg] on top of the current weight otherwise.
 */
fun ExerciseWithPerformanceHistory.suggestedIncreasedWeightKg(weightStacks: List<WeightStack>): Double {
    val current = exercise.weightKg
    if (exercise.type != ExerciseType.CABLE) {
        return current + (exercise.weightIncrementKg.takeIf { it > 0 } ?: 2.5)
    }
    val nextLevel = weightStacks.firstOrNull { it.id == exercise.weightStackId }
        ?.levelsKg
        ?.filter { it > current }
        ?.minOrNull()
    return nextLevel ?: (current + 2.5)
}
