package com.cuidadopet.ui.screens.water

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterConfigFormScreen(
    petId: Long,
    onNavigateBack: () -> Unit,
    viewModel: WaterConfigFormViewModel = hiltViewModel()
) {
    LaunchedEffect(petId) { viewModel.load(petId) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onNavigateBack() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val petName = state.petName.ifBlank { "Carregando..." }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar água — $petName") },
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

            // ── Meta diária ──────────────────────────────────────────────
            OutlinedTextField(
                value = state.dailyTargetMl,
                onValueChange = viewModel::updateTargetMl,
                label = { Text("Meta diária de água (ml)") },
                placeholder = { Text("Ex: 300") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text("ml/dia") }
            )

            // ── Lembretes ─────────────────────────────────────────────────
            Text("Lembretes", style = MaterialTheme.typography.titleSmall)

            // Toggle de ativar/desativar lembretes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Ativar lembretes", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "O app vai te lembrar de oferecer água",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.remindersEnabled,
                    onCheckedChange = viewModel::updateRemindersEnabled
                )
            }

            // Campo de intervalo — só exibido quando os lembretes estão ativos
            if (state.remindersEnabled) {
                OutlinedTextField(
                    value = state.reminderIntervalHours,
                    onValueChange = viewModel::updateReminderInterval,
                    label = { Text("Lembrar a cada (horas)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("h") },
                    supportingText = {
                        Text(
                            "Ex: 3 = lembrete a cada 3 horas. Mínimo recomendado: 2h.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // ── Botão salvar ──────────────────────────────────────────────
            Button(
                onClick = { viewModel.save(petId) },
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
                    Text("Salvar configuração")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
