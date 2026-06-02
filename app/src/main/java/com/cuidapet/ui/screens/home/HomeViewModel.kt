package com.cuidadopet.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.PetEntity
import com.cuidadopet.data.repository.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val petRepository: PetRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val todayKey get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

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

    val birthdayPets: StateFlow<List<PetEntity>> = petRepository
        .getAllPets()
        .map { list -> list.filter { isBirthdayToday(it) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deletePet(pet: PetEntity) {
        viewModelScope.launch {
            petRepository.deletePet(pet)
        }
    }

    fun wasBirthdayBannerShownToday(petId: Long): Boolean =
        prefs.getString("birthday_banner_$petId", null) == todayKey

    fun markBirthdayBannerShown(petId: Long) =
        prefs.edit().putString("birthday_banner_$petId", todayKey).apply()

    private fun isBirthdayToday(pet: PetEntity): Boolean {
        val birthDate = pet.birthDate ?: return false
        val today = Calendar.getInstance()
        val birth = Calendar.getInstance().apply { timeInMillis = birthDate }
        return today.get(Calendar.MONTH) == birth.get(Calendar.MONTH) &&
               today.get(Calendar.DAY_OF_MONTH) == birth.get(Calendar.DAY_OF_MONTH)
    }
}
