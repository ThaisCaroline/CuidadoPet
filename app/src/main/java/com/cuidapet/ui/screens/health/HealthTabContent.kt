package com.cuidadopet.ui.screens.health

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidadopet.data.db.entity.HealthEntryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.cuidadopet.ui.components.AdBanner
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding

private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"))

// Aba "Saúde" do dashboard — diário de sintomas e atalho para histórico de peso
@Composable
fun HealthTabContent(
    petId: Long,
    onNewEntry: () -> Unit,
    onEditEntry: (Long) -> Unit,
    onWeightHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HealthViewModel = hiltViewModel()
) {
    LaunchedEffect(petId) { viewModel.loadHealthData(petId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = adaptiveHorizontalPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }

        // Card de peso atual + botão para histórico
        item {
            WeightSummaryCard(
                latestWeightKg = state.latestWeight?.weightKg,
                onWeightHistory = onWeightHistory
            )
        }

        item { HorizontalDivider() }

        // Cabeçalho do diário com botão "Nova entrada"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Diário de saúde", style = MaterialTheme.typography.titleSmall)
                Button(onClick = onNewEntry) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(" Nova entrada")
                }
            }
        }

        if (state.entries.isEmpty()) {
            item {
                Text(
                    "Nenhuma entrada registrada ainda.\nUse o botão acima para começar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(state.entries, key = { it.id }) { entry ->
                HealthEntryCard(
                    entry = entry,
                    onEdit = { onEditEntry(entry.id) },
                    onDelete = { viewModel.deleteEntry(entry.id) }
                )
            }
        }

        // item { AdBanner() }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// Card com o peso mais recente e botão para ver o histórico completo
@Composable
private fun WeightSummaryCard(
    latestWeightKg: Double?,
    onWeightHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Peso atual", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (latestWeightKg != null) "$latestWeightKg kg" else "Não registrado",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            FilledTonalButton(onClick = onWeightHistory) {
                Icon(Icons.Default.MonitorWeight, contentDescription = null)
                Text(" Histórico de peso")
            }
        }
    }
}

// Card de uma entrada do diário — resumo compacto das observações do dia
@Composable
private fun HealthEntryCard(
    entry: HealthEntryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onEdit  // toque no card abre para edição
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    dateFormat.format(Date(entry.registeredAt)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remover entrada",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Exibe só os campos que foram preenchidos
            buildEntryLines(entry).forEach { line ->
                Text(line, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// Constrói as linhas de resumo de uma entrada — só mostra campos preenchidos
private fun buildEntryLines(entry: HealthEntryEntity): List<String> {
    val lines = mutableListOf<String>()
    entry.behavior?.let     { lines.add("Comportamento: ${behaviorLabel(it)}") }
    entry.fecesStatus?.let  { lines.add("Fezes: ${fecesLabel(it)}") }
    entry.urineStatus?.let  { lines.add("Urina: ${urineLabel(it)}") }
    entry.vomitCount?.let   { if (it > 0) lines.add("Vômitos: $it episódio(s)") }
    entry.mobility?.let     { if (it != "NORMAL") lines.add("Mobilidade: ${mobilityLabel(it)}") }
    entry.painSigns?.let    { if (it != "NONE") lines.add("Dor: ${painLabel(it)}") }
    entry.observations?.let { lines.add("Obs: $it") }
    return lines
}

private fun behaviorLabel(code: String) = when (code) {
    "NORMAL"    -> "Normal, ativo"
    "LETHARGIC" -> "Apático"
    "AGITATED"  -> "Agitado"
    "SLEEPY"    -> "Sonolento"
    else        -> code
}

private fun fecesLabel(code: String) = when (code) {
    "NORMAL"   -> "Normal"
    "SOFT"     -> "Amolecidas"
    "DIARRHEA" -> "Diarreia"
    "ABSENT"   -> "Não evacuou"
    "BLOOD"    -> "Com sangue ⚠️"
    else       -> code
}

private fun urineLabel(code: String) = when (code) {
    "NORMAL"    -> "Normal"
    "INCREASED" -> "Aumentada"
    "REDUCED"   -> "Reduzida"
    "ABSENT"    -> "Não urinou"
    "BLOOD"     -> "Com sangue ⚠️"
    else        -> code
}

private fun mobilityLabel(code: String) = when (code) {
    "NORMAL"   -> "Normal"
    "REDUCED"  -> "Reduzida"
    "IMMOBILE" -> "Imóvel"
    else       -> code
}

private fun painLabel(code: String) = when (code) {
    "NONE"     -> "Sem sinais"
    "APPARENT" -> "Aparente"
    "EVIDENT"  -> "Evidente"
    else       -> code
}
