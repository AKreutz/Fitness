package com.akreutz.fitness.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.akreutz.fitness.data.repository.TrainingPlanRepository

class HomeViewModelFactory(
    private val repository: TrainingPlanRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        return HomeViewModel(repository) as T
    }
}

class CreateTrainingPlanViewModelFactory(
    private val repository: TrainingPlanRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(CreateTrainingPlanViewModel::class.java))
        return CreateTrainingPlanViewModel(repository) as T
    }
}
