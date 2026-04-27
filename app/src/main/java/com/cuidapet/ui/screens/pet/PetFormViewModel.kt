package com.cuidadopet.ui.screens.pet

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.PetEntity
import com.cuidadopet.data.db.entity.WeightRecordEntity
import com.cuidadopet.data.repository.HealthRepository
import com.cuidadopet.data.repository.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import javax.inject.Inject

data class PetFormState(
    val name: String = "",
    val species: String = "DOG",
    val breed: String = "",
    val weightKg: String = "",
    val sex: String = "MALE",
    val isNeutered: Boolean = false,
    val clinicalStates: Set<String> = setOf("ACTIVE_TREATMENT"),
    val photoPath: String? = null,     // caminho da foto no armazenamento interno
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PetFormViewModel @Inject constructor(
    private val petRepository: PetRepository,
    private val healthRepository: HealthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetFormState())
    val uiState: StateFlow<PetFormState> = _uiState.asStateFlow()

    fun loadPet(petId: Long) {
        viewModelScope.launch {
            petRepository.getPetById(petId).collect { pet ->
                if (pet != null) {
                    _uiState.update {
                        it.copy(
                            name           = pet.name,
                            species        = pet.species,
                            breed          = pet.breed ?: "",
                            weightKg       = formatWeightForDisplay(pet.weightKg),
                            sex            = pet.sex,
                            isNeutered     = pet.isNeutered,
                            clinicalStates = parseClinicalStates(pet.clinicalStates),
                            photoPath      = pet.photoPath
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String)    = _uiState.update { it.copy(name = value) }
    fun onSpeciesChange(value: String) = _uiState.update { it.copy(species = value) }
    fun onBreedChange(value: String)   = _uiState.update { it.copy(breed = value) }

    fun onWeightChange(value: String) {
        val sb     = StringBuilder()
        var hasSep = false
        for (c in value) {
            when {
                c.isDigit()                        -> sb.append(c)
                (c == '.' || c == ',') && !hasSep  -> { sb.append(','); hasSep = true }
            }
        }
        _uiState.update { it.copy(weightKg = sb.toString()) }
    }

    // Formata o Double para exibição: 4.3 → "4,3" · 43.0 → "43" · 4.35 → "4,35"
    private fun formatWeightForDisplay(kg: Double): String =
        if (kg % 1.0 == 0.0) kg.toLong().toString()
        else kg.toBigDecimal().stripTrailingZeros().toPlainString().replace('.', ',')
    fun onSexChange(value: String)           = _uiState.update { it.copy(sex = value) }
    fun onNeuteredChange(value: Boolean)     = _uiState.update { it.copy(isNeutered = value) }

    fun onClinicalStateToggle(state: String) {
        _uiState.update { current ->
            val updated = if (state in current.clinicalStates)
                current.clinicalStates - state
            else
                current.clinicalStates + state
            current.copy(clinicalStates = updated)
        }
    }

    // Chamado quando o tutor escolhe uma foto da galeria.
    // Copia o arquivo para o armazenamento interno do app (filesDir/photos/).
    // Isso é necessário porque a URI da galeria pode ficar inválida depois,
    // especialmente se o arquivo original for movido ou deletado.
    fun onPhotoFromGallery(uri: Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { copyPhotoToInternalStorage(uri) }
            if (path != null) _uiState.update { it.copy(photoPath = path) }
        }
    }

    // Chamado após a câmera retornar com sucesso.
    // O arquivo já está no caminho correto (criado pelo createCameraFile()).
    fun onPhotoFromCamera(filePath: String) {
        _uiState.update { it.copy(photoPath = filePath) }
    }

    // Cria o arquivo temporário onde a câmera vai salvar a foto.
    // Retorna o File para que a composable possa criar o URI via FileProvider.
    fun createCameraFile(): File {
        val dir = File(context.filesDir, "photos").also { it.mkdirs() }
        return File(dir, "photo_${System.currentTimeMillis()}.jpg")
    }

    fun savePet(existingPetId: Long? = null) {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "O nome do pet é obrigatório") }
            return
        }
        if (state.name.length > 100) {
            _uiState.update { it.copy(errorMessage = "O nome deve ter no máximo 100 caracteres") }
            return
        }
        if (state.breed.length > 100) {
            _uiState.update { it.copy(errorMessage = "A raça deve ter no máximo 100 caracteres") }
            return
        }
        val weight = state.weightKg.replace(",", ".").toDoubleOrNull()
        if (weight == null || weight < 0.1) {
            _uiState.update { it.copy(errorMessage = "Informe um peso válido (mínimo 0,1 kg)") }
            return
        }
        if (weight > 200) {
            _uiState.update { it.copy(errorMessage = "Peso máximo permitido: 200 kg") }
            return
        }
        if (state.clinicalStates.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Selecione ao menos um estado clínico") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val pet = PetEntity(
                id             = existingPetId ?: 0L,
                name           = state.name.trim(),
                species        = state.species,
                breed          = state.breed.trim().ifBlank { null },
                weightKg       = weight,
                sex            = state.sex,
                isNeutered     = state.isNeutered,
                clinicalStates = formatClinicalStates(state.clinicalStates),
                photoPath      = state.photoPath
            )

            val petId = if (existingPetId != null) {
                petRepository.updatePet(pet)
                existingPetId
            } else {
                petRepository.insertPet(pet)
            }

            // Sincroniza o peso com o histórico (evita duplicata no mesmo dia)
            val today = startOfDay()
            val records = healthRepository.getAllWeightRecords(petId).first()
            if (records.none { it.date == today }) {
                healthRepository.saveWeightRecord(
                    WeightRecordEntity(petId = petId, date = today, weightKg = weight)
                )
            }

            _uiState.update { it.copy(isLoading = false, isSaved = true) }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun startOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0);      cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // Copia a imagem da URI (galeria) para filesDir/photos/ e retorna o caminho.
    // Retorna null se a cópia falhar.
    private fun copyPhotoToInternalStorage(uri: Uri): String? {
        return try {
            val dir  = File(context.filesDir, "photos").also { it.mkdirs() }
            val dest = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            dest.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun parseClinicalStates(json: String): Set<String> {
        return json.removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun formatClinicalStates(states: Set<String>): String {
        return "[${states.joinToString(",") { "\"$it\"" }}]"
    }
}
