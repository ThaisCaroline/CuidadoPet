package com.cuidadopet.ui.screens.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.WaterConfigEntity
import com.cuidadopet.data.db.entity.WaterLogEntity
import com.cuidadopet.data.repository.WaterRepository
import com.cuidadopet.domain.HydrationStatus
import com.cuidadopet.domain.WaterCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// Estado da aba de hidratação do dia
data class WaterUiState(
    val config: WaterConfigEntity? = null,      // configuração de água do pet
    val totalMlToday: Double = 0.0,             // total bebido hoje em ml
    val logsToday: List<WaterLogEntity> = emptyList(), // histórico de registros do dia
    val hydrationStatus: HydrationStatus = HydrationStatus.NO_RECORD,
    val hydrationPercentage: Int = 0,           // 0–100+ (pode passar de 100)
    val isLoading: Boolean = true
)

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val waterRepository: WaterRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WaterUiState())
    val state: StateFlow<WaterUiState> = _state.asStateFlow()

    // Timestamps do início e fim do dia atual para consultas ao banco


    // Carrega a configuração e os logs de hoje.
    // Chamado quando a aba "Água" é aberta no dashboard.
    fun loadWaterData(petId: Long) {
        val dayStart = startOfDay() // pega o horário atual e zera horas/minutos/segundos/milissegundos — ou seja, retorna o timestamp da meia-noite de hoje (ex: 29/04/2026 00:00:00.000).
        val dayEnd   = dayStart + 86_400_000L - 1L //  86_400_000L - 1L 86_400_000 é o total de milissegundos em 24 horas (24h × 60min × 60s × 1000ms). Então dayEnd = meia-noite de hoje + 24h - 1ms = 29/04/2026 23:59:59.999. O -1L evita que pegue o primeiro milissegundo do dia seguinte.
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Combina config + total + lista de logs em um único estado reativo
            combine(
                waterRepository.getWaterConfig(petId),
                waterRepository.getTotalForDay(petId, dayStart, dayEnd),
                waterRepository.getLogsForDay(petId, dayStart, dayEnd)
            ) { config, totalMl, logs ->
                val target = config?.dailyTargetMl ?: 0.0
                WaterUiState(
                    config = config,
                    totalMlToday = totalMl,
                    logsToday = logs,
                    hydrationStatus = WaterCalculator.evaluateHydration(totalMl, target),
                    hydrationPercentage = WaterCalculator.hydrationPercentage(totalMl, target),
                    isLoading = false
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    // Registra um consumo de água.
    // amountMl = quanto o pet bebeu neste registro (ex: 150.0)
    fun addWaterLog(petId: Long, amountMl: Double, notes: String = "") {
        if (amountMl <= 0) return
        viewModelScope.launch {
            val log = WaterLogEntity(
                petId = petId,
                amountMl = amountMl,
                notes = notes.ifBlank { null }
            )
            waterRepository.addWaterLog(log)
        }
    }

    // Remove um registro incorreto (o tutor digitou o valor errado)
    fun deleteLog(logId: Long) {
        viewModelScope.launch {
            waterRepository.deleteWaterLog(logId)
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
