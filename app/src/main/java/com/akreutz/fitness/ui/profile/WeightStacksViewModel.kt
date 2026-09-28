package com.akreutz.fitness.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.akreutz.fitness.data.model.WeightStack
import com.akreutz.fitness.data.repository.TrainingPlanRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Drives the weight-stacks management screen: every saved [WeightStack], with add/edit/delete. */
class WeightStacksViewModel(private val repository: TrainingPlanRepository) : ViewModel() {

    val weightStacks: StateFlow<List<WeightStack>> = repository.observeWeightStacks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    suspend fun createWeightStack(name: String, levelsKg: List<Double>): WeightStack =
        repository.createWeightStack(name, levelsKg)

    fun updateWeightStack(stack: WeightStack, name: String, levelsKg: List<Double>) {
        viewModelScope.launch { repository.updateWeightStack(stack, name, levelsKg) }
    }

    fun deleteWeightStack(stack: WeightStack) {
        viewModelScope.launch { repository.deleteWeightStack(stack) }
    }
}

class WeightStacksViewModelFactory(
    private val repository: TrainingPlanRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        require(modelClass.isAssignableFrom(WeightStacksViewModel::class.java))
        return WeightStacksViewModel(repository) as T
    }
}
