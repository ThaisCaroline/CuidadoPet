package com.cuidadopet.ui.screens.medication

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.cuidadopet.R
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
    var showDatePicker by remember { mutableStateOf(false) }
    var dialogSuperReminder by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }

    LaunchedEffect(uiState.startDateMillis) {
        val local = Calendar.getInstance().apply { timeInMillis = uiState.startDateMillis }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        datePickerState.selectedDateMillis = utc.timeInMillis
    }

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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
                        val localCal = Calendar.getInstance().apply {
                            set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        viewModel.onStartDateChange(localCal.timeInMillis)
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showNotifDialog) {
        AlertDialog(
            onDismissRequest = { showNotifDialog = false },
            title   = { Text(stringResource(R.string.med_form_notif_title)) },
            text    = {
                Column {
                    Text(stringResource(R.string.med_form_notif_msg))
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked         = dialogSuperReminder,
                            onCheckedChange = { dialogSuperReminder = it }
                        )
                        Column {
                            Text(
                                stringResource(R.string.super_reminder_label),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                stringResource(R.string.super_reminder_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotifDialog = false
                    viewModel.onReminderEnabledChange(true)
                    viewModel.onSuperReminderChange(dialogSuperReminder)
                    if (dialogSuperReminder) {
                        val alarmMgr = context.getSystemService(AlarmManager::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            !alarmMgr.canScheduleExactAlarms()
                        ) {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            val notifMgr = context.getSystemService(NotificationManager::class.java)
                            if (!notifMgr.canUseFullScreenIntent()) {
                                context.startActivity(
                                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                )
                            }
                        }
                    }
                    doSave()
                }) {
                    Text(stringResource(R.string.med_form_notif_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotifDialog = false
                    viewModel.onReminderEnabledChange(false)
                    viewModel.onSuperReminderChange(false)
                    viewModel.saveMedication(petId, medicationId)
                }) { Text(stringResource(R.string.med_form_notif_skip)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(if (medicationId == null) R.string.med_form_new_title else R.string.med_form_edit_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
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

            SectionLabel(stringResource(R.string.med_form_section_med))

            Text(
                text = stringResource(R.string.med_form_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.med_form_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            SectionLabel(stringResource(R.string.med_form_route_section))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "ORAL"       to R.string.dose_form_oral,
                    "TOPICAL"    to R.string.dose_form_topical,
                    "INJECTABLE" to R.string.dose_form_injectable,
                    "EYE_DROP"   to R.string.dose_form_eye_drop
                ).forEach { (key, labelRes) ->
                    FilterChip(
                        selected = uiState.form == key,
                        onClick = { viewModel.onFormChange(key) },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }

            SectionLabel(stringResource(R.string.med_form_dose_section))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.dose,
                    onValueChange = viewModel::onDoseChange,
                    label = { Text(stringResource(R.string.med_form_dose_qty_label)) },
                    placeholder = { Text(stringResource(R.string.med_form_dose_qty_hint)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                ExposedDropdownMenuBox(
                    expanded = doseUnitExpanded,
                    onExpandedChange = { doseUnitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.doseUnit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.med_form_dose_unit_label)) },
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

            SectionLabel(stringResource(R.string.med_form_frequency_section))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.frequencyType == "INTERVAL",
                    onClick = { viewModel.onFrequencyTypeChange("INTERVAL") },
                    label = { Text(stringResource(R.string.med_form_freq_interval)) }
                )
                FilterChip(
                    selected = uiState.frequencyType == "FIXED_TIMES",
                    onClick = { viewModel.onFrequencyTypeChange("FIXED_TIMES") },
                    label = { Text(stringResource(R.string.med_form_freq_fixed)) }
                )
            }

            if (uiState.frequencyType == "INTERVAL") {
                OutlinedTextField(
                    value = uiState.frequencyHours,
                    onValueChange = { viewModel.onFrequencyHoursChange(it.filter { c -> c.isDigit() }.take(3)) },
                    label = { Text(stringResource(R.string.med_form_interval_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text(stringResource(R.string.med_form_interval_suffix)) }
                )
                TimeInputField(
                    value = uiState.intervalStartTime,
                    onValueChange = viewModel::onIntervalStartTimeChange,
                    label = stringResource(R.string.med_form_first_dose_label),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            stringResource(R.string.med_form_first_dose_hint),
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
                                label = stringResource(R.string.med_form_fixed_time_label, index + 1),
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.fixedTimes.size > 1) {
                                IconButton(onClick = { viewModel.removeFixedTime(index) }) {
                                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.med_form_remove_time_cd))
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
                    Text(stringResource(R.string.med_form_add_time_btn))
                }
            }

            SectionLabel(stringResource(R.string.med_form_duration_section))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = uiState.isContinuous,
                    onCheckedChange = viewModel::onContinuousChange
                )
                Text(stringResource(R.string.med_form_continuous))
            }

            OutlinedTextField(
                value = dateFmt.format(Date(uiState.startDateMillis)),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.med_form_start_date_label)) },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = stringResource(R.string.med_form_start_date_cd))
                    }
                },
                supportingText = { Text(stringResource(R.string.med_form_start_date_hint)) },
                modifier = Modifier.fillMaxWidth()
            )

            if (!uiState.isContinuous) {
                OutlinedTextField(
                    value = uiState.durationDays,
                    onValueChange = { viewModel.onDurationDaysChange(it.filter { c -> c.isDigit() }.take(3)) },
                    label = { Text(stringResource(R.string.med_form_duration_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text(stringResource(R.string.med_form_duration_suffix)) }
                )
            }

            SectionLabel(stringResource(R.string.med_form_guideline_section))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "WITH_FOOD"  to R.string.med_form_guideline_with_food,
                    "FASTING"    to R.string.med_form_guideline_fasting,
                    "WITH_WATER" to R.string.med_form_guideline_with_water,
                    "OTHER"      to R.string.med_form_guideline_other
                ).forEach { (key, labelRes) ->
                    FilterChip(
                        selected = uiState.guideline == key,
                        onClick = { viewModel.onGuidelineChange(key) },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }

            if (uiState.guideline == "OTHER") {
                OutlinedTextField(
                    value = uiState.guidelineDetail,
                    onValueChange = viewModel::onGuidelineDetailChange,
                    label = { Text(stringResource(R.string.med_form_guideline_detail_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }

            OutlinedTextField(
                value = uiState.observations,
                onValueChange = viewModel::onObservationsChange,
                label = { Text(stringResource(R.string.med_form_obs_label)) },
                placeholder = { Text(stringResource(R.string.med_form_obs_hint)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showNotifDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(stringResource(if (medicationId == null) R.string.med_form_save_btn else R.string.med_form_update_btn))
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
