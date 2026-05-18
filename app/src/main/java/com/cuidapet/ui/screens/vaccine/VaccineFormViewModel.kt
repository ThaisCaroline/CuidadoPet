package com.cuidadopet.ui.screens.vaccine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.VaccineEntity
import com.cuidadopet.data.repository.PetRepository
import com.cuidadopet.data.repository.VaccineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

data class VaccineFormUiState(
    val type: String = "VACCINE",               // "VACCINE" | "DEWORMER"
    val name: String = "",
    val isAdministered: Boolean = true,
    val administeredAt: Long? = null,           // null = today if isAdministered
    val nextDueDate: Long? = null,
    val notes: String = "",
    val reminderEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class VaccineFormViewModel @Inject constructor(
    private val vaccineRepository: VaccineRepository,
    private val petRepository: PetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaccineFormUiState())
    val uiState: StateFlow<VaccineFormUiState> = _uiState.asStateFlow()

    private var petId: Long = 0

    fun init(petId: Long, vaccineId: Long?) {
        this.petId = petId
        if (vaccineId != null) {
            viewModelScope.launch {
                vaccineRepository.getById(vaccineId).first()?.let { vaccine ->
                    _uiState.update {
                        it.copy(
                            type            = vaccine.type,
                            name            = vaccine.name,
                            isAdministered  = vaccine.administeredAt != null,
                            administeredAt  = vaccine.administeredAt,
                            nextDueDate     = vaccine.nextDueDate,
                            notes           = vaccine.notes ?: "",
                            reminderEnabled = vaccine.reminderEnabled
                        )
                    }
                }
            }
        }
    }

    fun onTypeChange(type: String)          = _uiState.update { it.copy(type = type) }
    fun onNameChange(name: String)          = _uiState.update { it.copy(name = name) }
    fun onIsAdministeredChange(v: Boolean)  = _uiState.update { it.copy(isAdministered = v, administeredAt = if (v) (it.administeredAt ?: System.currentTimeMillis()) else null) }
    fun onAdministeredAtChange(ms: Long?)   = _uiState.update { it.copy(administeredAt = ms?.let { normalizeToLocalMidnight(it) }) }
    fun onNextDueDateChange(ms: Long?)      = _uiState.update { it.copy(nextDueDate = ms?.let { normalizeToLocalMidnight(it) }) }
    fun onNotesChange(notes: String)        = _uiState.update { it.copy(notes = notes) }
    fun onReminderEnabledChange(v: Boolean) = _uiState.update { it.copy(reminderEnabled = v) }
    fun clearError()                        = _uiState.update { it.copy(errorMessage = null) }

    // DatePickerDialog retorna UTC midnight; converte para meia-noite no fuso local para evitar exibir dia anterior
    private fun normalizeToLocalMidnight(utcMs: Long): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCal.timeInMillis = utcMs
        val local = Calendar.getInstance()
        local.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 9, 0, 0)
        local.set(Calendar.MILLISECOND, 0)
        return local.timeInMillis
    }

    fun save(existingId: Long?) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Informe o nome da vacina/vermífugo.") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val petName = petRepository.getPetById(petId).first()?.name ?: ""
                val vaccine = VaccineEntity(
                    id              = existingId ?: 0L,
                    petId           = petId,
                    type            = state.type,
                    name            = state.name.trim(),
                    administeredAt  = if (state.isAdministered) (state.administeredAt ?: System.currentTimeMillis()) else null,
                    nextDueDate     = state.nextDueDate,
                    notes           = state.notes.trim().ifBlank { null },
                    reminderEnabled = state.reminderEnabled
                )
                if (existingId == null) {
                    vaccineRepository.insert(vaccine, petName)
                } else {
                    vaccineRepository.update(vaccine, petName)
                }
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Erro ao salvar: ${e.message}") }
            }
        }
    }
}
