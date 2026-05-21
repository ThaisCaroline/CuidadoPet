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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
import com.cuidadopet.ui.components.AdBanner
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding

@Composable
fun FeedingTabContent(
    petId: Long,
    onConfigurePlan: (planId: Long?) -> Unit,
    onOpenPaywall: () -> Unit = {},
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

    var showPlanLimitDialog by remember { mutableStateOf(false) }

    if (showPlanLimitDialog) {
        AlertDialog(
            onDismissRequest = { showPlanLimitDialog = false },
            title = { Text("Limite atingido") },
            text  = { Text("Você atingiu o limite de 5 planos do plano gratuito.") },
            confirmButton = {
                Button(onClick = { showPlanLimitDialog = false; onOpenPaywall() }) { Text("Ver Premium") }
            },
            dismissButton = {
                TextButton(onClick = { showPlanLimitDialog = false }) { Text("Agora não") }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = adaptiveHorizontalPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }

        state.plans.forEach { planWithMeals ->
            item {
                PlanSectionHeader(
                    plan     = planWithMeals.plan,
                    onEdit   = { onConfigurePlan(planWithMeals.plan.id) },
                    onDelete = { viewModel.deletePlan(petId, planWithMeals.plan.id) }
                )
            }

            items(planWithMeals.meals, key = { it.id }) { meal ->
                MealInfoCard(meal = meal)
            }

            item { HorizontalDivider() }
        }

        item {
            OutlinedButton(
                onClick  = {
                    if (state.plans.size >= 5) showPlanLimitDialog = true
                    else onConfigurePlan(null)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Adicionar plano alimentar")
            }
        }

        // item { AdBanner() }
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
private fun MealInfoCard(meal: MealEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(meal.timeOfDay, style = MaterialTheme.typography.bodyMedium)
            if (meal.quantityGrams > 0) {
                Text(
                    "${meal.quantityGrams.toInt()} ${meal.quantityUnit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun foodTypeLabel(code: String): String = when (code) {
    "DRY_KIBBLE"  -> "Ração seca"
    "WET_FOOD"    -> "Ração úmida"
    "NATURAL"     -> "Alimentação natural/caseira"
    "THERAPEUTIC" -> "Dieta terapêutica"
    else          -> "Outro"
}
