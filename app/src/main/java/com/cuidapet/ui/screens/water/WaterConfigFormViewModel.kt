package com.cuidadopet.ui.screens.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.WaterConfigEntity
import com.cuidadopet.data.repository.PetRepository
import com.cuidadopet.data.repository.WaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WaterConfigFormState(
    val petName: String = "",
    val dailyTargetMl: String = "",
    val reminderIntervalHours: String = "3",
    val reminderStartTime: String = "08:00",
    val remindersEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WaterConfigFormViewModel @Inject constructor(
    private val waterRepository: WaterRepository,
    private val petRepository: PetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WaterConfigFormState())
    val state: StateFlow<WaterConfigFormState> = _state.asStateFlow()

    fun load(petId: Long) {
        viewModelScope.launch {
            val pet = petRepository.getPetById(petId).first()
            _state.update { it.copy(petName = pet?.name ?: "") }

            val config = waterRepository.getWaterConfig(petId).first()
            if (config != null) {
                _state.update {
                    it.copy(
                        dailyTargetMl         = config.dailyTargetMl.toInt().toString(),
                        reminderIntervalHours = config.reminderIntervalHours.toString(),
                        reminderStartTime     = config.reminderStartTime,
                        remindersEnabled      = config.remindersEnabled
                    )
                }
            }
        }
    }

    fun updateTargetMl(value: String)              = _state.update { it.copy(dailyTargetMl = value) }
    fun updateReminderInterval(value: String)      = _state.update { it.copy(reminderIntervalHours = value) }
    fun updateReminderStartTime(value: String)     = _state.update { it.copy(reminderStartTime = value) }
    fun updateRemindersEnabled(value: Boolean)     = _state.update { it.copy(remindersEnabled = value) }

    fun save(petId: Long) {
        val s = _state.value

        val targetMl = s.dailyTargetMl.toDoubleOrNull()
        if (targetMl == null || targetMl < 50) {
            _state.update { it.copy(error = "Meta mínima: 50 ml") }
            return
        }
        if (targetMl > 20_000) {
            _state.update { it.copy(error = "Meta máxima: 20.000 ml") }
            return
        }
        val interval = s.reminderIntervalHours.toIntOrNull()
        if (interval == null || interval < 1) {
            _state.update { it.copy(error = "Intervalo mínimo de lembrete: 1 hora") }
            return
        }
        if (interval > 24) {
            _state.update { it.copy(error = "Intervalo máximo de lembrete: 24 horas") }
            return
        }

        _state.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                val config = WaterConfigEntity(
                    petId                 = petId,
                    dailyTargetMl         = targetMl,
                    reminderIntervalHours = s.reminderIntervalHours.toIntOrNull() ?: 3,
                    reminderStartTime     = s.reminderStartTime.ifBlank { "08:00" },
                    remindersEnabled      = s.remindersEnabled
                )
                waterRepository.saveWaterConfig(config, s.petName)
                _state.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Erro ao salvar: ${e.message}") }
            }
        }
    }

    fun delete(petId: Long) {
        viewModelScope.launch {
            waterRepository.deleteWaterConfig(petId)
            _state.update { it.copy(isDeleted = true) }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
