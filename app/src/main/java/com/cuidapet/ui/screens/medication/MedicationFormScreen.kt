package com.cuidadopet.ui.screens.medication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding
import com.cuidadopet.ui.utils.TimeInputField
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val DOSE_UNIT_OPTIONS = listOf(
    "comprimido", "cápsula", "ml", "mg", "g", "gota", "aplicação", "sachê", "UI"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedicationFormScreen(
    petId: Long,
    medicationId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: MedicationFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var doseUnitExpanded by remember { mutableStateOf(false) }
    var showNotifDialog by remember { mutableStateOf(false) }

    // Lançador para solicitar permissão de notificação; salva após resultado
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.saveMedication(petId, medicationId) }

    fun doSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.saveMedication(petId, medicationId)
            else notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.saveMedication(petId, medicationId)
        }
    }

    LaunchedEffect(medicationId) {
        if (medicationId != null) viewModel.loadMedication(medicationId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Diálogo de confirmação para receber alertas
    if (showNotifDialog) {
        AlertDialog(
            onDismissRequest = { showNotifDialog = false },
            title   = { Text("Receber alertas?") },
            text    = {
                Text(
                    "O app enviará uma notificação nos horários configurados para lembrar " +
                    "você de administrar o medicamento. Deseja ativar os alertas?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotifDialog = false
                    viewModel.onReminderEnabledChange(true)
                    doSave()
                }) {
                    Text("Ativar e salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotifDialog = false
                    viewModel.onReminderEnabledChange(false)
                    viewModel.saveMedication(petId, medicationId)
                }) { Text("Salvar sem alertas") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (medicationId == null) "Novo medicamento" else "Editar medicamento",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
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
                    containerColor = MaterialTheme.colorScheme.primary,
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

            // ── Nome ───────────────────────────────────────────────────────
            SectionLabel("Medicamento")

            Text(
                text = "⚠ Medicamentos devem ser prescritos por médico veterinário. " +
                       "Este app não sugere nem recomenda medicamentos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nome (ex: Amoxicilina, Prednisolona) *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Forma ──────────────────────────────────────────────────────
            SectionLabel("Forma de administração *")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ORAL" to "Oral", "TOPICAL" to "Tópico",
                       "INJECTABLE" to "Injetável", "EYE_DROP" to "Colírio").forEach { (key, label) ->
                    FilterChip(
                        selected = uiState.form == key,
                        onClick = { viewModel.onFormChange(key) },
                        label = { Text(label) }
                    )
                }
            }

            // ── Dose ───────────────────────────────────────────────────────
            SectionLabel("Dose *")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.dose,
                    onValueChange = viewModel::onDoseChange,
                    label = { Text("Quantidade") },
                    placeholder = { Text("Ex: 0,5") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Dropdown de unidade de dose
                ExposedDropdownMenuBox(
                    expanded = doseUnitExpanded,
                    onExpandedChange = { doseUnitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.doseUnit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidade") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = doseUnitExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = doseUnitExpanded,
                        onDismissRequest = { doseUnitExpanded = false }
                    ) {
                        DOSE_UNIT_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.onDoseUnitChange(option)
                                    doseUnitExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            // ── Frequência ─────────────────────────────────────────────────
            SectionLabel("Frequência *")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.frequencyType == "INTERVAL",
                    onClick = { viewModel.onFrequencyTypeChange("INTERVAL") },
                    label = { Text("A cada X horas") }
                )
                FilterChip(
                    selected = uiState.frequencyType == "FIXED_TIMES",
                    onClick = { viewModel.onFrequencyTypeChange("FIXED_TIMES") },
                    label = { Text("Horários fixos") }
                )
            }

            if (uiState.frequencyType == "INTERVAL") {
                OutlinedTextField(
                    value = uiState.frequencyHours,
                    onValueChange = { viewModel.onFrequencyHoursChange(it.filter { c -> c.isDigit() }.take(3)) },
                    label = { Text("Intervalo entre doses *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("horas") }
                )
                TimeInputField(
                    value = uiState.intervalStartTime,
                    onValueChange = viewModel::onIntervalStartTimeChange,
                    label = "Horário da primeira dose (opcional)",
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            "Se não informado, o primeiro alarme é agendado a partir de agora.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            if (uiState.frequencyType == "FIXED_TIMES") {
                uiState.fixedTimes.forEachIndexed { index, time ->
                    key(index) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeInputField(
                                value = time,
                                onValueChange = { viewModel.onFixedTimeChange(index, it) },
                                label = "Horário ${index + 1}",
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.fixedTimes.size > 1) {
                                IconButton(onClick = { viewModel.removeFixedTime(index) }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Remover horário")
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = viewModel::addFixedTime,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(" Adicionar horário")
                }
            }

            // ── Duração ────────────────────────────────────────────────────
            SectionLabel("Duração do tratamento")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = uiState.isContinuous,
                    onCheckedChange = viewModel::onContinuousChange
                )
                Text("Uso contínuo (sem data de fim)")
            }

            if (!uiState.isContinuous) {
                OutlinedTextField(
                    value = uiState.durationDays,
                    onValueChange = { viewModel.onDurationDaysChange(it.filter { c -> c.isDigit() }.take(3)) },
                    label = { Text("Duração *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("dias") }
                )
            }

            // ── Orientação de administração ────────────────────────────────
            SectionLabel("Orientação de administração")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "WITH_FOOD"  to "Com alimentação",
                    "FASTING"    to "Em jejum",
                    "WITH_WATER" to "Com água",
                    "OTHER"      to "Outra"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = uiState.guideline == key,
                        onClick = { viewModel.onGuidelineChange(key) },
                        label = { Text(label) }
                    )
                }
            }

            if (uiState.guideline == "OTHER") {
                OutlinedTextField(
                    value = uiState.guidelineDetail,
                    onValueChange = viewModel::onGuidelineDetailChange,
                    label = { Text("Detalhe a orientação") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }

            // ── Observações ────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.observations,
                onValueChange = viewModel::onObservationsChange,
                label = { Text("Observações (opcional)") },
                placeholder = { Text("Ex: esconder na comida, amarga — usar seringa") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // ── Botão salvar ───────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showNotifDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(if (medicationId == null) "Cadastrar medicamento" else "Salvar alterações")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

