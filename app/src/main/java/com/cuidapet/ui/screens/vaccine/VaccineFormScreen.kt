package com.cuidadopet.ui.screens.vaccine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccineFormScreen(
    petId: Long,
    vaccineId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: VaccineFormViewModel = hiltViewModel()
) {
    LaunchedEffect(petId, vaccineId) { viewModel.init(petId, vaccineId) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")) }

    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onNavigateBack() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    var showAdministeredDatePicker by remember { mutableStateOf(false) }
    var showNextDueDatePicker       by remember { mutableStateOf(false) }

    if (showAdministeredDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.administeredAt ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showAdministeredDatePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    viewModel.onAdministeredAtChange(pickerState.selectedDateMillis)
                    showAdministeredDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showAdministeredDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showNextDueDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.nextDueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showNextDueDatePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    viewModel.onNextDueDateChange(pickerState.selectedDateMillis)
                    showNextDueDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showNextDueDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vaccineId == null) "Nova vacina/vermífugo" else "Editar registro") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
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
                .padding(horizontal = adaptiveHorizontalPadding(), vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Tipo ─────────────────────────────────────────────────────────
            SectionTitle("Tipo")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.type == "VACCINE",
                    onClick  = { viewModel.onTypeChange("VACCINE") },
                    label    = { Text("Vacina") }
                )
                FilterChip(
                    selected = uiState.type == "DEWORMER",
                    onClick  = { viewModel.onTypeChange("DEWORMER") },
                    label    = { Text("Vermífugo") }
                )
            }

            // ── Nome ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = uiState.name,
                onValueChange = { if (it.length <= 30) viewModel.onNameChange(it) },
                label         = { Text(if (uiState.type == "VACCINE") "Nome da vacina *" else "Nome do vermífugo *") },
                placeholder   = { Text(if (uiState.type == "VACCINE") "Ex: Antirrábica, V10..." else "Ex: Drontal Plus...") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                supportingText = { Text("${uiState.name.length}/30") }
            )

            // ── Aplicada? ─────────────────────────────────────────────────────
            SectionTitle("Aplicação")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked         = uiState.isAdministered,
                    onCheckedChange = viewModel::onIsAdministeredChange
                )
                Text(
                    "Já foi administrada",
                    style    = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (uiState.isAdministered) {
                OutlinedButton(
                    onClick  = { showAdministeredDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Data de aplicação: ${
                            uiState.administeredAt?.let { dateFmt.format(Date(it)) } ?: "Toque para selecionar"
                        }"
                    )
                }
            }

            // ── Próxima dose ──────────────────────────────────────────────────
            SectionTitle("Próxima dose (opcional)")
            val nextDueDate = uiState.nextDueDate
            if (nextDueDate == null) {
                OutlinedButton(
                    onClick  = { showNextDueDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Definir data da próxima dose")
                }
            } else {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick  = { showNextDueDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Próxima dose: ${dateFmt.format(Date(nextDueDate))}")
                    }
                    TextButton(onClick = { viewModel.onNextDueDateChange(null) }) {
                        Text("Remover")
                    }
                }

                // Lembrete — só aparece se há data de próxima dose
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked         = uiState.reminderEnabled,
                        onCheckedChange = viewModel::onReminderEnabledChange
                    )
                    Text(
                        "Lembrete na data da próxima dose",
                        style    = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            // ── Observações ───────────────────────────────────────────────────
            SectionTitle("Observações (opcional)")
            OutlinedTextField(
                value          = uiState.notes,
                onValueChange  = { if (it.length <= 300) viewModel.onNotesChange(it) },
                label          = { Text("Observações") },
                placeholder    = { Text("Lote, clínica, reações...") },
                modifier       = Modifier.fillMaxWidth(),
                minLines       = 2,
                maxLines       = 4,
                supportingText = { Text("${uiState.notes.length}/300") }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = { viewModel.save(vaccineId) },
                modifier = Modifier.fillMaxWidth(),
                enabled  = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (vaccineId == null) "Salvar" else "Atualizar")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary
    )
}
