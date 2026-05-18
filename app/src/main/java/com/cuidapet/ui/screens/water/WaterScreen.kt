package com.cuidadopet.ui.screens.water

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidadopet.data.db.entity.WaterLogEntity
import com.cuidadopet.domain.HydrationStatus
import com.cuidadopet.domain.toDisplayText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import com.cuidadopet.ui.components.AdBanner
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding

// Formata um timestamp para exibir só a hora — ex: "14:30"
private val hourFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR"))

private val quickAmounts = listOf(10.0, 20.0, 30.0, 40.0, 50.0)

// Aba "Água" do dashboard — mostra o total bebido hoje, barra de progresso e histórico
@Composable
fun WaterTabContent(
    petId: Long,
    onConfigureWater: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WaterViewModel = hiltViewModel()
) {
    LaunchedEffect(petId) { viewModel.loadWaterData(petId) }

    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.config == null) {
        // Sem configuração — convida o tutor a configurar
        NoWaterConfigContent(onConfigureWater = onConfigureWater, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = adaptiveHorizontalPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }

        // Card de progresso do dia
        item {
            WaterProgressCard(
                totalMl = state.totalMlToday,
                targetMl = state.config!!.dailyTargetMl,
                percentage = state.hydrationPercentage,
                status = state.hydrationStatus,
                onConfigureWater = onConfigureWater
            )
        }

        // Botões de registro rápido + campo livre
        item {
            Text("Registrar consumo", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            QuickAddButtons(onAdd = { ml -> viewModel.addWaterLog(petId, ml) })
            Spacer(Modifier.height(8.dp))
            CustomAmountInput(onAdd = { ml -> viewModel.addWaterLog(petId, ml) })
        }

        // Histórico do dia
        if (state.logsToday.isNotEmpty()) {
            item {
                HorizontalDivider()
                Text(
                    "Registros de hoje",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(state.logsToday.reversed(), key = { it.id }) { log ->
                WaterLogRow(
                    log = log,
                    onDelete = { viewModel.deleteLog(log.id) }
                )
            }
        }

        // item { AdBanner() }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// Conteúdo exibido quando não há configuração de água
@Composable
private fun NoWaterConfigContent(
    onConfigureWater: () -> Unit,
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
            text = "Hidratação não configurada",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Configure a meta diária de água para acompanhar a hidratação do seu pet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onConfigureWater) {
            Text("Configurar hidratação")
        }
    }
}

// Card com barra de progresso de hidratação do dia
@Composable
private fun WaterProgressCard(
    totalMl: Double,
    targetMl: Double,
    percentage: Int,
    status: HydrationStatus,
    onConfigureWater: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Água hoje", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onConfigureWater) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Configurar hidratação",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Valores: total bebido / meta
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${totalMl.toInt()} ml",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    " / ${targetMl.toInt()} ml",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Barra de progresso — limitada a 100% visualmente
            LinearProgressIndicator(
                progress = { min(percentage / 100f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    percentage >= 80 -> MaterialTheme.colorScheme.primary
                    percentage >= 50 -> MaterialTheme.colorScheme.secondary
                    else             -> MaterialTheme.colorScheme.error
                }
            )

            Text(
                "$percentage% da meta diária",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Status de hidratação
            Text(
                status.toDisplayText(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// Botões rápidos para registrar quantidades predefinidas
@Composable
private fun QuickAddButtons(onAdd: (Double) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        quickAmounts.forEach { ml ->
            FilledTonalButton(
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
}

// Campo de quantidade personalizada — permite digitar qualquer valor em ml
@Composable
private fun CustomAmountInput(onAdd: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.filter { c -> c.isDigit() } },
            label = { Text("Outro valor") },
            suffix = { Text("ml") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(
            onClick = {
                val ml = text.toDoubleOrNull()
                if (ml != null && ml > 0) {
                    onAdd(ml)
                    text = ""
                }
            },
            enabled = text.toDoubleOrNull() != null && (text.toDoubleOrNull() ?: 0.0) > 0
        ) { Text("Registrar") }
    }
}

// Linha de um registro de água com botão de deletar
@Composable
private fun WaterLogRow(
    log: WaterLogEntity,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    hourFormat.format(Date(log.registeredAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "  ${log.amountMl.toInt()} ml",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!log.notes.isNullOrBlank()) {
                Text(
                    log.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remover registro",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
