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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cuidadopet.R
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding
import com.cuidadopet.ui.utils.TimeInputField
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
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onNavigateBack() }

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_water_config_title)) },
            text = { Text(stringResource(R.string.dialog_delete_water_config_msg)) },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; viewModel.delete(petId) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val petName = state.petName.ifBlank { stringResource(R.string.loading) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.water_config_title, petName)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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

            OutlinedTextField(
                value = state.dailyTargetMl,
                onValueChange = { viewModel.updateTargetMl(it.filter { c -> c.isDigit() }.take(5)) },
                label = { Text(stringResource(R.string.water_config_daily_goal_label)) },
                placeholder = { Text(stringResource(R.string.water_config_daily_goal_hint)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text(stringResource(R.string.water_config_daily_goal_suffix)) }
            )

            Text(stringResource(R.string.water_config_reminders_section), style = MaterialTheme.typography.titleSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.water_config_enable_reminders), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.water_config_enable_reminders_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.remindersEnabled,
                    onCheckedChange = viewModel::updateRemindersEnabled
                )
            }

            if (state.remindersEnabled) {
                OutlinedTextField(
                    value = state.reminderIntervalHours,
                    onValueChange = { viewModel.updateReminderInterval(it.filter { c -> c.isDigit() }.take(2)) },
                    label = { Text(stringResource(R.string.water_config_interval_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text(stringResource(R.string.water_config_interval_suffix)) },
                    supportingText = {
                        Text(
                            stringResource(R.string.water_config_interval_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                TimeInputField(
                    value = state.reminderStartTime,
                    onValueChange = viewModel::updateReminderStartTime,
                    label = stringResource(R.string.water_config_start_time_label),
                    modifier = Modifier.fillMaxWidth()
                )

                val noRemindersText = stringResource(R.string.water_config_no_reminders_in_window)
                val endTimeHint     = stringResource(R.string.water_config_end_time_hint)
                TimeInputField(
                    value = state.reminderEndTime,
                    onValueChange = viewModel::updateReminderEndTime,
                    label = stringResource(R.string.water_config_end_time_label),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        val interval = state.reminderIntervalHours.toIntOrNull() ?: 0
                        val start    = state.reminderStartTime
                        val end      = state.reminderEndTime
                        val timeRegex = Regex("""\d{1,2}:\d{2}""")
                        if (interval > 0 && start.matches(timeRegex) && end.matches(timeRegex)) {
                            val sParts = start.split(":")
                            val sh = sParts[0].toIntOrNull() ?: 0
                            val sm = sParts[1].toIntOrNull() ?: 0
                            val eParts = end.split(":")
                            val endMin = (eParts[0].toIntOrNull() ?: 0) * 60 + (eParts[1].toIntOrNull() ?: 0)
                            val times = (0 until (24 / interval)).map { i ->
                                val totalMin = sh * 60 + sm + i * interval * 60
                                totalMin to "%02d:%02d".format((totalMin / 60) % 24, totalMin % 60)
                            }.filter { (min, _) -> min < endMin }.map { it.second }
                            if (times.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.water_config_reminders_preview,
                                        times.take(5).joinToString(", ") + if (times.size > 5) "..." else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    noRemindersText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Text(
                                endTimeHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

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
                    Text(stringResource(R.string.water_config_save_btn))
                }
            }

            if (state.dailyTargetMl.isNotBlank()) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.water_config_delete_btn))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
