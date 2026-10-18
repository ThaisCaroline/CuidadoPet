package com.cuidadopet.ui.screens.medication

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.R
import com.cuidadopet.data.db.entity.MedicationEntity
import com.cuidadopet.data.repository.MedicationRepository
import com.cuidadopet.data.repository.PetRepository
import com.cuidadopet.domain.ParsedMedication
import com.cuidadopet.domain.PrescriptionParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Item da lista de revisão. Todos os campos são texto editável pelo tutor antes de salvar.
data class ScanItem(
    val id: Int,
    val name: String,
    val dose: String,
    val doseUnit: String,
    // "INTERVAL" (a cada X horas) ou "FIXED_TIMES" (horários fixos), igual ao MedicationEntity
    val frequencyType: String,
    // Horário da primeira dose, HH:mm (usado em INTERVAL)
    val startTime: String,
    // Horas entre as doses (usado em INTERVAL)
    val intervalHours: String,
    // Horários separados por vírgula, HH:mm (usado em FIXED_TIMES)
    val timesText: String,
    val durationDays: String,
    val needsReview: Boolean,
    // Já existe um medicamento ativo com o mesmo nome no pet
    val isDuplicate: Boolean,
    // false quando o nome não parece de medicamento; o item começa desmarcado
    val nameRecognized: Boolean = true,
    // false quando a receita não trouxe frequência: o intervalo começa vazio e o item não vem marcado
    val hasFrequency: Boolean = true,
    val sourceLine: String,
    val selected: Boolean = true
)

