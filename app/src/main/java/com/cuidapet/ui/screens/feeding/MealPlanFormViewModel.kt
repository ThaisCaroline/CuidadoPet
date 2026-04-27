package com.cuidadopet.ui.screens.feeding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
import com.cuidadopet.data.repository.FeedingRepository
import com.cuidadopet.data.repository.PetRepository
import com.cuidadopet.domain.EnergyCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado do formulário de plano alimentar.
// Cada campo é uma String para facilitar o binding com os TextFields —
// a conversão para Double acontece apenas ao salvar.
data class MealPlanFormState(
    val petName: String = "",                  // nome do pet — carregado pelo ViewModel
    val foodType: String = "DRY_KIBBLE",       // tipo de alimento selecionado
    val restrictions: String = "",             // restrições em texto livre
    val dailyQuantityGrams: String = "",       // gramas totais por dia (input do tutor)
    val dailyKcalTarget: String = "",          // kcal/dia (calculado ou digitado)
    val meals: List<MealTimeEntry> = listOf(   // lista de refeições com horário e quantidade
        MealTimeEntry("07:00", ""),
        MealTimeEntry("19:00", "")
    ),
    val quantityUnit: String = "g",        // "g" ou "ml"
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

// Representa uma entrada de refeição no formulário:
// horário (ex: "07:00") + quantidade em gramas (ex: "150")
data class MealTimeEntry(
    val time: String,
    val quantityGrams: String
)

@HiltViewModel
class MealPlanFormViewModel @Inject constructor(
    private val feedingRepository: FeedingRepository,
    private val petRepository: PetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MealPlanFormState())
    val state: StateFlow<MealPlanFormState> = _state.asStateFlow()

    // Carrega o nome do pet e o plano ativo (se existir) para pré-preencher o formulário
    fun loadExistingPlan(petId: Long) {
        viewModelScope.launch {
            // Carrega o nome do pet para exibir na TopAppBar
            val pet = petRepository.getPetById(petId).first()
            _state.update { it.copy(petName = pet?.name ?: "") }

            val plan = feedingRepository.getActiveMealPlan(petId).first()
            if (plan != null) {
                val savedMeals = feedingRepository.getMealsForPlan(plan.id).first()
                val mealEntries = if (savedMeals.isNotEmpty()) {
                    savedMeals.map { m ->
                        val qty = when {
                            m.quantityGrams == 0.0       -> ""
                            m.quantityGrams % 1.0 == 0.0 -> m.quantityGrams.toLong().toString()
                            else                         -> m.quantityGrams.toString()
                        }
                        MealTimeEntry(m.timeOfDay, qty)
                    }
                } else {
                    listOf(MealTimeEntry("07:00", ""), MealTimeEntry("19:00", ""))
                }
                _state.update {
                    it.copy(
                        foodType = plan.foodType,
                        restrictions = plan.restrictions ?: "",
                        dailyQuantityGrams = plan.dailyQuantityGrams?.toInt()?.toString() ?: "",
                        dailyKcalTarget = plan.dailyKcalTarget?.toInt()?.toString() ?: "",
                        meals = mealEntries
                    )
                }
            }
        }
    }


    // ─── Funções de atualização de campo ───────────────────────────────────

    fun updateFoodType(value: String)          = _state.update { it.copy(foodType = value) }
    fun updateRestrictions(value: String)      = _state.update { it.copy(restrictions = value) }
    fun updateDailyQuantity(value: String)     = _state.update { it.copy(dailyQuantityGrams = value) }
    fun updateDailyKcal(value: String)         = _state.update { it.copy(dailyKcalTarget = value) }
    fun updateQuantityUnit(unit: String)       = _state.update { it.copy(quantityUnit = unit) }

    // Adiciona uma nova entrada de refeição com horário vazio — o tutor preenche depois
    fun addMealEntry() {
        _state.update { it.copy(meals = it.meals + MealTimeEntry("", "")) }
    }

    // Remove a refeição no índice especificado
    fun removeMealEntry(index: Int) {
        _state.update { it.copy(meals = it.meals.toMutableList().also { list -> list.removeAt(index) }) }
    }

    // Atualiza o horário de uma refeição específica — aplica auto-formato HH:mm
    fun updateMealTime(index: Int, time: String) {
        _state.update { state ->
            val updated = state.meals.toMutableList()
            updated[index] = updated[index].copy(time = time)
            state.copy(meals = updated)
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

    // Atualiza a quantidade em gramas de uma refeição específica
    fun updateMealQuantity(index: Int, quantity: String) {
        _state.update { state ->
            val updated = state.meals.toMutableList()
            updated[index] = updated[index].copy(quantityGrams = quantity)
            state.copy(meals = updated)
        }
    }

    // ─── Salvar ─────────────────────────────────────────────────────────────

    fun savePlan(petId: Long, petName: String) {
        // Normaliza horários incompletos antes de validar: "8" → "8:00", "08:" → "08:00"
        _state.update { s -> s.copy(meals = s.meals.map { it.copy(time = normalizeTime(it.time)) }) }
        val s = _state.value

        val validMeals = s.meals.filter { it.time.isNotBlank() }
        if (validMeals.isEmpty()) {
            _state.update { it.copy(error = "Adicione ao menos uma refeição com horário.") }
            return
        }
        if (validMeals.size > 12) {
            _state.update { it.copy(error = "Máximo de 12 refeições por plano.") }
            return
        }
        val timeRegex = Regex("""^\d{2}:\d{2}$""")
        if (validMeals.any { !it.time.matches(timeRegex) }) {
            _state.update { it.copy(error = "Horários devem estar no formato HH:mm.") }
            return
        }
        validMeals.forEach { meal ->
            val qty = meal.quantityGrams.toDoubleOrNull()
            if (qty != null && qty > 5000) {
                _state.update { it.copy(error = "Quantidade máxima por refeição: 5.000 g.") }
                return
            }
        }
        val kcal = s.dailyKcalTarget.toDoubleOrNull()
        if (kcal != null && kcal > 10_000) {
            _state.update { it.copy(error = "Meta calórica máxima: 10.000 kcal.") }
            return
        }
        val grams = s.dailyQuantityGrams.toDoubleOrNull()
        if (grams != null && grams > 10_000) {
            _state.update { it.copy(error = "Quantidade diária máxima: 10.000 g.") }
            return
        }

        _state.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                val plan = MealPlanEntity(
                    petId = petId,
                    foodType = s.foodType,
                    restrictions = s.restrictions.ifBlank { null },
                    dailyKcalTarget = s.dailyKcalTarget.toDoubleOrNull(),
                    dailyQuantityGrams = s.dailyQuantityGrams.toDoubleOrNull(),
                    isActive = true
                )

                // As refeições só terão mealPlanId real depois que o plano for inserido —
                // o repositório faz esse vínculo internamente
                val meals = validMeals.map { entry ->
                    MealEntity(
                        mealPlanId = 0L,  // substituído no repositório
                        timeOfDay = entry.time,
                        quantityGrams = entry.quantityGrams.toDoubleOrNull() ?: 0.0
                    )
                }

                feedingRepository.setMealPlan(plan, meals, petName)
                _state.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Erro ao salvar: ${e.message}") }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
