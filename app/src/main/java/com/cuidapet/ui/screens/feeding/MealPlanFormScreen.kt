package com.cuidadopet.ui.screens.feeding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.cuidadopet.ui.utils.TimeInputField
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Tipos de alimento disponíveis para seleção — cada um com rótulo exibido na tela
private val foodTypeOptions = listOf(
    "DRY_KIBBLE"   to "Ração seca",
    "WET_FOOD"     to "Ração úmida",
    "NATURAL"      to "Natural/Caseira",
    "THERAPEUTIC"  to "Dieta terapêutica",
    "OTHER"        to "Outro"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanFormScreen(
    petId: Long,
    onNavigateBack: () -> Unit,
    viewModel: MealPlanFormViewModel = hiltViewModel()
) {
    LaunchedEffect(petId) { viewModel.loadExistingPlan(petId) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navega de volta automaticamente após salvar com sucesso
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    // Exibe erros via Snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // petName vem do ViewModel — exibe "Carregando..." enquanto o banco responde
    val petName = state.petName.ifBlank { "Carregando..." }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plano alimentar de $petName") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = adaptiveHorizontalPadding())
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Tipo de alimento ──────────────────────────────────────────
            Text("Tipo de alimento", style = MaterialTheme.typography.titleSmall)
            FoodTypeSelector(
                selected = state.foodType,
                onSelect = viewModel::updateFoodType
            )

            // ── Restrições alimentares ────────────────────────────────────
            OutlinedTextField(
                value = state.restrictions,
                onValueChange = viewModel::updateRestrictions,
                label = { Text("Restrições alimentares (opcional)") },
                placeholder = { Text("Ex: sem sódio, sem frango...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Text(
                "Todos os valores abaixo são metas diárias. Siga sempre a orientação do veterinário.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Unidade de quantidade ────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.quantityUnit == "g",
                    onClick  = { viewModel.updateQuantityUnit("g") },
                    label    = { Text("Gramas (g)") }
                )
                FilterChip(
                    selected = state.quantityUnit == "ml",
                    onClick  = { viewModel.updateQuantityUnit("ml") },
                    label    = { Text("Mililitros (ml)") }
                )
            }

            // ── Meta diária de ração ──────────────────────────────────────
            OutlinedTextField(
                value = state.dailyQuantityGrams,
                onValueChange = { viewModel.updateDailyQuantity(it.filter { c -> c.isDigit() }.take(5)) },
                label = { Text("Meta diária de ração (${state.quantityUnit})") },
                placeholder = { Text("Ex: 300") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text("${state.quantityUnit}/dia") }
            )

            // ── Meta calórica diária ──────────────────────────────────────
            OutlinedTextField(
                value = state.dailyKcalTarget,
                onValueChange = { viewModel.updateDailyKcal(it.filter { c -> c.isDigit() }.take(5)) },
                label = { Text("Meta calórica diária (opcional)") },
                placeholder = { Text("Ex: 450") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text("kcal/dia") }
            )

            // ── Refeições do dia ──────────────────────────────────────────
            Text("Refeições do dia", style = MaterialTheme.typography.titleSmall)
            Text(
                "Configure os horários e as quantidades de cada refeição.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            state.meals.forEachIndexed { index, entry ->
                MealEntryRow(
                    entry = entry,
                    onTimeChange = { viewModel.updateMealTime(index, it) },
                    onQuantityChange = { viewModel.updateMealQuantity(index, it) },
                    onRemove = { viewModel.removeMealEntry(index) },
                    canRemove = state.meals.size > 1,
                    unit = state.quantityUnit
                )
            }

            // Botão para adicionar mais refeições ao plano
            TextButton(
                onClick = viewModel::addMealEntry,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Adicionar refeição")
            }

            // ── Botão salvar ──────────────────────────────────────────────
            Button(
                onClick = { viewModel.savePlan(petId, state.petName) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp).width(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Salvar plano")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// Chips de seleção do tipo de alimento
@Composable
private fun FoodTypeSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    // Divide em duas linhas para não extravasar a tela
    val chunked = foodTypeOptions.chunked(3)
    chunked.forEach { row ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            row.forEach { (key, label) ->
                FilterChip(
                    selected = selected == key,
                    onClick = { onSelect(key) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

// Linha de uma refeição: campo de horário + campo de quantidade + botão de remover
@Composable
private fun MealEntryRow(
    entry: MealTimeEntry,
    onTimeChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
    unit: String = "g"
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        TimeInputField(
            value = entry.time,
            onValueChange = onTimeChange,
            label = "Horário",
            placeholder = "07:00",
            modifier = Modifier.weight(1f)
        )

        OutlinedTextField(
            value = entry.quantityGrams,
            onValueChange = { new ->
                val filtered = new.filter { it.isDigit() }.take(5)
                onQuantityChange(filtered)
            },
            label = { Text(if (unit == "ml") "Mililitros" else "Gramas") },
            placeholder = { Text("150") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            suffix = { Text(unit) }
        )

        // Botão de remover — desabilitado se só restar uma refeição
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remover refeição",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

