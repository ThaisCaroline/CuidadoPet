package com.cuidadopet.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.PetEntity
import com.cuidadopet.data.repository.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// @HiltViewModel indica ao Hilt que este ViewModel deve ser criado por ele.
// Isso permite injetar o PetRepository automaticamente no construtor.
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val petRepository: PetRepository
) : ViewModel() {

    // StateFlow é um "stream" de estado que sempre tem um valor atual.
    // A tela observa este StateFlow e redesenha automaticamente quando muda.
    //
    // stateIn() converte o Flow do repositório em StateFlow:
    //   - scope: viewModelScope = o Flow vive enquanto o ViewModel existir
    //   - started: SharingStarted.WhileSubscribed(5000) = mantém o Flow ativo
    //     por 5 segundos após a tela sair (evita recarregar dados ao girar a tela)
    //   - initialValue: emptyList() = enquanto o banco não responde, a lista está vazia
    val pets: StateFlow<List<PetEntity>> = petRepository
        .getAllPets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Remove um pet — chamado quando o tutor confirma a exclusão
    // launch { } executa a operação de forma assíncrona (sem travar a tela)
    fun deletePet(pet: PetEntity) {
        viewModelScope.launch {
            petRepository.deletePet(pet)
        }
    }
}