data class PrescriptionScanState(
    val showDisclaimer: Boolean = false,
    // Nomes (em minúsculas) dos medicamentos ativos do pet, para detectar duplicados
    val activeNames: List<String> = emptyList(),
    val isReading: Boolean = false,
    val hasScanned: Boolean = false,
    val items: List<ScanItem> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

// Aceita horários digitados de vários jeitos (8h, 8:30, às 8h...) e reconhece se são válidos
internal fun isValidTime(value: String): Boolean = PrescriptionParser.normalizeTime(value) != null

// Converte "8h, 20:00" em ["08:00", "20:00"]. Retorna null se algum horário for inválido ou se a lista for vazia/grande demais.
internal fun parseTimes(text: String): List<String>? {
    val normalized = text.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { PrescriptionParser.normalizeTime(it) ?: return null }
    return normalized.takeIf { it.isNotEmpty() && it.size <= 10 }
}

// Nome sem acento, sem maiúsculas e com espaços simples
internal fun normalizeMedicationName(name: String): String =
    java.text.Normalizer.normalize(name.lowercase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .trim()
        .replace(Regex("\\s+"), " ")

// Considera o mesmo medicamento: nomes iguais, ou um começa com o outro (ex.: "Dipirona" e "Dipirona Sódica").
// Nomes curtos (menos de 4 letras) só são comparados por igualdade, para evitar falsos positivos.
internal fun isSimilarMedicationName(a: String, b: String): Boolean {
    val x = normalizeMedicationName(a)
    val y = normalizeMedicationName(b)
    if (x.isEmpty() || y.isEmpty()) return false
    if (x == y) return true
    val shorter = if (x.length <= y.length) x else y
    val longer = if (shorter == x) y else x
    return shorter.length >= 4 && longer.startsWith(shorter)
}

@HiltViewModel
class PrescriptionScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val medicationRepository: MedicationRepository,
    private val petRepository: PetRepository
) : ViewModel() {

    // Aviso de "pode errar" aparece só na primeira vez que a tela abre
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        PrescriptionScanState(showDisclaimer = !prefs.getBoolean(KEY_DISCLAIMER_SEEN, false))
    )
    val uiState: StateFlow<PrescriptionScanState> = _uiState.asStateFlow()

    fun onDisclaimerAccepted() {
        prefs.edit().putBoolean(KEY_DISCLAIMER_SEEN, true).apply()
        _uiState.update { it.copy(showDisclaimer = false) }
    }

    // Pet da tela, para recarregar os ativos antes de cada leitura
    private var currentPetId: Long? = null

    // Carrega os medicamentos ativos do pet para avisar sobre duplicados
    fun loadActiveNames(petId: Long) {
        currentPetId = petId
        viewModelScope.launch { refreshActiveNames(petId) }
    }

    private suspend fun refreshActiveNames(petId: Long) {
        val names = medicationRepository.getActiveMedications(petId).first().map { it.name }
        _uiState.update { it.copy(activeNames = names) }
    }

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isReading = true, hasScanned = false, items = emptyList(), errorMessage = null, isSaved = false)
            }
            try {
                // Recarrega os ativos antes de comparar: evita perder duplicados se a tela carregou tarde
                currentPetId?.let { refreshActiveNames(it) }
                val text = recognizeText(uri)
                val activeNames = _uiState.value.activeNames
                // Interpretação do texto em segundo plano, fora da thread principal
                val parsedList = withContext(Dispatchers.Default) { PrescriptionParser.parse(text) }
                val items = parsedList.mapIndexed { index, parsed ->
                    val duplicate = activeNames.any { isSimilarMedicationName(it, parsed.name) }
                    parsed.toScanItem(id = index, duplicate = duplicate)
                }
                _uiState.update { it.copy(isReading = false, hasScanned = true, items = items) }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isReading = false,
                        hasScanned = true,
                        errorMessage = context.getString(R.string.scan_read_error)
                    )
                }
            }
        }
    }

    fun onToggleSelected(id: Int) = updateItem(id) { it.copy(selected = !it.selected) }
    fun onNameChange(id: Int, value: String) = updateItem(id) { it.copy(name = value) }
    fun onDoseChange(id: Int, value: String) = updateItem(id) { it.copy(dose = value) }
    fun onDurationChange(id: Int, value: String) = updateItem(id) {
        it.copy(durationDays = value.filter { c -> c.isDigit() }.take(4))
    }
    fun onStartTimeChange(id: Int, value: String) = updateItem(id) {
        it.copy(startTime = value.filter { c -> c.isLetterOrDigit() || c in ":. " }.take(12))
    }
    fun onIntervalHoursChange(id: Int, value: String) = updateItem(id) {
        it.copy(intervalHours = value.filter { c -> c.isDigit() }.take(3))
    }
    fun onTimesChange(id: Int, value: String) = updateItem(id) {
        it.copy(timesText = value.filter { c -> c.isLetterOrDigit() || c in ":,. " }.take(120))
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun saveSelected(petId: Long) {
        // Evita salvar duas vezes se o botão for tocado de novo durante o salvamento
        if (_uiState.value.isSaving) return
        val selected = _uiState.value.items.filter { it.selected }
        if (selected.isEmpty()) return

        val invalidFields = selected.any { item ->
            item.name.isBlank() || item.dose.isBlank() ||
                (item.durationDays.toIntOrNull() ?: 0) !in 1..3650
        }
        if (invalidFields) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.scan_invalid_items)) }
            return
        }

        val invalidSchedule = selected.any { !it.scheduleIsValid() }
        if (invalidSchedule) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.scan_invalid_schedule)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                // Mesmo limite da lista de medicamentos: conta os ativos antes de inserir
                val activeCount = medicationRepository.getActiveMedications(petId).first().size
                if (activeCount + selected.size > MAX_ACTIVE_MEDICATIONS) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = context.getString(R.string.scan_limit_error, MAX_ACTIVE_MEDICATIONS)
                        )
                    }
                    return@launch
                }

                val petName = petRepository.getPetById(petId).first()?.name
                    ?: context.getString(R.string.scan_default_pet_name)
                // Tudo ou nada: se o banco falhar em uma linha, nenhum medicamento da receita é salvo
                medicationRepository.insertMedications(selected.map { it.toEntity(petId) }, petName)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = context.getString(R.string.scan_save_error))
                }
            }
        }
    }

    private fun updateItem(id: Int, transform: (ScanItem) -> ScanItem) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.id == id) transform(it) else it })
        }
    }

    // Reconhecimento de texto no aparelho (ML Kit), sem enviar a foto para fora
    private suspend fun recognizeText(uri: Uri): String = suspendCancellableCoroutine { cont ->
        // Abre a imagem antes do reconhecedor: se ela não puder ser lida, não sobra reconhecedor aberto
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            cont.resumeWithException(e)
            return@suspendCancellableCoroutine
        }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        // Se a coroutine for cancelada (ex.: tela fechada), o reconhecedor também é fechado
        cont.invokeOnCancellation { recognizer.close() }
        recognizer.process(image)
            .addOnSuccessListener { result ->
                recognizer.close()
                cont.resume(result.text)
            }
            .addOnFailureListener { error ->
                recognizer.close()
                cont.resumeWithException(error)
            }
    }

    // Duplicado e nome desconhecido começam desmarcados para não gerar dose dobrada sem querer.
    // Sem dose na receita, também começa desmarcado: o tutor preenche antes de incluir.
    private fun ParsedMedication.toScanItem(id: Int, duplicate: Boolean) = ScanItem(
        id = id,
        name = name,
        dose = dose,
        doseUnit = doseUnit,
        frequencyType = frequencyType,
        startTime = startTime ?: "08:00",
        // Sem frequência na receita, o intervalo começa vazio: o app não inventa um intervalo
        intervalHours = if (hasFrequency) (frequencyHours ?: 24).toString() else "",
        timesText = fixedTimes.joinToString(", "),
        // Sem duração na receita, o campo começa vazio: o app não assume um tratamento
        durationDays = durationDays?.toString() ?: "",
        needsReview = needsReview,
        isDuplicate = duplicate,
        nameRecognized = nameRecognized,
        hasFrequency = hasFrequency,
        sourceLine = sourceLine,
        selected = !duplicate && nameRecognized && dose.isNotBlank() && hasFrequency
    )

    // Chamado quando o scanner de documentos não abre (ex.: aparelho sem Google Play Services)
    fun onScanFailed() {
        _uiState.update { it.copy(errorMessage = context.getString(R.string.scan_camera_error)) }
    }

    private fun ScanItem.scheduleIsValid(): Boolean =
        if (frequencyType == "INTERVAL") {
            isValidTime(startTime) && (intervalHours.toIntOrNull() ?: 0) in 1..720
        } else {
            parseTimes(timesText) != null
        }

    private fun ScanItem.toEntity(petId: Long): MedicationEntity {
        val days = durationDays.toInt()
        val isInterval = frequencyType == "INTERVAL"
        // Intervalo começa no horário escolhido de hoje; horários fixos começam à meia-noite (como no formulário)
        val startDate = if (isInterval) {
            val (hour, minute) = PrescriptionParser.normalizeTime(startTime)!!.split(":").map { it.toInt() }
            todayAt(hour, minute)
        } else {
            todayAt(0, 0)
        }
        val times = parseTimes(timesText).orEmpty()
        return MedicationEntity(
            petId = petId,
            name = name.trim(),
            form = "ORAL",
            dose = dose.trim(),
            doseUnit = doseUnit,
            frequencyType = frequencyType,
            frequencyHours = if (isInterval) intervalHours.toIntOrNull() else null,
            fixedTimes = if (!isInterval) "[${times.joinToString(",") { "\"$it\"" }}]" else null,
            startDate = startDate,
            endDate = startDate + days * DAY_MILLIS,
            isContinuous = false,
            administrationGuideline = "OTHER",
            observations = context.getString(R.string.scan_observation, sourceLine)
        )
    }

    private fun todayAt(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private companion object {
        const val DAY_MILLIS = 24L * 60 * 60 * 1000
        const val MAX_ACTIVE_MEDICATIONS = 20
        const val PREFS_NAME = "prescription_scan"
        const val KEY_DISCLAIMER_SEEN = "disclaimer_seen"
    }
}
