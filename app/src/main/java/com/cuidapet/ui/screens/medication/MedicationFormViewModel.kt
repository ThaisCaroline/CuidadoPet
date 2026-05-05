package com.cuidadopet.ui.screens.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.MedicationEntity
import com.cuidadopet.data.repository.MedicationRepository
import com.cuidadopet.data.repository.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class MedicationFormState(
    val name: String = "",
    val form: String = "ORAL",
    val dose: String = "",
    val doseUnit: String = "comprimido",
    val frequencyType: String = "INTERVAL",
    val frequencyHours: String = "8",
    val intervalStartTime: String = "",   // horário da primeira dose para intervalo — ex: "08:00"
    val fixedTimes: List<String> = listOf("08:00"),
    val isContinuous: Boolean = false,
    val durationDays: String = "7",
    val guideline: String = "OTHER",
    val guidelineDetail: String = "",
    val observations: String = "",
    val reminderEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val originalStartDate: Long? = null
)

@HiltViewModel
class MedicationFormViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val petRepository: PetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicationFormState())
    val uiState: StateFlow<MedicationFormState> = _uiState.asStateFlow()

    // Carrega medicamento existente para edição
    fun loadMedication(medicationId: Long) {
        viewModelScope.launch {
            medicationRepository.getMedicationById(medicationId).first()?.let { med ->
                val times = med.fixedTimes
                    ?.removeSurrounding("[", "]")
                    ?.split(",")
                    ?.map { it.trim().removeSurrounding("\"") }
                    ?.filter { it.isNotBlank() }
                    ?: listOf("08:00")

                // Reconstrói o horário inicial a partir do startDate salvo
                val loadedStartTime = if (med.frequencyType == "INTERVAL") {
                    val cal = Calendar.getInstance().apply { timeInMillis = med.startDate }
                    "%02d:%02d".format(
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE)
                    )
                } else ""

                val loadedDurationDays = if (!med.isContinuous && med.endDate != null) {
                    val days = ((med.endDate - med.startDate) / (24 * 60 * 60 * 1000L)).toInt()
                    if (days > 0) days.toString() else "7"
                } else "7"

                _uiState.update {
                    it.copy(
                        name              = med.name,
                        form              = med.form,
                        dose              = med.dose,
                        doseUnit          = med.doseUnit,
                        frequencyType     = med.frequencyType,
                        frequencyHours    = med.frequencyHours?.toString() ?: "8",
                        intervalStartTime = loadedStartTime,
                        fixedTimes        = times,
                        isContinuous      = med.isContinuous,
                        durationDays      = loadedDurationDays,
                        guideline         = med.administrationGuideline,
                        guidelineDetail   = med.guidelineDetail ?: "",
                        observations      = med.observations ?: "",
                        reminderEnabled   = med.reminderEnabled,
                        originalStartDate = med.startDate
                    )
                }
            }
        }
    }

    fun onNameChange(v: String)          = _uiState.update { it.copy(name = v) }
    fun onFormChange(v: String)          = _uiState.update { it.copy(form = v) }
    fun onDoseChange(v: String) {
        val sb = StringBuilder()
        var hasSep = false
        for (c in v) {
            when {
                c.isDigit() -> sb.append(c)
                (c == '.' || c == ',') && !hasSep -> { sb.append(c); hasSep = true }
                c == '/' && sb.isNotEmpty() && !hasSep -> { sb.append(c); hasSep = true }
            }
        }
        _uiState.update { it.copy(dose = sb.toString()) }
    }
    fun onDoseUnitChange(v: String)      = _uiState.update { it.copy(doseUnit = v) }
    fun onFrequencyTypeChange(v: String) = _uiState.update { it.copy(frequencyType = v) }
    fun onFrequencyHoursChange(v: String)= _uiState.update { it.copy(frequencyHours = v) }
    fun onIntervalStartTimeChange(v: String) = _uiState.update { it.copy(intervalStartTime = v) }
    fun onContinuousChange(v: Boolean)   = _uiState.update { it.copy(isContinuous = v) }
    fun onDurationDaysChange(v: String)  = _uiState.update { it.copy(durationDays = v) }
    fun onGuidelineChange(v: String)       = _uiState.update { it.copy(guideline = v) }
    fun onGuidelineDetailChange(v: String) = _uiState.update { it.copy(guidelineDetail = v) }
    fun onObservationsChange(v: String)    = _uiState.update { it.copy(observations = v) }
    fun onReminderEnabledChange(v: Boolean) = _uiState.update { it.copy(reminderEnabled = v) }
    fun clearError()                       = _uiState.update { it.copy(errorMessage = null) }

    // Atualiza um horário fixo específico pelo índice — aplica auto-formato HH:mm
    fun onFixedTimeChange(index: Int, value: String) {
        _uiState.update { state ->
            val updated = state.fixedTimes.toMutableList()
            if (index < updated.size) updated[index] = value
            state.copy(fixedTimes = updated)
        }
    }

    // Insere ':' automaticamente conforme o tutor digita.
    // Hora com um algarismo (ex: 3-9): "8" → "8", "83" → "8:3", "830" → "8:30"
    // Hora com dois algarismos (ex: 0-2): "08" → "08:", "083" → "08:3", "0830" → "08:30"
    private fun autoFormatTime(raw: String): String {
        if (Regex("""^\d{1,2}:\d{0,2}$""").matches(raw)) return raw
        val digits = raw.filter { it.isDigit() }.take(4)
        if (digits.isEmpty()) return ""
        return when (digits.length) {
            1 -> digits
            2 -> if (digits[0].digitToInt() >= 3) "${digits[0]}:${digits[1]}"
                 else "${digits[0]}${digits[1]}:"
            3 -> if (digits[0].digitToInt() >= 3) "${digits[0]}:${digits[1]}${digits[2]}"
                 else "${digits[0]}${digits[1]}:${digits[2]}"
            else -> "${digits[0]}${digits[1]}:${digits[2]}${digits[3]}"
        }
    }

    // Completa horários parciais: "8" → "8:00", "08:" → "08:00", "8:3" → "8:30"
    private fun normalizeTime(raw: String): String {
        val s = raw.trim()
        return when {
            Regex("""^\d{1,2}:\d{2}$""").matches(s) -> s
            Regex("""^\d{1,2}:\d$""").matches(s)    -> "${s}0"
            Regex("""^\d{1,2}:$""").matches(s)      -> "${s}00"
            Regex("""^\d{1,2}$""").matches(s)       -> "$s:00"
            else -> s
        }
    }

    // Converte "HH:mm" para timestamp de hoje naquele horário
    private fun parseStartTime(timeStr: String): Long {
        if (!Regex("""^\d{1,2}:\d{2}$""").matches(timeStr)) return System.currentTimeMillis()
        val parts = timeStr.split(":")
        val h = parts[0].toIntOrNull() ?: return System.currentTimeMillis()
        val m = parts[1].toIntOrNull() ?: return System.currentTimeMillis()
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Adiciona um novo horário fixo
    fun addFixedTime() {
        _uiState.update { it.copy(fixedTimes = it.fixedTimes + "12:00") }
    }

    // Remove um horário fixo pelo índice
    fun removeFixedTime(index: Int) {
        _uiState.update { state ->
            val updated = state.fixedTimes.toMutableList()
            if (updated.size > 1) updated.removeAt(index)  // mantém ao menos 1 horário
            state.copy(fixedTimes = updated)
        }
    }

    fun saveMedication(petId: Long, existingMedicationId: Long? = null) {
        // Normaliza horários incompletos antes de validar: "8" → "8:00", "08:" → "08:00"
        _uiState.update { s ->
            s.copy(
                fixedTimes        = s.fixedTimes.map { normalizeTime(it) },
                intervalStartTime = normalizeTime(s.intervalStartTime)
            )
        }
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "O nome do medicamento é obrigatório") }
            return
        }
        if (state.name.length > 100) {
            _uiState.update { it.copy(errorMessage = "O nome deve ter no máximo 100 caracteres") }
            return
        }
        if (state.dose.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Informe a dose") }
            return
        }
        if (state.dose.length > 200) {
            _uiState.update { it.copy(errorMessage = "Descrição da dose muito longa (máx. 200 caracteres)") }
            return
        }
        if (state.frequencyType == "INTERVAL") {
            val hours = state.frequencyHours.toIntOrNull()
            if (hours == null || hours < 1) {
                _uiState.update { it.copy(errorMessage = "Intervalo mínimo: 1 hora") }
                return
            }
            if (hours > 720) {
                _uiState.update { it.copy(errorMessage = "Intervalo máximo: 720 horas (30 dias)") }
                return
            }
        }
        if (state.frequencyType == "FIXED_TIMES") {
            val timeRegex = Regex("""^\d{2}:\d{2}$""")
            if (state.fixedTimes.any { !it.matches(timeRegex) }) {
                _uiState.update { it.copy(errorMessage = "Horários devem estar no formato HH:mm") }
                return
            }
            if (state.fixedTimes.size > 10) {
                _uiState.update { it.copy(errorMessage = "Máximo de 10 horários fixos por medicamento") }
                return
            }
        }
        if (!state.isContinuous) {
            val days = state.durationDays.toIntOrNull()
            if (days == null || days < 1) {
                _uiState.update { it.copy(errorMessage = "Duração mínima: 1 dia") }
                return
            }
            if (days > 3650) {
                _uiState.update { it.copy(errorMessage = "Duração máxima: 3.650 dias (10 anos)") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Busca o nome do pet para personalizar a notificação
            val petName = petRepository.getPetById(petId).first()?.name ?: "seu pet"

            val startDate = when {
                existingMedicationId != null && state.originalStartDate != null -> state.originalStartDate
                state.frequencyType == "INTERVAL" && state.intervalStartTime.isNotBlank() -> parseStartTime(state.intervalStartTime)
                else -> System.currentTimeMillis()
            }
            val endDate = if (state.isContinuous) null
                          else startDate + (state.durationDays.toLongOrNull() ?: 7) * 24 * 60 * 60 * 1000L

            val medication = MedicationEntity(
                id = existingMedicationId ?: 0L,
                petId = petId,
                name = state.name.trim(),
                form = state.form,
                dose = state.dose.trim(),
                doseUnit = state.doseUnit.trim(),
                frequencyType = state.frequencyType,
                frequencyHours = state.frequencyHours.toIntOrNull(),
                fixedTimes = if (state.frequencyType == "FIXED_TIMES")
                    "[${state.fixedTimes.joinToString(",") { "\"$it\"" }}]"
                else null,
                startDate = startDate,
                endDate = endDate,
                isContinuous = state.isContinuous,
                administrationGuideline = state.guideline,
                guidelineDetail = state.guidelineDetail.ifBlank { null },
                observations = state.observations.ifBlank { null },
                reminderEnabled = state.reminderEnabled
            )

            if (existingMedicationId != null) {
                medicationRepository.updateMedication(medication, petName)
            } else {
                medicationRepository.insertMedication(medication, petName)
            }

            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}
