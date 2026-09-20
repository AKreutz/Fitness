package com.akreutz.fitness.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.launch

class CreateTrainingPlanViewModel(private val repository: TrainingPlanRepository) : ViewModel() {

    /**
     * Creates a new training plan named [name] with one workout per entry in [workoutNames]
     * (blank entries are dropped), and marks it as the user's active plan.
     */
    fun createTrainingPlan(name: String, workoutNames: List<String>) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        val trimmedWorkoutNames = workoutNames.map { it.trim() }.filter { it.isNotEmpty() }

        viewModelScope.launch {
            repository.createTrainingPlan(trimmedName, trimmedWorkoutNames)
        }
    }
}
