package com.cuidadopet.ui.screens.medication

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cuidadopet.R
import com.cuidadopet.domain.PrescriptionParser

// Lê uma foto de receita e cria os medicamentos depois que o tutor confere
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionScanScreen(
    petId: Long,
    onNavigateBack: () -> Unit,
    viewModel: PrescriptionScanViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onImageSelected(uri)
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    LaunchedEffect(petId) {
        viewModel.loadActiveNames(petId)
    }

    val selectedCount = state.items.count { it.selected }

    if (state.showDisclaimer) {
        AlertDialog(
            onDismissRequest = { viewModel.onDisclaimerAccepted() },
            title = { Text(stringResource(R.string.scan_disclaimer_title)) },
            text = { Text(stringResource(R.string.scan_disclaimer_text)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onDisclaimerAccepted() }) {
                    Text(stringResource(R.string.scan_disclaimer_ok))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        bottomBar = {
            if (state.items.isNotEmpty()) {
                Button(
                    onClick = { viewModel.saveSelected(petId) },
                    enabled = selectedCount > 0 && !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.scan_save_button, selectedCount))
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Button(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !state.isReading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_pick_button))
                }
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.scan_tips_title),
                        style = MaterialTheme.typography.labelLarge
                    )
                    listOf(
                        R.string.scan_tip_frame,
                        R.string.scan_tip_light,
                        R.string.scan_tip_steady,
                        R.string.scan_tip_handwriting,
                        R.string.scan_tip_privacy
                    ).forEach { tip ->
                        Text(
                            text = stringResource(R.string.scan_tip_bullet, stringResource(tip)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.isReading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.scan_reading))
                        }
                    }
                }
            }

            state.errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (state.hasScanned && !state.isReading && state.items.isEmpty() && state.errorMessage == null) {
                item {
                    Text(
                        text = stringResource(R.string.scan_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            items(state.items, key = { it.id }) { item ->
                ScanItemCard(
                    item = item,
                    onToggle = { viewModel.onToggleSelected(item.id) },
                    onNameChange = { viewModel.onNameChange(item.id, it) },
                    onDoseChange = { viewModel.onDoseChange(item.id, it) },
                    onDurationChange = { viewModel.onDurationChange(item.id, it) },
                    onStartTimeChange = { viewModel.onStartTimeChange(item.id, it) },
                    onIntervalHoursChange = { viewModel.onIntervalHoursChange(item.id, it) },
                    onTimesChange = { viewModel.onTimesChange(item.id, it) }
                )
            }
        }
    }
}

// Resumo do que será agendado: horários e total de doses até o fim do tratamento.
// Retorna null enquanto algum campo de horário ou duração estiver inválido.
@Composable
private fun schedulePreview(item: ScanItem): String? {
    val days = item.durationDays.toIntOrNull()?.takeIf { it in 1..3650 } ?: return null
    val endDate = DateFormat.getDateInstance(DateFormat.SHORT).format(
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }.time
    )
    val body = if (item.frequencyType == "INTERVAL") {
        val hours = item.intervalHours.toIntOrNull()?.takeIf { it in 1..720 } ?: return null
        if (!isValidTime(item.startTime)) return null
        val total = days * 24 / hours
        stringResource(R.string.scan_preview_interval, hours, PrescriptionParser.normalizeTime(item.startTime) ?: item.startTime) + "\n" +
            stringResource(R.string.scan_preview_total, total, endDate)
    } else {
        val times = parseTimes(item.timesText) ?: return null
        val total = times.size * days
        stringResource(R.string.scan_fixed_times, times.joinToString(", ")) + "\n" +
            stringResource(R.string.scan_preview_total, total, endDate)
    }
    val first = nextAlarmMillis(item, System.currentTimeMillis()) ?: return body
    val firstText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(first))
    return body + "\n" + stringResource(R.string.scan_preview_first, firstText)
}

// Primeiro momento em que o app vai tocar, com a mesma regra do agendador:
// se o horário já passou hoje, pula para o próximo horário do intervalo (ou para amanhã, nos horários fixos)
private fun nextAlarmMillis(item: ScanItem, now: Long): Long? {
    val day = 24L * 60 * 60 * 1000
    if (item.frequencyType == "INTERVAL") {
        val hours = item.intervalHours.toIntOrNull()?.takeIf { it in 1..720 } ?: return null
        val (hour, minute) = PrescriptionParser.normalizeTime(item.startTime)?.split(":")?.map { it.toInt() } ?: return null
        var next = todayAtMillis(hour, minute)
        val intervalMs = hours * 60L * 60 * 1000
        while (next <= now) next += intervalMs
        return next
    }
    val times = parseTimes(item.timesText) ?: return null
    return times
        .map { time -> time.split(":").map { it.toInt() }.let { todayAtMillis(it[0], it[1]) } }
        .map { if (it > now) it else it + day }
        .minOrNull()
}

private fun todayAtMillis(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, hour)
    set(Calendar.MINUTE, minute)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
private fun ScanItemCard(
    item: ScanItem,
    onToggle: () -> Unit,
    onNameChange: (String) -> Unit,
    onDoseChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onIntervalHoursChange: (String) -> Unit,
    onTimesChange: (String) -> Unit
) {
    val preview = schedulePreview(item)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.selected, onCheckedChange = { onToggle() })
                Text(
                    text = stringResource(R.string.scan_include_label),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = item.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.scan_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.dose,
                    onValueChange = onDoseChange,
                    label = { Text(stringResource(R.string.scan_dose_label, item.doseUnit)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = item.durationDays,
                    onValueChange = onDurationChange,
                    label = { Text(stringResource(R.string.scan_duration_label)) },
                    singleLine = true,
                    isError = (item.durationDays.toIntOrNull() ?: 0) !in 1..3650,
                    supportingText = {
                        if (item.durationDays.isBlank()) {
                            Text(stringResource(R.string.scan_duration_required))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            if (item.frequencyType == "INTERVAL") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = item.startTime,
                        onValueChange = onStartTimeChange,
                        label = { Text(stringResource(R.string.scan_start_time_label)) },
                        singleLine = true,
                        isError = !isValidTime(item.startTime),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = item.intervalHours,
                        onValueChange = onIntervalHoursChange,
                        label = { Text(stringResource(R.string.scan_interval_label)) },
                        singleLine = true,
                        isError = (item.intervalHours.toIntOrNull() ?: 0) !in 1..720,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                OutlinedTextField(
                    value = item.timesText,
                    onValueChange = onTimesChange,
                    label = { Text(stringResource(R.string.scan_times_label)) },
                    singleLine = true,
                    isError = parseTimes(item.timesText) == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            preview?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }

            if (item.isDuplicate) {
                Text(
                    text = stringResource(R.string.scan_duplicate_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (item.needsReview) {
                Text(
                    text = stringResource(R.string.scan_review_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = stringResource(R.string.scan_source_label, item.sourceLine),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
