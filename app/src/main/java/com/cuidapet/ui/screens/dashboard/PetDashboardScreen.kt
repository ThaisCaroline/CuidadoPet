package com.cuidadopet.ui.screens.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import java.util.Calendar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidadopet.ui.components.PetAvatar
import com.cuidadopet.ui.screens.feeding.FeedingTabContent
import com.cuidadopet.ui.screens.health.HealthTabContent
import com.cuidadopet.ui.screens.medication.MedicationListScreen
import com.cuidadopet.ui.screens.today.TodayTabContent
import com.cuidadopet.ui.screens.water.WaterTabContent
import com.cuidadopet.ui.utils.isTablet

private data class DashboardTab(
    val index: Int,
    val label: String,
    val icon: @Composable () -> Unit
)

private val dashboardTabs = listOf(
    DashboardTab(0, "Hoje",        { Icon(Icons.Default.Home,            contentDescription = "Hoje") }),
    DashboardTab(1, "Medicação",   { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Medicação") }),
    DashboardTab(2, "Refeições",   { Icon(Icons.Default.Restaurant,       contentDescription = "Refeições") }),
    DashboardTab(3, "Água",        { Icon(Icons.Default.WaterDrop,        contentDescription = "Água") }),
    DashboardTab(4, "Saúde",       { Icon(Icons.Default.Favorite,         contentDescription = "Saúde") })
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDashboardScreen(
    petId: Long,
    onNavigateBack: () -> Unit,
    onEditPet: (Long) -> Unit,
    onAddMedication: () -> Unit = {},
    onEditMedication: (Long) -> Unit = {},
    onConfigureMealPlan: (Long?) -> Unit = {},
    onConfigureWater: () -> Unit = {},
    onNewHealthEntry: () -> Unit = {},
    onEditHealthEntry: (Long) -> Unit = {},
    onWeightHistory: () -> Unit = {},
    onReport: () -> Unit = {},
    onVaccines: () -> Unit = {},
    viewModel: PetDashboardViewModel = hiltViewModel()
) {
    LaunchedEffect(petId) { viewModel.loadPet(petId) }

    val pet by viewModel.pet.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tablet = isTablet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PetAvatar(photoPath = pet?.photoPath, petName = pet?.name ?: "", size = 32.dp)
                        Column {
                            Text(
                                text  = "  ${pet?.name ?: "Carregando..."}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            pet?.birthDate?.let { birthDateMs ->
                                Text(
                                    text  = "  ${petAgeLabel(birthDateMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onEditPet(petId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar pet",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onVaccines) {
                        Icon(Icons.Default.Vaccines, contentDescription = "Vacinas e vermífugos",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onReport) {
                        Icon(Icons.Default.Summarize, contentDescription = "Relatório",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (!tablet) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    dashboardTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab.index,
                            onClick  = { selectedTab = tab.index },
                            icon     = tab.icon,
                            label    = { Text(tab.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (tablet) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                    dashboardTabs.forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab.index,
                            onClick  = { selectedTab = tab.index },
                            icon     = tab.icon,
                            label    = { Text(tab.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                Box(Modifier.weight(1f)) {
                    DashboardTabContent(
                        selectedTab         = selectedTab,
                        petId               = petId,
                        modifier            = Modifier.fillMaxSize(),
                        onAddMedication     = onAddMedication,
                        onEditMedication    = onEditMedication,
                        onConfigureMealPlan = onConfigureMealPlan,
                        onConfigureWater    = onConfigureWater,
                        onNewHealthEntry    = onNewHealthEntry,
                        onEditHealthEntry   = onEditHealthEntry,
                        onWeightHistory     = onWeightHistory
                    )
                }
            }
        } else {
            DashboardTabContent(
                selectedTab         = selectedTab,
                petId               = petId,
                modifier            = Modifier.padding(innerPadding),
                onAddMedication     = onAddMedication,
                onEditMedication    = onEditMedication,
                onConfigureMealPlan = onConfigureMealPlan,
                onConfigureWater    = onConfigureWater,
                onNewHealthEntry    = onNewHealthEntry,
                onEditHealthEntry   = onEditHealthEntry,
                onWeightHistory     = onWeightHistory
            )
        }
    }
}

@Composable
private fun DashboardTabContent(
    selectedTab: Int,
    petId: Long,
    modifier: Modifier = Modifier,
    onAddMedication: () -> Unit,
    onEditMedication: (Long) -> Unit,
    onConfigureMealPlan: (Long?) -> Unit,
    onConfigureWater: () -> Unit,
    onNewHealthEntry: () -> Unit,
    onEditHealthEntry: (Long) -> Unit,
    onWeightHistory: () -> Unit
) {
    when (selectedTab) {
        0 -> TodayTabContent(petId = petId, modifier = modifier)
        1 -> MedicationListScreen(
            petId      = petId,
            modifier   = modifier,
            onAddClick  = onAddMedication,
            onEditClick = onEditMedication
        )
        2 -> FeedingTabContent(
            petId           = petId,
            onConfigurePlan = { planId -> onConfigureMealPlan(planId) },
            modifier        = modifier
        )
        3 -> WaterTabContent(
            petId            = petId,
            onConfigureWater = onConfigureWater,
            modifier         = modifier
        )
        4 -> HealthTabContent(
            petId           = petId,
            onNewEntry      = onNewHealthEntry,
            onEditEntry     = onEditHealthEntry,
            onWeightHistory = onWeightHistory,
            modifier        = modifier
        )
    }
}

private fun petAgeLabel(birthDateMs: Long): String {
    val birth = Calendar.getInstance().also { it.timeInMillis = birthDateMs }
    val today = Calendar.getInstance()
    var years  = today.get(Calendar.YEAR)  - birth.get(Calendar.YEAR)
    var months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
    if (months < 0) { years--; months += 12 }
    if (today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)) {
        if (months == 0) { years--; months = 11 } else months--
    }
    return when {
        years > 0 && months > 0 -> "$years ${if (years == 1) "ano" else "anos"} e $months ${if (months == 1) "mês" else "meses"}"
        years > 0               -> "$years ${if (years == 1) "ano" else "anos"}"
        months > 0              -> "$months ${if (months == 1) "mês" else "meses"}"
        else                    -> "menos de 1 mês"
    }
}
