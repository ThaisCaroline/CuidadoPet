package com.cuidadopet.ui.screens.medication

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.cuidadopet.R
import com.cuidadopet.data.db.entity.MedicationEntity
import com.cuidadopet.ui.components.AdBanner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MedicationListScreen(
    petId: Long,
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
    onEditClick: ((Long) -> Unit)? = null,
    onOpenPaywall: () -> Unit = {},
    viewModel: MedicationListViewModel = hiltViewModel()
) {
    LaunchedEffect(petId) { viewModel.loadMedications(petId) }

    val medications by viewModel.medications.collectAsStateWithLifecycle()

    var pendingDelete  by remember { mutableStateOf<MedicationEntity?>(null) }
    var showLimitDialog by remember { mutableStateOf(false) }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text(stringResource(R.string.dialog_med_limit_title)) },
            text  = { Text(stringResource(R.string.dialog_med_limit_msg)) },
            confirmButton = {
                Button(onClick = { showLimitDialog = false; onOpenPaywall() }) { Text(stringResource(R.string.dialog_med_limit_premium)) }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) { Text(stringResource(R.string.dialog_med_limit_later)) }
            }
        )
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title   = { Text(stringResource(R.string.dialog_med_delete_title)) },
            text    = { Text(stringResource(R.string.dialog_med_delete_msg, pendingDelete!!.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deactivateMedication(pendingDelete!!.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (medications.size >= 20) showLimitDialog = true
                    else onAddClick?.invoke()
                },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.Add, stringResource(R.string.med_list_add_cd),
                    tint = MaterialTheme.colorScheme.onSecondary)
            }
        }
    ) { innerPadding ->
        if (medications.isEmpty()) {
            EmptyMedicationsContent(Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(medications, key = { it.id }) { med ->
                    MedicationCard(
                        medication = med,
                        onEdit   = { onEditClick?.invoke(med.id) },
                        onDelete = { pendingDelete = med }
                    )
                }
                item { AdBanner() }
            }
        }
    }
}

@Composable
private fun EmptyMedicationsContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Medication, null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.med_list_empty_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.med_list_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationCard(
    medication: MedicationEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = medication.name,
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (medication.reminderEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = if (medication.reminderEnabled) stringResource(R.string.med_list_reminder_on_cd) else stringResource(R.string.med_list_reminder_off_cd),
                    tint = if (medication.reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, stringResource(R.string.med_list_edit_cd),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, stringResource(R.string.med_list_delete_cd),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(4.dp))

            val formLabel = when (medication.form) {
                "ORAL"       -> stringResource(R.string.dose_form_oral)
                "TOPICAL"    -> stringResource(R.string.dose_form_topical)
                "INJECTABLE" -> stringResource(R.string.dose_form_injectable)
                "EYE_DROP"   -> stringResource(R.string.dose_form_eye_drop)
                else         -> stringResource(R.string.dose_form_other)
            }
            Text("${medication.dose} ${medication.doseUnit} • $formLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(4.dp))

            val frequencyText = when (medication.frequencyType) {
                "INTERVAL"    -> stringResource(R.string.med_list_interval, medication.frequencyHours ?: 0)
                "FIXED_TIMES" -> medication.fixedTimes
                    ?.removeSurrounding("[", "]")
                    ?.split(",")
                    ?.joinToString(" • ") { it.trim().removeSurrounding("\"") }
                    ?: ""
                else -> ""
            }
            val badge = if (medication.isContinuous) stringResource(R.string.med_list_continuous) else stringResource(R.string.med_list_with_deadline)
            Text("$badge • $frequencyText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(2.dp))

            val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            val dateRangeText = if (medication.isContinuous) {
                stringResource(R.string.med_list_since, fmt.format(Date(medication.startDate)))
            } else if (medication.endDate != null) {
                stringResource(R.string.med_list_from_to, fmt.format(Date(medication.startDate)), fmt.format(Date(medication.endDate)))
            } else null

            if (dateRangeText != null) {
                Text(dateRangeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            val isExpired = !medication.isContinuous &&
                medication.endDate != null &&
                medication.endDate < System.currentTimeMillis()

            if (isExpired) {
                Spacer(Modifier.height(6.dp))
                SuggestionChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.med_list_completed), style = MaterialTheme.typography.labelSmall) },
                    icon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor     = MaterialTheme.colorScheme.onSecondaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
    }
}
