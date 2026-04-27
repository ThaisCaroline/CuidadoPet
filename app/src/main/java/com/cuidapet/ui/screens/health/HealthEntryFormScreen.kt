package com.cuidadopet.ui.screens.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HealthEntryFormScreen(
    petId: Long,
    entryId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: HealthEntryFormViewModel = hiltViewModel()
) {
    LaunchedEffect(petId, entryId) { viewModel.loadEntry(petId, entryId) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onNavigateBack() }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    val title = if (entryId == null) "Nova entrada no diário" else "Editar entrada"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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

            // ── Comportamento ───────────────────────────────────────────
            ChipSelector(
                title = "Comportamento geral",
                options = listOf(
                    "NORMAL"    to "Normal, ativo",
                    "LETHARGIC" to "Apático",
                    "AGITATED"  to "Agitado",
                    "SLEEPY"    to "Sonolento"
                ),
                selected = state.behavior,
                onSelect = viewModel::updateBehavior
            )

            // ── Fezes ──────────────────────────────────────────────────
            ChipSelector(
                title = "Fezes",
                options = listOf(
                    "NORMAL"   to "Normal",
                    "SOFT"     to "Amolecidas",
                    "DIARRHEA" to "Diarreia",
                    "ABSENT"   to "Não evacuou",
                    "BLOOD"    to "Com sangue ⚠️"
                ),
                selected = state.fecesStatus,
                onSelect = viewModel::updateFeces
            )

            // ── Urina ──────────────────────────────────────────────────
            ChipSelector(
                title = "Urina",
                options = listOf(
                    "NORMAL"    to "Normal",
                    "INCREASED" to "Aumentada",
                    "REDUCED"   to "Reduzida",
                    "ABSENT"    to "Não urinou",
                    "BLOOD"     to "Com sangue ⚠️"
                ),
                selected = state.urineStatus,
                onSelect = viewModel::updateUrine
            )

            // ── Vômitos ────────────────────────────────────────────────
            OutlinedTextField(
                value = state.vomitCount,
                onValueChange = viewModel::updateVomitCount,
                label = { Text("Número de episódios de vômito") },
                placeholder = { Text("0 = não vomitou") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // ── Mobilidade ────────────────────────────────────────────
            ChipSelector(
                title = "Mobilidade",
                options = listOf(
                    "NORMAL"   to "Normal",
                    "REDUCED"  to "Reduzida",
                    "IMMOBILE" to "Imóvel"
                ),
                selected = state.mobility,
                onSelect = viewModel::updateMobility
            )

            // ── Sinais de dor ─────────────────────────────────────────
            ChipSelector(
                title = "Sinais de dor",
                options = listOf(
                    "NONE"     to "Sem sinais",
                    "APPARENT" to "Aparente",
                    "EVIDENT"  to "Evidente"
                ),
                selected = state.painSigns,
                onSelect = viewModel::updatePainSigns
            )

            // ── Observações livres ────────────────────────────────────
            OutlinedTextField(
                value = state.observations,
                onValueChange = viewModel::updateObservations,
                label = { Text("Observações (campo livre)") },
                placeholder = { Text("Ex: dormiu o dia todo, não quis brincar...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5
            )

            // ── Aviso ─────────────────────────────────────────────────
            Text(
                "Este diário é para observação do tutor. Não substitui avaliação veterinária.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Salvar ────────────────────────────────────────────────
            Button(
                onClick = { viewModel.save(petId, entryId) },
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
                    Text("Salvar entrada")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// Seletor de chip reutilizável — toque no chip selecionado para desmarcar (nullable)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSelector(
    title: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        // FlowRow quebra automaticamente para a próxima linha quando não cabe
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (key, label) ->
                FilterChip(
                    selected = selected == key,
                    // Tocar no chip já selecionado o desmarca (volta a null)
                    onClick = { onSelect(if (selected == key) null else key) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}
