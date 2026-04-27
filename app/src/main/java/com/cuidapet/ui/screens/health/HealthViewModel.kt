package com.cuidadopet.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.HealthEntryEntity
import com.cuidadopet.data.db.entity.WeightRecordEntity
import com.cuidadopet.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado da aba de saúde
data class HealthUiState(
    val entries: List<HealthEntryEntity> = emptyList(),   // histórico do diário
    val latestWeight: WeightRecordEntity? = null,         // peso mais recente
    val isLoading: Boolean = true
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HealthUiState())
    val state: StateFlow<HealthUiState> = _state.asStateFlow()

    // Carrega entradas do diário e o peso mais recente do pet
    fun loadHealthData(petId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            combine(
                healthRepository.getAllEntries(petId),
                healthRepository.getLatestWeight(petId)
            ) { entries, latestWeight ->
                HealthUiState(
                    entries = entries,
                    latestWeight = latestWeight,
                    isLoading = false
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    // Remove uma entrada do diário
    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            healthRepository.deleteEntry(entryId)
        }
    }
}
