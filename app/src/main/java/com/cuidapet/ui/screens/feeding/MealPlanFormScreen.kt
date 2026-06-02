package com.cuidadopet.ui.screens.feeding

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
import com.cuidadopet.ui.utils.TimeInputField
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val foodTypeOptions = listOf(
    "DRY_KIBBLE"  to R.string.food_type_dry_kibble,
    "WET_FOOD"    to R.string.food_type_wet_food,
    "NATURAL"     to R.string.food_type_natural_homemade,
    "THERAPEUTIC" to R.string.food_type_therapeutic,
    "OTHER"       to R.string.food_type_other
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanFormScreen(
    petId: Long,
    planId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: MealPlanFormViewModel = hiltViewModel()
) {
    LaunchedEffect(petId, planId) { viewModel.loadPlan(petId, planId) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showNotifDialog by remember { mutableStateOf(false) }
    var dialogSuperReminder by remember { mutableStateOf(false) }

    if (showNotifDialog) {
        AlertDialog(
            onDismissRequest = { showNotifDialog = false },
            title = { Text(stringResource(R.string.meal_plan_notif_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.meal_plan_notif_msg))
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = dialogSuperReminder,
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
                    viewModel.setReminderOptions(reminderEnabled = true, isSuperReminder = dialogSuperReminder)
                    viewModel.savePlan(petId, state.petName)
                }) { Text(stringResource(R.string.meal_plan_notif_enable)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotifDialog = false
                    viewModel.setReminderOptions(reminderEnabled = false, isSuperReminder = false)
                    viewModel.savePlan(petId, state.petName)
                }) { Text(stringResource(R.string.meal_plan_notif_skip)) }
            }
        )
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val petName   = state.petName.ifBlank { stringResource(R.string.loading) }
    val isEditing = state.editingPlanId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isEditing) R.string.meal_plan_edit_title else R.string.meal_plan_new_title, petName)) },
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

            Text(stringResource(R.string.meal_plan_food_type_section), style = MaterialTheme.typography.titleSmall)
            FoodTypeSelector(
                selected = state.foodType,
                onSelect = viewModel::updateFoodType
            )

            val foodDetailsPlaceholder = when (state.foodType) {
                "DRY_KIBBLE"  -> stringResource(R.string.meal_plan_food_details_hint_dry)
                "WET_FOOD"    -> stringResource(R.string.meal_plan_food_details_hint_wet)
                "NATURAL"     -> stringResource(R.string.meal_plan_food_details_hint_natural)
                "THERAPEUTIC" -> stringResource(R.string.meal_plan_food_details_hint_therapeutic)
                else          -> stringResource(R.string.meal_plan_food_details_hint_other)
            }
            OutlinedTextField(
                value         = state.foodDetails,
                onValueChange = { if (it.length <= 150) viewModel.updateFoodDetails(it) },
                label         = { Text(stringResource(R.string.meal_plan_food_details_label)) },
                placeholder   = { Text(foodDetailsPlaceholder) },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                supportingText = { Text("${state.foodDetails.length}/150") }
            )

            OutlinedTextField(
                value = state.restrictions,
                onValueChange = viewModel::updateRestrictions,
                label = { Text(stringResource(R.string.meal_plan_restrictions_label)) },
                placeholder = { Text(stringResource(R.string.meal_plan_restrictions_hint)) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Text(
                stringResource(R.string.meal_plan_goals_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.quantityUnit == "g",
                    onClick  = { viewModel.updateQuantityUnit("g") },
                    label    = { Text(stringResource(R.string.meal_plan_unit_grams)) }
                )
                FilterChip(
                    selected = state.quantityUnit == "ml",
                    onClick  = { viewModel.updateQuantityUnit("ml") },
                    label    = { Text(stringResource(R.string.meal_plan_unit_ml)) }
                )
            }

            OutlinedTextField(
                value = state.dailyQuantityGrams,
                onValueChange = { viewModel.updateDailyQuantity(it.filter { c -> c.isDigit() }.take(5)) },
                label = { Text(stringResource(R.string.meal_plan_daily_qty_label, state.quantityUnit)) },
                placeholder = { Text(stringResource(R.string.meal_plan_daily_qty_hint)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text("${state.quantityUnit}/dia") }
            )

            OutlinedTextField(
                value = state.dailyKcalTarget,
                onValueChange = { viewModel.updateDailyKcal(it.filter { c -> c.isDigit() }.take(5)) },
                label = { Text(stringResource(R.string.meal_plan_daily_kcal_label)) },
                placeholder = { Text(stringResource(R.string.meal_plan_daily_kcal_hint)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text(stringResource(R.string.meal_plan_daily_kcal_suffix)) }
            )

            Text(stringResource(R.string.meal_plan_meals_section), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.meal_plan_meals_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            state.meals.forEachIndexed { index, entry ->
                MealEntryRow(
                    entry = entry,
                    onTimeChange = { viewModel.updateMealTime(index, it) },
                    onQuantityChange = { viewModel.updateMealQuantity(index, it) },
                    onRemove = { viewModel.removeMealEntry(index) },
                    canRemove = state.meals.size > 1,
                    unit = state.quantityUnit
                )
            }

            TextButton(
                onClick = viewModel::addMealEntry,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.meal_plan_add_meal_btn))
            }

            Button(
                onClick = {
                    dialogSuperReminder = state.isSuperReminder
                    showNotifDialog = true
                },
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
                    Text(stringResource(if (isEditing) R.string.meal_plan_save_btn else R.string.meal_plan_add_btn))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FoodTypeSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val chunked = foodTypeOptions.chunked(3)
    chunked.forEach { row ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            row.forEach { (key, labelRes) ->
                FilterChip(
                    selected = selected == key,
                    onClick = { onSelect(key) },
                    label = { Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
private fun MealEntryRow(
    entry: MealTimeEntry,
    onTimeChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
    unit: String = "g"
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        TimeInputField(
            value = entry.time,
            onValueChange = onTimeChange,
            label = stringResource(R.string.meal_plan_time_label),
            placeholder = stringResource(R.string.meal_plan_time_hint),
            modifier = Modifier.weight(1f)
        )

        OutlinedTextField(
            value = entry.quantityGrams,
            onValueChange = { new ->
                val filtered = new.filter { it.isDigit() }.take(5)
                onQuantityChange(filtered)
            },
            label = { Text(stringResource(if (unit == "ml") R.string.meal_plan_ml_label else R.string.meal_plan_grams_label)) },
            placeholder = { Text(stringResource(R.string.meal_plan_qty_hint)) },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            suffix = { Text(unit) }
        )

        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.meal_plan_remove_meal_cd),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
