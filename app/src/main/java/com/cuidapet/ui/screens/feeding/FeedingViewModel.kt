package com.cuidadopet.ui.screens.feeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
import com.cuidadopet.data.repository.FeedingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanWithMeals(
    val plan: MealPlanEntity,
    val meals: List<MealEntity>
)

data class FeedingUiState(
    val plans: List<PlanWithMeals> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedingViewModel @Inject constructor(
    private val feedingRepository: FeedingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FeedingUiState())
    val state: StateFlow<FeedingUiState> = _state.asStateFlow()

    fun loadFeedingData(petId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            feedingRepository.getActiveMealPlans(petId)
                .flatMapLatest { plans ->
                    if (plans.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val planFlows = plans.map { plan ->
                            feedingRepository.getMealsForPlan(plan.id)
                                .map { meals -> PlanWithMeals(plan, meals) }
                        }
                        combine(planFlows) { it.toList() }
                    }
                }
                .collect { plansWithMeals ->
                    _state.value = FeedingUiState(plans = plansWithMeals, isLoading = false)
                }
        }
    }

    fun deletePlan(petId: Long, planId: Long) {
        viewModelScope.launch {
            feedingRepository.deleteMealPlan(petId, planId)
        }
    }
}
