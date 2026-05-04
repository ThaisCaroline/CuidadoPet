package com.cuidadopet.ui.screens.health

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.HealthEntryEntity
import com.cuidadopet.data.db.entity.HealthPhotoEntity
import com.cuidadopet.data.repository.HealthPhotoRepository
import com.cuidadopet.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HealthEntryFormState(
    val behavior: String? = null,
    val fecesStatus: String? = null,
    val urineStatus: String? = null,
    val vomitCount: String = "",
    val mobility: String? = null,
    val painSigns: String? = null,
    val observations: String = "",
    val registeredAt: Long? = null,
    val photos: List<HealthPhotoEntity> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HealthEntryFormViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val photoRepository: HealthPhotoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HealthEntryFormState())
    val state: StateFlow<HealthEntryFormState> = _state.asStateFlow()

    private var currentPetId: Long = 0
    private var currentEntryDate: Long = todayStartOfDay()

    fun loadEntry(petId: Long, entryId: Long?) {
        currentPetId = petId
        if (entryId == null) {
            currentEntryDate = todayStartOfDay()
            observePhotos()
            return
        }
        viewModelScope.launch {
            val entries = healthRepository.getAllEntries(petId).first()
            val entry = entries.find { it.id == entryId } ?: return@launch
            currentEntryDate = startOfDay(entry.registeredAt)
            _state.update {
                it.copy(
                    behavior    = entry.behavior,
                    fecesStatus = entry.fecesStatus,
                    urineStatus = entry.urineStatus,
                    vomitCount  = entry.vomitCount?.toString() ?: "",
                    mobility    = entry.mobility,
                    painSigns   = entry.painSigns,
                    observations = entry.observations ?: "",
                    registeredAt = entry.registeredAt,
                )
            }
            observePhotos()
        }
    }

    private fun observePhotos() {
        viewModelScope.launch {
            photoRepository.observeForDay(currentPetId, currentEntryDate).collect { photos ->
                _state.update { it.copy(photos = photos) }
            }
        }
    }

    fun addPhoto(uri: Uri) {
        viewModelScope.launch {
            try {
                photoRepository.savePhoto(currentPetId, currentEntryDate, uri)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Erro ao salvar foto: ${e.message}") }
            }
        }
    }

    fun deletePhoto(photo: HealthPhotoEntity) {
        viewModelScope.launch {
            try {
                photoRepository.deletePhoto(photo)
            } catch (e: Exception) {
                _state.update { it.copy(error = "Erro ao excluir foto: ${e.message}") }
            }
        }
    }

    fun updateBehavior(value: String?)    = _state.update { it.copy(behavior = value) }
    fun updateFeces(value: String?)       = _state.update { it.copy(fecesStatus = value) }
    fun updateUrine(value: String?)       = _state.update { it.copy(urineStatus = value) }
    fun updateVomitCount(value: String)   = _state.update { it.copy(vomitCount = value) }
    fun updateMobility(value: String?)    = _state.update { it.copy(mobility = value) }
    fun updatePainSigns(value: String?)   = _state.update { it.copy(painSigns = value) }
    fun updateObservations(value: String) = _state.update { it.copy(observations = value) }

    fun save(petId: Long, entryId: Long?) {
        val s = _state.value

        val hasAnyData = s.behavior != null || s.fecesStatus != null ||
            s.urineStatus != null || s.vomitCount.isNotBlank() ||
            s.mobility != null || s.painSigns != null ||
            s.observations.isNotBlank() || s.photos.isNotEmpty()

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
                    id          = entryId ?: 0L,
                    petId       = petId,
                    registeredAt = if (entryId != null) s.registeredAt ?: System.currentTimeMillis() else System.currentTimeMillis(),
                    behavior    = s.behavior,
                    fecesStatus = s.fecesStatus,
                    urineStatus = s.urineStatus,
                    vomitCount  = s.vomitCount.toIntOrNull(),
                    mobility    = s.mobility,
                    painSigns   = s.painSigns,
                    observations = s.observations.ifBlank { null }
                )
                if (entryId == null) healthRepository.saveEntry(entry)
                else healthRepository.updateEntry(entry)
                _state.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Erro ao salvar: ${e.message}") }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun todayStartOfDay(): Long = startOfDay(System.currentTimeMillis())

    private fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
