package com.cuidadopet.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.PetEntity
import com.cuidadopet.data.repository.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PetDashboardViewModel @Inject constructor(
    private val petRepository: PetRepository
) : ViewModel() {

    private val _petId = MutableStateFlow(-1L)

    // Sempre inicializado — emite null até loadPet() ser chamado
    val pet: StateFlow<PetEntity?> = _petId
        .flatMapLatest { id ->
            if (id == -1L) flowOf(null)
            else petRepository.getPetById(id)
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun loadPet(petId: Long) {
        _petId.value = petId
    }
}
