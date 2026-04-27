package com.cuidadopet.ui.screens.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.MedicationEntity
import com.cuidadopet.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MedicationListViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    private val _petId = MutableStateFlow(-1L)

    val medications: StateFlow<List<MedicationEntity>> = _petId
        .flatMapLatest { id ->
            if (id == -1L) emptyFlow()
            else medicationRepository.getActiveMedications(id)
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun loadMedications(petId: Long) {
        _petId.value = petId
    }

    fun deactivateMedication(medicationId: Long) {
        viewModelScope.launch {
            medicationRepository.deactivateMedication(medicationId)
        }
    }
}
