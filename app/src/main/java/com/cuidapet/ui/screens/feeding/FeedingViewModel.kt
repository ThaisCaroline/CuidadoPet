package com.cuidadopet.ui.screens.feeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
import com.cuidadopet.data.repository.FeedingRepository
import com.cuidadopet.domain.EnergyCalculator
import com.cuidadopet.domain.FeedingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// Estado da aba de alimentação do dia
data class FeedingUiState(
    val plan: MealPlanEntity? = null,               // plano alimentar ativo
    val meals: List<MealEntity> = emptyList(),      // refeições do plano
    val logs: Map<Long, MealLogEntity> = emptyMap(),// logs de hoje: mealId → log
    val dailyStatus: FeedingStatus? = null,         // resumo do dia (suficiente, abaixo, etc.)
    val isLoading: Boolean = true
)

@HiltViewModel
class FeedingViewModel @Inject constructor(
    private val feedingRepository: FeedingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FeedingUiState())
    val state: StateFlow<FeedingUiState> = _state.asStateFlow()

    // Timestamp de meia-noite do dia atual — chave para consultar os logs de hoje
    private val todayMillis: Long = startOfDay()

    // Carrega o plano ativo e os logs de hoje para o pet.
    // Chamado quando a aba "Alimentação" é aberta no dashboard.
    fun loadFeedingData(petId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Observa o plano ativo do pet em tempo real
            feedingRepository.getActiveMealPlan(petId).collect { plan ->
                if (plan == null) {
                    // Sem plano configurado ainda
                    _state.update { it.copy(plan = null, meals = emptyList(), isLoading = false) }
                    return@collect
                }

                // Carrega as refeições e seus logs de hoje juntos
                combine(
                    feedingRepository.getMealsForPlan(plan.id),
                    feedingRepository.getLogsForPetInPeriod(petId, todayMillis, todayMillis + 86_400_000L)
                ) { meals, allLogs ->
                    // Monta um mapa de mealId → log para acesso rápido na tela
                    val logsMap = allLogs.associateBy { it.mealId }

                    // Calcula o status do dia com base nos logs existentes
                    val status = if (meals.isEmpty()) null else {
                        val percentages = meals.map { meal ->
                            logsMap[meal.id]?.eatenPercentage ?: 0
                        }
                        // Média de porcentagem comida em todas as refeições do dia
                        val avg = percentages.average().toInt()
                        EnergyCalculator.evaluateDailyIntake(avg)
                    }

                    FeedingUiState(
                        plan = plan,
                        meals = meals,
                        logs = logsMap,
                        dailyStatus = status,
                        isLoading = false
                    )
                }.collect { newState ->
                    _state.value = newState
                }
            }
        }
    }

    // Salva ou atualiza o registro de uma refeição.
    // appetiteStatus: "ALL", "PARTIAL" ou "REFUSED"
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
                id = existingLog?.id ?: 0L,        // 0 = novo registro; != 0 = atualização
                mealId = mealId,
                date = todayMillis,
                eatenPercentage = eatenPercentage,
                appetiteStatus = appetiteStatus,
                notes = notes.ifBlank { null }
            )

            if (existingLog == null) {
                feedingRepository.saveMealLog(log)
            } else {
                feedingRepository.updateMealLog(log)
            }
        }
    }

    // Retorna o timestamp de meia-noite do dia de hoje.
    // Usado como chave de data nas consultas ao banco.
    private fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
