package com.cuidadopet.ui.screens.feeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
import com.cuidadopet.data.db.entity.SporadicMealLogEntity
import com.cuidadopet.data.repository.FeedingRepository
import com.cuidadopet.domain.EnergyCalculator
import com.cuidadopet.domain.FeedingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class PlanWithMeals(
    val plan: MealPlanEntity,
    val meals: List<MealEntity>
)

data class FeedingUiState(
    val plans: List<PlanWithMeals> = emptyList(),
    val logs: Map<Long, MealLogEntity> = emptyMap(),
    val sporadicLogs: List<SporadicMealLogEntity> = emptyList(),
    val dailyStatus: FeedingStatus? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedingViewModel @Inject constructor(
    private val feedingRepository: FeedingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FeedingUiState())
    val state: StateFlow<FeedingUiState> = _state.asStateFlow()

    private val todayMillis: Long = startOfDay()

    fun loadFeedingData(petId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val plansWithMealsFlow = feedingRepository.getActiveMealPlans(petId)
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

            combine(
                plansWithMealsFlow,
                feedingRepository.getLogsForPetInPeriod(petId, todayMillis, todayMillis + 86_400_000L),
                feedingRepository.getSporadicLogsForDay(petId, todayMillis, todayMillis + 86_400_000L)
            ) { plansWithMeals, allLogs, sporadicLogs ->
                val logsMap = allLogs.associateBy { it.mealId }
                val allMeals = plansWithMeals.flatMap { it.meals }

                val status = if (allMeals.isEmpty()) null else {
                    val avg = allMeals.map { logsMap[it.id]?.eatenPercentage ?: 0 }.average().toInt()
                    EnergyCalculator.evaluateDailyIntake(avg)
                }

                FeedingUiState(
                    plans        = plansWithMeals,
                    logs         = logsMap,
                    sporadicLogs = sporadicLogs,
                    dailyStatus  = status,
                    isLoading    = false
                )
            }.collect { _state.value = it }
        }
    }

    fun logMeal(
        mealId: Long,
        eatenPercentage: Int,
        appetiteStatus: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val existingLog = feedingRepository
                .getLogForMealOnDate(mealId, todayMillis)
                .first()

            val log = MealLogEntity(
                id = existingLog?.id ?: 0L,
                mealId = mealId,
                date = todayMillis,
                eatenPercentage = eatenPercentage,
                appetiteStatus = appetiteStatus,
                notes = notes.ifBlank { null }
            )

            if (existingLog == null) feedingRepository.saveMealLog(log)
            else feedingRepository.updateMealLog(log)
        }
    }

    fun deletePlan(petId: Long, planId: Long) {
        viewModelScope.launch {
            feedingRepository.deleteMealPlan(petId, planId)
        }
    }

    private fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
