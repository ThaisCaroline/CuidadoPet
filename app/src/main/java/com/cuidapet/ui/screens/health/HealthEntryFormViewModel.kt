package com.cuidadopet.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.HealthEntryEntity
import com.cuidadopet.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado do formulário de entrada do diário de saúde.
// Todos os campos clínicos são nullable — o tutor só preenche o que observou.
data class HealthEntryFormState(
    val behavior: String? = null,       // comportamento geral
    val fecesStatus: String? = null,    // estado das fezes
    val urineStatus: String? = null,    // estado da urina
    val vomitCount: String = "",        // campo de texto — convertido para Int ao salvar
    val mobility: String? = null,       // mobilidade
    val painSigns: String? = null,      // sinais de dor
    val observations: String = "",      // texto livre
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HealthEntryFormViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HealthEntryFormState())
    val state: StateFlow<HealthEntryFormState> = _state.asStateFlow()

    // Carrega uma entrada existente para edição (entryId != null)
    // Quando entryId é null, o formulário está em modo de criação
    fun loadEntry(petId: Long, entryId: Long?) {
        if (entryId == null) return  // modo de criação — formulário já vazio
        viewModelScope.launch {
            val entries = healthRepository.getAllEntries(petId).first()
            val entry = entries.find { it.id == entryId } ?: return@launch
            _state.update {
                it.copy(
                    behavior = entry.behavior,
                    fecesStatus = entry.fecesStatus,
                    urineStatus = entry.urineStatus,
                    vomitCount = entry.vomitCount?.toString() ?: "",
                    mobility = entry.mobility,
                    painSigns = entry.painSigns,
                    observations = entry.observations ?: ""
                )
            }
        }
    }

    // ─── Funções de atualização — uma por campo ─────────────────────────────

    fun updateBehavior(value: String?)    = _state.update { it.copy(behavior = value) }
    fun updateFeces(value: String?)       = _state.update { it.copy(fecesStatus = value) }
    fun updateUrine(value: String?)       = _state.update { it.copy(urineStatus = value) }
    fun updateVomitCount(value: String)   = _state.update { it.copy(vomitCount = value) }
    fun updateMobility(value: String?)    = _state.update { it.copy(mobility = value) }
    fun updatePainSigns(value: String?)   = _state.update { it.copy(painSigns = value) }
    fun updateObservations(value: String) = _state.update { it.copy(observations = value) }

    fun save(petId: Long, entryId: Long?) {
        val s = _state.value

        // Exige ao menos uma observação para não salvar entrada vazia
        val hasAnyData = s.behavior != null || s.fecesStatus != null ||
            s.urineStatus != null || s.vomitCount.isNotBlank() ||
            s.mobility != null || s.painSigns != null ||
            s.observations.isNotBlank()

        if (!hasAnyData) {
            _state.update { it.copy(error = "Preencha ao menos um campo antes de salvar.") }
            return
        }
        if (s.vomitCount.isNotBlank()) {
            val count = s.vomitCount.toIntOrNull()
            if (count == null || count < 0) {
                _state.update { it.copy(error = "Número de vômitos inválido") }
                return
            }
            if (count > 100) {
                _state.update { it.copy(error = "Número de vômitos muito alto — verifique o valor") }
                return
            }
        }
        if (s.observations.length > 2000) {
            _state.update { it.copy(error = "Observações muito longas (máx. 2.000 caracteres)") }
            return
        }

        _state.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                val entry = HealthEntryEntity(
                    id = entryId ?: 0L,
                    petId = petId,
                    behavior = s.behavior,
                    fecesStatus = s.fecesStatus,
                    urineStatus = s.urineStatus,
                    vomitCount = s.vomitCount.toIntOrNull(),
                    mobility = s.mobility,
                    painSigns = s.painSigns,
                    observations = s.observations.ifBlank { null }
                )

                if (entryId == null) {
                    healthRepository.saveEntry(entry)
                } else {
                    healthRepository.updateEntry(entry)
                }

                _state.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Erro ao salvar: ${e.message}") }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
