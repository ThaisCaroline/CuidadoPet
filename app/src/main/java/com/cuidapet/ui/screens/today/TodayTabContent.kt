package com.cuidadopet.ui.screens.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.cuidadopet.ui.components.AdBanner
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding

@Composable
fun TodayTabContent(
    petId: Long,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel()
) {
    LaunchedEffect(petId) { viewModel.load(petId) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(petId, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load(petId)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSporadicDialog by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = adaptiveHorizontalPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Medicamentos do dia ───────────────────────────────────────────────
        SectionHeader("Medicamentos hoje")

        if (state.doses.isEmpty()) {
            EmptySection("Nenhuma dose programada para hoje.")
        } else {
            state.doses.forEach { item ->
                DoseCard(
                    item    = item,
                    onMark  = { status -> viewModel.markDose(item.medication, item.scheduledAt, status) }
                )
            }
        }

        // ── Refeições do dia ──────────────────────────────────────────────────
        SectionHeader("Refeições hoje")

        if (state.meals.isEmpty()) {
            EmptySection("Nenhum plano alimentar configurado.\nConfigure na aba Alimentação.")
        } else {
            state.meals.forEach { item ->
                MealCard(
                    item   = item,
                    onMark = { pct, status -> viewModel.markMeal(item.meal, pct, status) }
                )
            }
        }

        // ── Refeições esporádicas ─────────────────────────────────────────────
        val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR")) }
        state.sporadicLogs.forEach { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            timeFmt.format(Date(log.registeredAt)),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            log.description ?: "Petisco",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (log.amountGrams != null) {
                            Text(
                                "${log.amountGrams.toInt()}${log.amountUnit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.deleteSporadicMeal(log.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remover extra",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        TextButton(
            onClick = { showSporadicDialog = true },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("+ Extra")
        }

        if (showSporadicDialog) {
            SporadicMealDialog(
                onConfirm = { desc, amount, unit ->
                    viewModel.addSporadicMeal(petId, desc, amount, unit)
                    showSporadicDialog = false
                },
                onDismiss = { showSporadicDialog = false }
            )
        }

        // ── Hidratação do dia ─────────────────────────────────────────────────
        SectionHeader("Hidratação hoje")

        WaterCard(
            totalMl  = state.waterTotalMl,
            targetMl = state.waterTargetMl,
            onAdd    = { ml -> viewModel.addWater(petId, ml) }
        )

        AdBanner()
        Spacer(Modifier.height(16.dp))
    }
}

// ── Dose card ─────────────────────────────────────────────────────────────────

@Composable
private fun DoseCard(
    item: TodayDoseItem,
    onMark: (String) -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR")) }
    var showDialog by remember { mutableStateOf(false) }

    val status     = item.log?.status
    val isPending  = status == null
    val isTaken    = status == "TAKEN"
    val isVomited  = status == "VOMITED"

    // Cor de fundo do card reflete o status da dose
    val containerColor = when {
        isTaken               -> MaterialTheme.colorScheme.tertiaryContainer
        status == "NOT_TAKEN" -> MaterialTheme.colorScheme.errorContainer
        isVomited             -> Color(0xFFFFF8E1)   // âmbar claro — dose problemática
        else                  -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        onClick  = { if (isPending) showDialog = true }
    ) {
        Row(
            modifier              = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    timeFmt.format(Date(item.scheduledAt)),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Text(
                    item.medication.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${item.medication.dose} ${item.medication.doseUnit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Ícone / label de status
            when {
                isTaken -> Icon(
                    Icons.Default.Check, contentDescription = "Administrado",
                    tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp)
                )
                status == "NOT_TAKEN" -> Icon(
                    Icons.Default.Close, contentDescription = "Não administrado",
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp)
                )
                isVomited -> Icon(
                    Icons.Default.Warning, contentDescription = "Vomitou após a dose",
                    tint = Color(0xFFF57F17), modifier = Modifier.size(24.dp)
                )
                else -> Text(
                    "A REGISTRAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Diálogo para registrar o que aconteceu com a dose
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title            = { Text("${item.medication.name} — ${timeFmt.format(Date(item.scheduledAt))}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val formLabel = when (item.medication.form) {
                        "ORAL"       -> "Oral"
                        "TOPICAL"    -> "Tópico"
                        "INJECTABLE" -> "Injetável"
                        "EYE_DROP"   -> "Colírio"
                        else         -> "Outro"
                    }
                    Text(
                        "${item.medication.dose} ${item.medication.doseUnit} · $formLabel",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    val guidelineText = when (item.medication.administrationGuideline) {
                        "WITH_FOOD"  -> "Administrar com alimento"
                        "FASTING"    -> "Administrar em jejum"
                        "WITH_WATER" -> "Diluir em água"
                        "OTHER"      -> item.medication.guidelineDetail
                        else         -> null
                    }
                    if (!guidelineText.isNullOrBlank()) {
                        Text(
                            guidelineText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!item.medication.observations.isNullOrBlank()) {
                        Text(
                            "Obs: ${item.medication.observations}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(2.dp))

                    Text("O que aconteceu com esta dose?", style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(2.dp))
                    Button(
                        onClick  = { onMark("TAKEN"); showDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Text("  Administrou")
                    }
                    OutlinedButton(
                        onClick  = { onMark("VOMITED"); showDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Administrou e vomitou") }
                    TextButton(
                        onClick  = { onMark("NOT_TAKEN"); showDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Não administrou") }
                }
            },
            confirmButton = {}
        )
    }
}

// ── Meal card ─────────────────────────────────────────────────────────────────

@Composable
private fun MealCard(
    item:   TodayMealItem,
    onMark: (Int, String) -> Unit
) {
    val isLogged   = item.log != null
    val percentage = item.log?.eatenPercentage
    var showDialog by remember { mutableStateOf(false) }

    val containerColor = when {
        !isLogged                       -> MaterialTheme.colorScheme.surfaceVariant
        (percentage ?: 0) == 0         -> MaterialTheme.colorScheme.errorContainer  // recusou
        (percentage ?: 0) <= 25        -> Color(0xFFFFF8E1)                          // comeu pouco
        else                           -> MaterialTheme.colorScheme.tertiaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        onClick  = { showDialog = true }
    ) {
        Row(
            modifier              = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    item.meal.timeOfDay,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${item.meal.quantityGrams}${item.meal.quantityUnit}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (item.planFoodType.isNotBlank()) {
                    val foodLabel = buildString {
                        append(foodTypeLabel(item.planFoodType))
                        if (!item.planFoodDetails.isNullOrBlank()) append(" · ${item.planFoodDetails}")
                    }
                    Text(
                        foodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isLogged && percentage != null) {
                val textColor = when {
                    percentage == 0  -> MaterialTheme.colorScheme.error
                    percentage <= 25 -> Color(0xFFF57F17)
                    else             -> MaterialTheme.colorScheme.tertiary
                }
                Text(
                    if (percentage == 0) "Recusou" else "$percentage% comido",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor
                )
            } else {
                Text(
                    "A REGISTRAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("${item.meal.timeOfDay} — ${item.meal.quantityGrams}${item.meal.quantityUnit}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Como foi esta refeição?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Button(onClick = { onMark(100, "ALL"); showDialog = false },
                           modifier = Modifier.fillMaxWidth()) { Text("Comeu tudo (100%)") }
                    OutlinedButton(onClick = { onMark(75, "PARTIAL"); showDialog = false },
                                   modifier = Modifier.fillMaxWidth()) { Text("Comeu bastante (75%)") }
                    OutlinedButton(onClick = { onMark(50, "PARTIAL"); showDialog = false },
                                   modifier = Modifier.fillMaxWidth()) { Text("Comeu metade (50%)") }
                    OutlinedButton(onClick = { onMark(25, "PARTIAL"); showDialog = false },
                                   modifier = Modifier.fillMaxWidth()) { Text("Comeu pouco (25%)") }
                    TextButton(onClick = { onMark(0, "REFUSED"); showDialog = false },
                               modifier = Modifier.fillMaxWidth()) { Text("Recusou") }
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick  = { showDialog = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text("Cancelar") }
                }
            },
            confirmButton = {}
        )
    }
}

// ── Water card ────────────────────────────────────────────────────────────────

@Composable
private fun WaterCard(
    totalMl: Double,
    targetMl: Double?,
    onAdd: (Double) -> Unit
) {
    val waterProgress = if (targetMl != null && targetMl > 0) totalMl / targetMl else null
    val waterContainerColor = when {
        waterProgress == null       -> MaterialTheme.colorScheme.surfaceVariant  // sem meta
        totalMl == 0.0              -> MaterialTheme.colorScheme.errorContainer  // não bebeu nada
        waterProgress < 0.5         -> Color(0xFFFFF8E1)                          // bebeu pouco
        else                        -> MaterialTheme.colorScheme.tertiaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = waterContainerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Linha com ícone e valores
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(20.dp)
                    )
                    Text(
                        "%.0f ml".format(totalMl),
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (targetMl != null) {
                    val pct = (totalMl / targetMl * 100).toInt().coerceIn(0, 100)
                    Text(
                        "meta: %.0f ml ($pct%%)".format(targetMl),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Barra de progresso (só aparece se tiver meta configurada)
            if (targetMl != null) {
                val progress = (totalMl / targetMl).toFloat().coerceIn(0f, 1f)
                val barColor = when {
                    progress >= 1f   -> Color(0xFF2E7D32)  // verde escuro: meta atingida
                    progress >= 0.5f -> MaterialTheme.colorScheme.primary
                    else             -> MaterialTheme.colorScheme.error
                }
                LinearProgressIndicator(
                    progress          = { progress },
                    modifier          = Modifier.fillMaxWidth().height(6.dp),
                    color             = barColor,
                    trackColor        = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }

            // Botões de adição rápida
            Text("Registrar:", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10.0, 20.0, 30.0, 40.0, 50.0).forEach { ml ->
                    OutlinedButton(
                        onClick        = { onAdd(ml) },
                        modifier       = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 4.dp, vertical = 8.dp
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${ml.toInt()}", style = MaterialTheme.typography.labelSmall)
                            Text("ml",           style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Campo para quantidade personalizada
            var customMl by remember { mutableStateOf("") }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value         = customMl,
                    onValueChange = { raw ->
                        val sb = StringBuilder(); var hasSep = false
                        for (c in raw) when {
                            c.isDigit() -> sb.append(c)
                            (c == '.' || c == ',') && !hasSep -> { sb.append(c); hasSep = true }
                        }
                        customMl = sb.toString()
                    },
                    label          = { Text("Outro valor") },
                    placeholder    = { Text("Ex: 80") },
                    suffix         = { Text("ml") },
                    modifier       = Modifier.weight(1f),
                    singleLine     = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Button(
                    onClick  = {
                        val ml = customMl.replace(",", ".").toDoubleOrNull()
                        if (ml != null && ml > 0) { onAdd(ml); customMl = "" }
                    },
                    modifier = Modifier.height(56.dp)
                ) { Text("+ Adicionar") }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun foodTypeLabel(code: String): String = when (code) {
    "DRY_KIBBLE"  -> "Ração seca"
    "WET_FOOD"    -> "Ração úmida"
    "NATURAL"     -> "Alimentação natural/caseira"
    "THERAPEUTIC" -> "Dieta terapêutica"
    else          -> "Outro"
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun EmptySection(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
@Composable
private fun SporadicMealDialog(
    onConfirm: (String, Double?, String) -> Unit,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar extra") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (opcional)") },
                    placeholder = { Text("Ex: petisco") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Quantidade (opcional)") },
                        placeholder = { Text("Ex: 30") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        listOf("g", "ml").forEach { option ->
                            androidx.compose.material3.FilterChip(
                                selected = unit == option,
                                onClick = { unit = option },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(description, amount.toDoubleOrNull(), unit) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}