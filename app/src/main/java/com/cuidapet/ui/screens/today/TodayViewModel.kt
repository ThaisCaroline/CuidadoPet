package com.cuidadopet.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealLogEntity
import kotlinx.coroutines.flow.first
import com.cuidadopet.data.db.entity.MedicationEntity
import com.cuidadopet.data.db.entity.MedicationLogEntity
import com.cuidadopet.data.db.entity.WaterLogEntity
import com.cuidadopet.data.repository.FeedingRepository
import com.cuidadopet.data.repository.MedicationRepository
import com.cuidadopet.data.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// Representa uma dose de medicamento agendada para hoje
data class TodayDoseItem(
    val medication: MedicationEntity,
    val scheduledAt: Long,            // timestamp exato desta dose
    val log: MedicationLogEntity?     // null = ainda não registrada
)

// Representa uma refeição agendada para hoje
data class TodayMealItem(
    val meal: MealEntity,
    val log: MealLogEntity?           // null = ainda não registrada
)

data class TodayUiState(
    val doses: List<TodayDoseItem>   = emptyList(),
    val meals: List<TodayMealItem>   = emptyList(),
    val waterTotalMl: Double         = 0.0,
    val waterTargetMl: Double?       = null,
    val isLoading: Boolean           = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val feedingRepository: FeedingRepository,
    private val waterRepository: WaterRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    fun load(petId: Long) {
        viewModelScope.launch {
            val dayStart = startOfDay()
            val dayEnd   = dayStart + 86_400_000L - 1L  // 23:59:59.999 do mesmo dia

            // Refeições do plano ativo, reagindo quando o plano muda (ex: tutor reconfigura)
            // flatMapLatest cancela o collect anterior sempre que o plano mudar
            val mealsFlow = feedingRepository.getActiveMealPlan(petId)
                .flatMapLatest { plan ->
                    if (plan == null) flowOf(emptyList())
                    else feedingRepository.getMealsForPlan(plan.id)
                }

            // Combina todas as fontes de dados em um único estado reativo.
            // Sempre que qualquer flow emite um novo valor, o bloco de transformação
            // é reexecutado e a tela atualiza automaticamente.
            combine(
                medicationRepository.getActiveMedications(petId),
                medicationRepository.getLogsForPetInPeriod(petId, dayStart, dayEnd),
                mealsFlow,
                feedingRepository.getLogsForPetInPeriod(petId, dayStart, dayEnd),
                waterRepository.getTotalForDay(petId, dayStart, dayEnd)
            ) { meds, medLogs, meals, mealLogs, waterTotal ->
                // Monta a lista de doses do dia com o status de cada uma
                val doses = buildDoseItems(meds, medLogs, dayStart, dayEnd)
                // Associa cada refeição ao seu log de hoje (se houver)
                val mealItems = meals.map { meal ->
                    TodayMealItem(
                        meal = meal,
                        log  = mealLogs.find { it.mealId == meal.id && it.date == dayStart }
                    )
                }.sortedBy { it.meal.timeOfDay }

                Triple(doses, mealItems, waterTotal)
            }
            // Encadeia com o 6º flow (waterConfig) — combine() suporta até 5; usamos .combine()
            .combine(waterRepository.getWaterConfig(petId)) { (doses, mealItems, waterTotal), config ->
                TodayUiState(
                    doses         = doses,
                    meals         = mealItems,
                    waterTotalMl  = waterTotal,
                    waterTargetMl = config?.dailyTargetMl,
                    isLoading     = false
                )
            }
            .collect { newState -> _state.value = newState }
        }
    }

    // Registra o status de uma dose (TAKEN, NOT_TAKEN, VOMITED)
    fun markDose(medication: MedicationEntity, scheduledAt: Long, status: String) {
        viewModelScope.launch {
            medicationRepository.saveLog(
                MedicationLogEntity(
                    medicationId  = medication.id,
                    scheduledAt   = scheduledAt,
                    registeredAt  = System.currentTimeMillis(),
                    status        = status
                )
            )
        }
    }

    // Registra o resultado de uma refeição (quanto o pet comeu)
    fun markMeal(meal: MealEntity, eatenPercentage: Int, appetiteStatus: String) {
        viewModelScope.launch {
            val date = startOfDay()
            val existing = feedingRepository.getLogForMealOnDate(meal.id, date).first()
            if (existing != null) {
                feedingRepository.updateMealLog(
                    existing.copy(eatenPercentage = eatenPercentage, appetiteStatus = appetiteStatus,
                                  registeredAt = System.currentTimeMillis())
                )
            } else {
                feedingRepository.saveMealLog(
                    MealLogEntity(mealId = meal.id, date = date,
                                  eatenPercentage = eatenPercentage, appetiteStatus = appetiteStatus)
                )
            }
        }
    }

    // Registra água rápida (+ml) direto da aba "Hoje"
    fun addWater(petId: Long, amountMl: Double) {
        viewModelScope.launch {
            waterRepository.addWaterLog(
                WaterLogEntity(petId = petId, amountMl = amountMl)
            )
        }
    }

    // ─── Cálculo de doses do dia ──────────────────────────────────────────────

    // Constrói a lista de doses agendadas para hoje cruzando com os logs existentes
    private fun buildDoseItems(
        medications: List<MedicationEntity>,
        logs: List<MedicationLogEntity>,
        dayStart: Long,
        dayEnd: Long
    ): List<TodayDoseItem> {
        val items = mutableListOf<TodayDoseItem>()

        medications.forEach { med ->
            // Ignora medicamentos que ainda não começaram ou já terminaram hoje
            if (med.startDate > dayEnd) return@forEach
            if (med.endDate != null && med.endDate < dayStart) return@forEach

            calculateDosesForDay(med, dayStart, dayEnd).forEach { scheduledAt ->
                val log = logs.find {
                    it.medicationId == med.id && it.scheduledAt == scheduledAt
                }
                items.add(TodayDoseItem(med, scheduledAt, log))
            }
        }

        // Ordena por horário da dose para exibir na ordem cronológica
        return items.sortedBy { it.scheduledAt }
    }

    // Calcula os timestamps de cada dose de um medicamento que caem no dia de hoje.
    // A lógica varia dependendo do tipo de frequência do medicamento.
    private fun calculateDosesForDay(
        med: MedicationEntity,
        dayStart: Long,
        dayEnd: Long
    ): List<Long> {
        return when (med.frequencyType) {

            // FIXED_TIMES: horários fixos do dia — ex: ["08:00","20:00"]
            // Converte cada horário para o timestamp de hoje
            "FIXED_TIMES" -> {
                med.fixedTimes
                    ?.removePrefix("[")?.removeSuffix("]")
                    ?.split(",")
                    ?.map { it.trim().removeSurrounding("\"") }
                    ?.filter { it.isNotBlank() }
                    ?.mapNotNull { time ->
                        val parts = time.split(":").mapNotNull { it.toIntOrNull() }
                        if (parts.size != 2) return@mapNotNull null
                        val (h, m) = parts
                        // Timestamp = início do dia + horas em ms + minutos em ms
                        dayStart + h * 3_600_000L + m * 60_000L
                    }
                    ?: emptyList()
            }

            // INTERVAL: a cada X horas — ex: a cada 8h desde o início do tratamento
            // Calcula qual dose cai dentro do intervalo de hoje
            "INTERVAL" -> {
                val intervalMs = (med.frequencyHours ?: 24) * 3_600_000L
                val doses = mutableListOf<Long>()

                // Calcula o número de intervalos completos desde o início do tratamento
                // até o começo de hoje — assim pulamos diretamente para doses relevantes
                val elapsed   = dayStart - med.startDate
                val skipCount = if (elapsed <= 0L) 0L else elapsed / intervalMs
                var t         = med.startDate + skipCount * intervalMs

                // Avança até a primeira dose >= dayStart (pode estar um intervalo atrás)
                while (t < dayStart) t += intervalMs

                // Coleta todas as doses que caem dentro do dia
                while (t <= dayEnd) {
                    doses.add(t)
                    t += intervalMs
                }
                doses
            }

            else -> emptyList()
        }
    }

    // Timestamp de meia-noite do dia atual (00:00:00.000)
    private fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
