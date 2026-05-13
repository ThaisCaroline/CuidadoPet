package com.cuidadopet.ui.screens.vaccine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.VaccineEntity
import com.cuidadopet.data.repository.VaccineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaccineListViewModel @Inject constructor(
    private val vaccineRepository: VaccineRepository
) : ViewModel() {

    private val _petId = MutableStateFlow(0L)

    val vaccines: StateFlow<List<VaccineEntity>> = _petId
        .filter { it != 0L }
        .flatMapLatest { vaccineRepository.getVaccinesForPet(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPetId(petId: Long) {
        _petId.value = petId
    }

    fun delete(vaccine: VaccineEntity) {
        viewModelScope.launch { vaccineRepository.delete(vaccine.id) }
    }
}
