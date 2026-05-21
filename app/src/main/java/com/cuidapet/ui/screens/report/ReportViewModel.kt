package com.cuidadopet.ui.screens.report

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MedicationLogEntity
import com.cuidadopet.data.db.entity.SporadicMealLogEntity
import com.cuidadopet.data.db.entity.WaterLogEntity
import com.cuidadopet.data.db.entity.WeightRecordEntity
import com.cuidadopet.data.repository.FeedingRepository
import com.cuidadopet.data.repository.HealthRepository
import com.cuidadopet.data.repository.MedicationRepository
import com.cuidadopet.data.repository.PurchaseRepository
import com.cuidadopet.data.repository.ReportRepository
import com.cuidadopet.data.repository.WaterRepository
import com.cuidadopet.domain.PetReport
import com.cuidadopet.domain.ReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class ReportUiState(
    val isLoading: Boolean       = true,
    val report: PetReport?       = null,
    val pdfFile: File?           = null,
    val isPdfGenerating: Boolean = false,
    val error: String?           = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val medicationRepository: MedicationRepository,
    private val feedingRepository: FeedingRepository,
    private val waterRepository: WaterRepository,
    private val healthRepository: HealthRepository,
    private val purchaseRepository: PurchaseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    val isPremium = purchaseRepository.isPremium

    private var selectedDays = 7
    private var currentPetId: Long = 0

    fun load(petId: Long, days: Int = selectedDays) {
        currentPetId = petId
        selectedDays = days
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, pdfFile = null) }
            try {
                val report = reportRepository.buildReport(petId, days)
                if (report == null) {
                    _state.update { it.copy(isLoading = false, error = "Pet não encontrado.") }
                } else {
                    _state.update { it.copy(isLoading = false, report = report) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Erro ao carregar dados: ${e.message}") }
            }
        }
    }

    fun updateMedicationLog(log: MedicationLogEntity) {
        viewModelScope.launch {
            medicationRepository.updateLog(log)
            load(currentPetId, selectedDays)
        }
    }

    fun updateMealLog(log: MealLogEntity) {
        viewModelScope.launch {
            feedingRepository.updateMealLog(log)
            load(currentPetId, selectedDays)
        }
    }

    fun updateWaterLog(log: WaterLogEntity) {
        viewModelScope.launch {
            waterRepository.updateWaterLog(log)
            load(currentPetId, selectedDays)
        }
    }

    fun updateWeightRecord(record: WeightRecordEntity) {
        viewModelScope.launch {
            healthRepository.updateWeightRecord(record)
            load(currentPetId, selectedDays)
        }
    }

    fun deleteMedicationLog(logId: Long) {
        viewModelScope.launch {
            medicationRepository.deleteLog(logId)
            load(currentPetId, selectedDays)
        }
    }

    fun deleteMealLog(logId: Long) {
        viewModelScope.launch {
            feedingRepository.deleteMealLog(logId)
            load(currentPetId, selectedDays)
        }
    }

    fun deleteWaterLog(logId: Long) {
        viewModelScope.launch {
            waterRepository.deleteWaterLog(logId)
            load(currentPetId, selectedDays)
        }
    }

    fun deleteWeightRecord(recordId: Long) {
        viewModelScope.launch {
            healthRepository.deleteWeightRecord(recordId)
            load(currentPetId, selectedDays)
        }
    }

    fun updateWaterDailyTotal(logsForDay: List<WaterLogEntity>, newTotalMl: Double) {
        val diff    = newTotalMl - logsForDay.sumOf { it.amountMl }
        val lastLog = logsForDay.maxByOrNull { it.registeredAt } ?: return
        val newAmount = lastLog.amountMl + diff
        if (newAmount < 0) {
            _state.update { it.copy(error = "O total não pode ser menor que as outras entradas do dia.") }
            return
        }
        viewModelScope.launch {
            waterRepository.updateWaterLog(lastLog.copy(amountMl = newAmount))
            load(currentPetId, selectedDays)
        }
    }

    fun updateFoodDailyTotal(
        mealLogsForDay: List<MealLogEntity>,
        sporadicLogsForDay: List<SporadicMealLogEntity>,
        newTotalGrams: Double
    ) {
        val report    = _state.value.report ?: return
        val mealsById = report.meals.associateBy { it.id }

        val currentTotal = mealLogsForDay.sumOf { log ->
            (mealsById[log.mealId]?.quantityGrams ?: 0.0) * log.eatenPercentage / 100.0
        } + sporadicLogsForDay.sumOf { it.amountGrams ?: 0.0 }

        val diff         = newTotalGrams - currentTotal
        val lastSporadic = sporadicLogsForDay.filter { it.amountGrams != null }
            .maxByOrNull { it.registeredAt }

        if (lastSporadic != null) {
            val newAmount = (lastSporadic.amountGrams ?: 0.0) + diff
            if (newAmount < 0) {
                _state.update { it.copy(error = "O total não pode ser menor que as outras entradas do dia.") }
                return
            }
            viewModelScope.launch {
                feedingRepository.updateSporadicLog(lastSporadic.copy(amountGrams = newAmount))
                load(currentPetId, selectedDays)
            }
        } else if (mealLogsForDay.isNotEmpty()) {
            val lastMealLog = mealLogsForDay.last()
            val meal        = mealsById[lastMealLog.mealId] ?: return
            if (meal.quantityGrams <= 0) return
            val othersTotal = mealLogsForDay.dropLast(1).sumOf { log ->
                (mealsById[log.mealId]?.quantityGrams ?: 0.0) * log.eatenPercentage / 100.0
            }
            val targetForLast = newTotalGrams - othersTotal
            if (targetForLast < 0) {
                _state.update { it.copy(error = "O total não pode ser menor que as outras entradas do dia.") }
                return
            }
            val newPct = ((targetForLast / meal.quantityGrams) * 100).toInt().coerceIn(0, 100)
            viewModelScope.launch {
                feedingRepository.updateMealLog(lastMealLog.copy(eatenPercentage = newPct))
                load(currentPetId, selectedDays)
            }
        }
    }

    fun getShareText(): String? =
        _state.value.report?.let { ReportGenerator.generateText(it) }

    fun generatePdf() {
        val report = _state.value.report ?: return
        viewModelScope.launch {
            _state.update { it.copy(isPdfGenerating = true) }
            try {
                val file = withContext(Dispatchers.IO) {
                    ReportGenerator.generatePdf(context, report)
                }
                _state.update { it.copy(isPdfGenerating = false, pdfFile = file) }
            } catch (e: Exception) {
                _state.update { it.copy(isPdfGenerating = false, error = "Erro ao gerar PDF: ${e.message}") }
            }
        }
    }

    fun clearPdfFile() {
        _state.update { it.copy(pdfFile = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
