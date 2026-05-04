package com.cuidadopet.ui.screens.health

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidadopet.data.db.entity.WeightRecordEntity
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.ShapeComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat      = SimpleDateFormat("dd/MM/yy",  Locale.forLanguageTag("pt-BR"))
private val shortDateFormat = SimpleDateFormat("dd/MM",     Locale.forLanguageTag("pt-BR"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightHistoryScreen(
    petId: Long,
    onNavigateBack: () -> Unit,
    viewModel: WeightHistoryViewModel = hiltViewModel()
) {
    LaunchedEffect(petId) { viewModel.load(petId) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val chartModelProducer = remember { ChartEntryModelProducer() }

    LaunchedEffect(state.records) {
        if (state.records.isNotEmpty()) {
            chartModelProducer.setEntries(
                state.records.mapIndexed { index, record ->
                    entryOf(index.toFloat(), record.weightKg.toFloat())
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de peso") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Registrar peso",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
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

            if (state.records.isEmpty()) {
                Text(
                    "Nenhum peso registrado ainda.\nToque no + para adicionar o primeiro.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text("Evolução do peso", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)

                WeightChart(records = state.records, chartModelProducer = chartModelProducer)

                HorizontalDivider()

                Text("Registros", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)

                state.records.reversed().forEachIndexed { revIdx, record ->
                    val chronoIdx   = state.records.size - 1 - revIdx
                    val prevRecord  = if (chronoIdx > 0) state.records[chronoIdx - 1] else null
                    WeightRecordRow(
                        record         = record,
                        previousRecord = prevRecord,
                        canDelete      = state.records.size > 1,
                        onDelete       = { viewModel.deleteRecord(record.id) }
                    )
                }

                if (state.records.size == 1) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "O histórico precisa ter ao menos um registro.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAddDialog) {
        AddWeightDialog(
            onConfirm = { kg, notes ->
                viewModel.addWeight(petId, kg, notes)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

// ── Gráfico ───────────────────────────────────────────────────────────────────

@Composable
private fun WeightChart(
    records: List<WeightRecordEntity>,
    chartModelProducer: ChartEntryModelProducer
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryArgb  = primaryColor.toArgb()

    val lineSpec = remember(primaryArgb) {
        LineChart.LineSpec(
            lineColor           = primaryArgb,
            lineThicknessDp     = 2.5f,
            lineBackgroundShader = DynamicShaders.fromBrush(
                Brush.verticalGradient(
                    listOf(primaryColor.copy(alpha = 0.28f), Color.Transparent)
                )
            ),
            point       = ShapeComponent(shape = Shapes.pillShape, color = primaryArgb),
            pointSizeDp = 8f
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        ProvideChartStyle(m3ChartStyle()) {
            Chart(
                chart              = lineChart(lines = listOf(lineSpec)),
                chartModelProducer = chartModelProducer,
                startAxis          = rememberStartAxis(),
                bottomAxis         = rememberBottomAxis(
                    valueFormatter = { value, _ ->
                        val idx = value.toInt()
                        if (idx in records.indices) shortDateFormat.format(Date(records[idx].date)) else ""
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            )
        }
    }
}

// ── Card de registro ──────────────────────────────────────────────────────────

@Composable
private fun WeightRecordRow(
    record: WeightRecordEntity,
    previousRecord: WeightRecordEntity?,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    val delta = if (previousRecord != null) record.weightKg - previousRecord.weightKg else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${record.weightKg} kg",
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    dateFormat.format(Date(record.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!record.notes.isNullOrBlank()) {
                    Text(
                        record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (delta != null) {
                val (label, color) = when {
                    delta > 0.05  -> "▲ +${"%.2f".format(delta)} kg"  to MaterialTheme.colorScheme.primary
                    delta < -0.05 -> "▼ ${"%.2f".format(-delta)} kg" to MaterialTheme.colorScheme.error
                    else          -> "— igual"                         to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    label,
                    style      = MaterialTheme.typography.labelSmall,
                    color      = color,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(end = 4.dp)
                )
            }

            IconButton(onClick = onDelete, enabled = canDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remover",
                    tint = if (canDelete) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ── Diálogo de novo peso ──────────────────────────────────────────────────────

@Composable
private fun AddWeightDialog(
    onConfirm: (Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var weightText by remember { mutableStateOf("") }
    var notes      by remember { mutableStateOf("") }
    var error      by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar peso") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = weightText,
                    onValueChange = { raw ->
                        val sb     = StringBuilder()
                        var hasSep = false
                        for (c in raw) {
                            when {
                                c.isDigit()                          -> sb.append(c)
                                (c == '.' || c == ',') && !hasSep   -> { sb.append('.'); hasSep = true }
                            }
                        }
                        weightText = sb.toString()
                        error = false
                    },
                    label           = { Text("Peso") },
                    placeholder     = { Text("Ex: 4,3") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine      = true,
                    suffix          = { Text("kg") },
                    isError         = error,
                    supportingText  = { if (error) Text("Informe um peso válido (ex: 4,3).") }
                )
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Observação (opcional)") },
                    placeholder   = { Text("Ex: pesado na clínica") },
                    singleLine    = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val kg = weightText.toDoubleOrNull()
                if (kg == null || kg < 0.1 || kg > 200) { error = true; return@Button }
                onConfirm(kg, notes)
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
