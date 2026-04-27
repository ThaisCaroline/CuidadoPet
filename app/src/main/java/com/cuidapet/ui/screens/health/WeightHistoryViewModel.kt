package com.cuidadopet.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.WeightRecordEntity
import com.cuidadopet.data.repository.HealthRepository
import com.cuidadopet.data.repository.PetRepository
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class WeightHistoryUiState(
    val records: List<WeightRecordEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class WeightHistoryViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val petRepository: PetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WeightHistoryUiState())
    val state: StateFlow<WeightHistoryUiState> = _state.asStateFlow()

    // Carrega todos os registros de peso em ordem cronológica (ASC no DAO)
    fun load(petId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            healthRepository.getAllWeightRecords(petId).collect { records ->
                _state.update { it.copy(records = records, isLoading = false) }
            }
        }
    }

    fun addWeight(petId: Long, weightKg: Double, notes: String) {
        viewModelScope.launch {
            healthRepository.saveWeightRecord(
                WeightRecordEntity(petId = petId, date = startOfDay(), weightKg = weightKg, notes = notes.ifBlank { null })
            )
            // Atualiza o peso atual do pet para manter sincronizado com o cadastro
            petRepository.getPetById(petId).first()?.let { pet ->
                petRepository.updatePet(pet.copy(weightKg = weightKg))
            }
        }
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            healthRepository.deleteWeightRecord(recordId)
        }
    }

    // Timestamp de meia-noite do dia atual — data padrão para os registros
    private fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
