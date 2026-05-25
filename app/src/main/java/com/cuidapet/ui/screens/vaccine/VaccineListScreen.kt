package com.cuidadopet.ui.screens.vaccine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.cuidadopet.R
import com.cuidadopet.data.db.entity.VaccineEntity
import com.cuidadopet.ui.components.AdBanner
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccineListScreen(
    petId: Long,
    onNavigateBack: () -> Unit,
    onAddVaccine: () -> Unit,
    onEditVaccine: (Long) -> Unit,
    viewModel: VaccineListViewModel = hiltViewModel()
) {
    LaunchedEffect(petId) { viewModel.setPetId(petId) }

    val vaccines by viewModel.vaccines.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<VaccineEntity?>(null) }

    deleteTarget?.let { vaccine ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title            = { Text(stringResource(R.string.dialog_delete_vaccine_title)) },
            text             = { Text(stringResource(R.string.dialog_delete_vaccine_msg, vaccine.name)) },
            confirmButton    = {
                TextButton(onClick = { viewModel.delete(vaccine); deleteTarget = null }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vaccine_list_title)) },
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
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVaccine) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.vaccine_list_add_cd))
            }
        }
    ) { innerPadding ->
        if (vaccines.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Vaccines,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.vaccine_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.vaccine_list_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val vaccinesList  = vaccines.filter { it.type == "VACCINE" }
            val dewormersList = vaccines.filter { it.type == "DEWORMER" }

            LazyColumn(
                modifier            = Modifier.padding(innerPadding).padding(horizontal = adaptiveHorizontalPadding()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                if (vaccinesList.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.vaccine_section_vaccines),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(vaccinesList, key = { it.id }) { vaccine ->
                        VaccineCard(
                            vaccine  = vaccine,
                            onEdit   = { onEditVaccine(vaccine.id) },
                            onDelete = { deleteTarget = vaccine }
                        )
                    }
                }

                if (dewormersList.isNotEmpty()) {
                    item {
                        if (vaccinesList.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        Text(
                            stringResource(R.string.vaccine_section_dewormers),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(dewormersList, key = { it.id }) { vaccine ->
                        VaccineCard(
                            vaccine  = vaccine,
                            onEdit   = { onEditVaccine(vaccine.id) },
                            onDelete = { deleteTarget = vaccine }
                        )
                    }
                }

                item { AdBanner() }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun VaccineCard(
    vaccine: VaccineEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier            = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(vaccine.name, style = MaterialTheme.typography.bodyLarge)
                    if (vaccine.nextDueDate != null) {
                        if (vaccine.reminderEnabled) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = stringResource(R.string.vaccine_alerts_on_cd),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Default.NotificationsOff,
                                contentDescription = stringResource(R.string.vaccine_alerts_off_cd),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (vaccine.administeredAt != null) {
                    Text(
                        stringResource(R.string.vaccine_applied_date, dateFmt.format(Date(vaccine.administeredAt))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        stringResource(R.string.vaccine_not_applied),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (vaccine.nextDueDate != null) {
                    Text(
                        stringResource(R.string.vaccine_next_dose, dateFmt.format(Date(vaccine.nextDueDate))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (!vaccine.notes.isNullOrBlank()) {
                    Text(
                        vaccine.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
