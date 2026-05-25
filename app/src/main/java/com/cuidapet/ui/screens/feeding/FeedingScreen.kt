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
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.cuidadopet.R
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
            title = { Text(stringResource(R.string.dialog_med_limit_title)) },
            text  = { Text(stringResource(R.string.dialog_feeding_plan_limit_msg)) },
            confirmButton = {
                Button(onClick = { showPlanLimitDialog = false; onOpenPaywall() }) { Text(stringResource(R.string.dialog_med_limit_premium)) }
            },
            dismissButton = {
                TextButton(onClick = { showPlanLimitDialog = false }) { Text(stringResource(R.string.dialog_med_limit_later)) }
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
                Text(stringResource(R.string.feeding_add_plan_btn))
            }
        }

        item { AdBanner() }
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
            text = stringResource(R.string.feeding_no_plan_title),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.feeding_no_plan_msg),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onConfigurePlan) {
            Text(stringResource(R.string.feeding_configure_plan_btn))
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
            title = { Text(stringResource(R.string.dialog_delete_plan_title)) },
            text = { Text(stringResource(R.string.dialog_delete_plan_msg)) },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        Column(modifier = Modifier.weight(1f)) {
            Text(
                foodTypeLabel(context, plan.foodType),
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
                    stringResource(R.string.feeding_restrictions, plan.restrictions),
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
                Text(stringResource(R.string.feeding_edit_btn), style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.action_delete), style = MaterialTheme.typography.labelSmall)
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

private fun foodTypeLabel(context: Context, code: String): String = when (code) {
    "DRY_KIBBLE"  -> context.getString(R.string.food_type_dry_kibble)
    "WET_FOOD"    -> context.getString(R.string.food_type_wet_food)
    "NATURAL"     -> context.getString(R.string.food_type_natural_homemade)
    "THERAPEUTIC" -> context.getString(R.string.food_type_therapeutic)
    else          -> context.getString(R.string.food_type_other)
}
