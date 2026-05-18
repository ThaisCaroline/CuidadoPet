package com.cuidadopet.ui.screens.feeding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
import com.cuidadopet.data.db.entity.SporadicMealLogEntity
import com.cuidadopet.domain.FeedingStatus
import com.cuidadopet.domain.toDisplayText
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@Composable
fun FeedingTabContent(
    petId: Long,
    onConfigurePlan: (planId: Long?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedingViewModel = hiltViewModel()
) {
    LaunchedEffect(petId) {
        viewModel.loadFeedingData(petId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.plans.isEmpty()) {
        NoPlanContent(
            onConfigurePlan = { onConfigurePlan(null) },
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = adaptiveHorizontalPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }

        item {
            DailySummaryCard(
                plans        = state.plans,
                logs         = state.logs,
                sporadicLogs = state.sporadicLogs,
                status       = state.dailyStatus
            )
        }

        state.plans.forEach { planWithMeals ->
            item { HorizontalDivider() }

            item {
                PlanSectionHeader(
                    plan     = planWithMeals.plan,
                    onEdit   = { onConfigurePlan(planWithMeals.plan.id) },
                    onDelete = { viewModel.deletePlan(petId, planWithMeals.plan.id) }
                )
            }

            items(planWithMeals.meals, key = { it.id }) { meal ->
                MealCard(
                    meal     = meal,
                    log      = state.logs[meal.id],
                    onLogMeal = { percentage, appetite, notes ->
                        viewModel.logMeal(meal.id, percentage, appetite, notes)
                    }
                )
            }
        }

        item {
            OutlinedButton(
                onClick  = { onConfigurePlan(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Adicionar plano alimentar")
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun NoPlanContent(
    onConfigurePlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Nenhum plano alimentar configurado",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Configure o plano para registrar as refeições e acompanhar a alimentação do seu pet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onConfigurePlan) {
            Text("Configurar plano alimentar")
        }
    }
}

@Composable
private fun DailySummaryCard(
    plans: List<PlanWithMeals>,
    logs: Map<Long, MealLogEntity>,
    sporadicLogs: List<SporadicMealLogEntity>,
    status: FeedingStatus?
) {
    val allMeals = plans.flatMap { it.meals }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Resumo do dia", style = MaterialTheme.typography.titleSmall)

            status?.let {
                Text(it.toDisplayText(), style = MaterialTheme.typography.bodyMedium)
            }

            val planByUnit = allMeals
                .groupBy { it.quantityUnit }
                .mapValues { (_, mealsInUnit) ->
                    mealsInUnit.sumOf { meal ->
                        val log = logs[meal.id]
                        if (log != null) meal.quantityGrams * log.eatenPercentage / 100.0 else 0.0
                    }
                }
                .filter { it.value > 0 }

            val sporadicByUnit = sporadicLogs
                .filter { it.amountGrams != null }
                .groupBy { it.amountUnit }
                .mapValues { (_, entries) -> entries.sumOf { it.amountGrams ?: 0.0 } }

            val allUnits = (planByUnit.keys + sporadicByUnit.keys).toSet()
            if (allUnits.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "Consumo de hoje",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                allUnits.sorted().forEach { unit ->
                    val planAmt  = planByUnit[unit]
                    val extraAmt = sporadicByUnit[unit]
                    val totalAmt = (planAmt ?: 0.0) + (extraAmt ?: 0.0)
                    val line = when {
                        planAmt != null && extraAmt != null ->
                            "${planAmt.toInt()}$unit plano + ${extraAmt.toInt()}$unit extras = ${totalAmt.toInt()}$unit"
                        planAmt != null -> "${planAmt.toInt()}$unit (plano)"
                        else -> "${extraAmt!!.toInt()}$unit (extras)"
                    }
                    Text(line, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PlanSectionHeader(
    plan: MealPlanEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir plano alimentar?") },
            text = { Text("O plano e todas as refeições configuradas serão removidos. Os registros do histórico serão mantidos.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                foodTypeLabel(plan.foodType),
                style = MaterialTheme.typography.titleSmall
            )
            if (!plan.foodDetails.isNullOrBlank()) {
                Text(
                    plan.foodDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!plan.restrictions.isNullOrBlank()) {
                Text(
                    "Restrições: ${plan.restrictions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = onEdit,
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Text(" Editar", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Excluir", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MealCard(
    meal: MealEntity,
    log: MealLogEntity?,
    onLogMeal: (eatenPercentage: Int, appetiteStatus: String, notes: String) -> Unit
) {
    var expanded by remember { mutableStateOf(log == null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (log != null)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(meal.timeOfDay, style = MaterialTheme.typography.titleMedium)
                    if (meal.quantityGrams > 0) {
                        Text(
                            "${meal.quantityGrams.toInt()} ${meal.quantityUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (log != null && !expanded) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            appetiteLabel(log.appetiteStatus),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${log.eatenPercentage}% comido",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { expanded = true },
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Text("Editar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                MealLogInput(
                    initialPercentage = log?.eatenPercentage ?: 100,
                    initialAppetite   = log?.appetiteStatus ?: "ALL",
                    initialNotes      = log?.notes ?: "",
                    onSave = { pct, appetite, notes ->
                        onLogMeal(pct, appetite, notes)
                        expanded = false
                    },
                    onCancel    = { expanded = false },
                    showCancel  = log != null
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealLogInput(
    initialPercentage: Int,
    initialAppetite: String,
    initialNotes: String,
    onSave: (Int, String, String) -> Unit,
    onCancel: () -> Unit,
    showCancel: Boolean
) {
    var selectedPct by remember { mutableIntStateOf(initialPercentage) }
    var selectedAppetite by remember { mutableStateOf(initialAppetite) }
    var notes by remember { mutableStateOf(initialNotes) }

    val percentageOptions = listOf(0, 25, 50, 75, 100)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Quanto o pet comeu?", style = MaterialTheme.typography.labelMedium)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            percentageOptions.forEach { pct ->
                FilterChipCompact(
                    selected = selectedPct == pct,
                    onClick  = {
                        selectedPct = pct
                        selectedAppetite = when (pct) {
                            100  -> "ALL"
                            0    -> "REFUSED"
                            else -> "PARTIAL"
                        }
                    },
                    label = "$pct%"
                )
            }
        }

        Text("Apetite:", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChipCompact(selected = selectedAppetite == "ALL",     onClick = { selectedAppetite = "ALL" },     label = "Comeu tudo")
            FilterChipCompact(selected = selectedAppetite == "PARTIAL", onClick = { selectedAppetite = "PARTIAL" }, label = "Parcial")
            FilterChipCompact(selected = selectedAppetite == "REFUSED", onClick = { selectedAppetite = "REFUSED" }, label = "Recusou")
        }

        androidx.compose.material3.OutlinedTextField(
            value         = notes,
            onValueChange = { notes = it },
            label         = { Text("Observações (opcional)") },
            placeholder   = { Text("Ex: comeu devagar, misturou com patê...") },
            modifier      = Modifier.fillMaxWidth(),
            maxLines      = 3
        )

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (showCancel) {
                OutlinedButton(onClick = onCancel) { Text("Cancelar") }
            }
            Button(onClick = { onSave(selectedPct, selectedAppetite, notes) }) {
                Text("Salvar registro")
            }
        }
    }
}

@Composable
private fun FilterChipCompact(selected: Boolean, onClick: () -> Unit, label: String) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(label, style = MaterialTheme.typography.labelSmall, softWrap = false, maxLines = 1) }
    )
}

private fun foodTypeLabel(code: String): String = when (code) {
    "DRY_KIBBLE"  -> "Ração seca"
    "WET_FOOD"    -> "Ração úmida"
    "NATURAL"     -> "Alimentação natural/caseira"
    "THERAPEUTIC" -> "Dieta terapêutica"
    else          -> "Outro"
}

private fun appetiteLabel(code: String): String = when (code) {
    "ALL"     -> "Comeu tudo"
    "PARTIAL" -> "Comeu parcialmente"
    "REFUSED" -> "Recusou"
    else      -> code
}
