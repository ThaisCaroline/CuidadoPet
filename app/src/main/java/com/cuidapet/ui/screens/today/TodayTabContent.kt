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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding

@Composable
fun TodayTabContent(
    petId: Long,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel()
) {
    LaunchedEffect(petId) { viewModel.load(petId) }

    val state by viewModel.state.collectAsStateWithLifecycle()

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

        // ── Hidratação do dia ─────────────────────────────────────────────────
        SectionHeader("Hidratação hoje")

        WaterCard(
            totalMl  = state.waterTotalMl,
            targetMl = state.waterTargetMl,
            onAdd    = { ml -> viewModel.addWater(petId, ml) }
        )

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

    val status    = item.log?.status
    val isPending = status == null
    val isTaken   = status == "TAKEN"

    // Cor de fundo do card reflete o status da dose
    val containerColor = when {
        isTaken            -> MaterialTheme.colorScheme.tertiaryContainer
        status == "NOT_TAKEN" -> MaterialTheme.colorScheme.errorContainer
        else               -> MaterialTheme.colorScheme.surfaceVariant
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
                isTaken -> Icon(Icons.Default.Check, contentDescription = "Administrado",
                    tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                status == "NOT_TAKEN" -> Icon(Icons.Default.Close, contentDescription = "Não administrado",
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
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
            text             = { Text("O que aconteceu com esta dose?") },
            confirmButton = {
                Button(onClick = { onMark("TAKEN"); showDialog = false }) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Text("  Administrou")
                }
            },
            dismissButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { onMark("VOMITED"); showDialog = false },
                        colors  = ButtonDefaults.outlinedButtonColors()
                    ) { Text("Administrou e vomitou") }
                    TextButton(onClick = { onMark("NOT_TAKEN"); showDialog = false }) {
                        Text("Não administrou")
                    }
                }
            }
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

    val containerColor = if (isLogged) MaterialTheme.colorScheme.tertiaryContainer
                         else          MaterialTheme.colorScheme.surfaceVariant

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
                    "${item.meal.quantityGrams}g",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (isLogged && percentage != null) {
                Text(
                    "$percentage% comido",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
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
            title = { Text("${item.meal.timeOfDay} — ${item.meal.quantityGrams}g") },
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                listOf(50.0, 100.0, 150.0, 200.0).forEach { ml ->
                    OutlinedButton(
                        onClick        = { onAdd(ml) },
                        modifier       = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text("+${ml.toInt()}", style = MaterialTheme.typography.labelSmall)
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
