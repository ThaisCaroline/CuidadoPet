package com.cuidadopet.ui.screens.report

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.cuidadopet.data.db.entity.HealthPhotoEntity
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MedicationLogEntity
import com.cuidadopet.data.db.entity.SporadicMealLogEntity
import com.cuidadopet.data.db.entity.WaterLogEntity
import com.cuidadopet.data.db.entity.WeightRecordEntity
import java.io.File
import com.cuidadopet.domain.PetReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.cuidadopet.ui.ads.InterstitialAdManager
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding

private data class FoodDayEditState(
    val day: String,
    val totalGrams: Double,
    val unit: String,
    val mealLogs: List<MealLogEntity>,
    val sporadicLogs: List<SporadicMealLogEntity>
)

private data class WaterDayEditState(
    val day: String,
    val totalMl: Int,
    val logs: List<WaterLogEntity>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    petId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    LaunchedEffect(petId) { viewModel.load(petId) }

    val state      by viewModel.state.collectAsStateWithLifecycle()
    val isPremium  by viewModel.isPremium.collectAsStateWithLifecycle()
    val context    = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val interstitialAd = remember { InterstitialAdManager(context) }
    LaunchedEffect(petId) {
        if (!isPremium) {
            val activity = context as? android.app.Activity
            activity?.let { interstitialAd.show(it) }
        }
    }

    // Quando o PDF estiver pronto, abre o share dialog do Android automaticamente
    LaunchedEffect(state.pdfFile) {
        val file = state.pdfFile ?: return@LaunchedEffect

        // FileProvider.getUriForFile transforma o File em Uri segura para compartilhamento
        // A authority deve bater com o que está declarado no AndroidManifest
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        // Cria um Intent de compartilhamento para o PDF
        val intent = Intent(Intent.ACTION_SEND).apply {
            type    = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)  // permite que o app receptor leia o arquivo
        }
        // Abre o seletor de apps (WhatsApp, Drive, e-mail etc.)
        context.startActivity(Intent.createChooser(intent, "Compartilhar PDF"))

        // Limpa o pdfFile do estado para evitar reabrir o dialog ao girar a tela
        viewModel.clearPdfFile()
    }

    // Mostra o erro como Snackbar se houver
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Relatório") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor          = MaterialTheme.colorScheme.primary,
                    titleContentColor       = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->

        when {
            state.isLoading -> {
                Box(
                    modifier          = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment  = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            state.report != null -> {
                ReportContent(
                    report              = state.report!!,
                    isPdfGenerating     = state.isPdfGenerating,
                    onShareText         = {
                        val text   = viewModel.getShareText() ?: return@ReportContent
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
                    },
                    onGeneratePdf       = { viewModel.generatePdf() },
                    onChangePeriod      = { days -> viewModel.load(petId, days) },
                    onUpdateMedicationLog    = { viewModel.updateMedicationLog(it) },
                    onDeleteMedicationLog    = { viewModel.deleteMedicationLog(it) },
                    onUpdateFoodDailyTotal   = { ml, sl, t -> viewModel.updateFoodDailyTotal(ml, sl, t) },
                    onUpdateWaterDailyTotal  = { logs, t -> viewModel.updateWaterDailyTotal(logs, t) },
                    onUpdateWeightRecord     = { viewModel.updateWeightRecord(it) },
                    onDeleteWeightRecord     = { viewModel.deleteWeightRecord(it) },
                    modifier            = Modifier.padding(innerPadding)
                )
            }

            else -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Não foi possível carregar os dados.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportContent(
    report: PetReport,
    isPdfGenerating: Boolean,
    onShareText: () -> Unit,
    onGeneratePdf: () -> Unit,
    onChangePeriod: (Int) -> Unit,
    onUpdateMedicationLog: (MedicationLogEntity) -> Unit,
    onDeleteMedicationLog: (Long) -> Unit,
    onUpdateFoodDailyTotal: (List<MealLogEntity>, List<SporadicMealLogEntity>, Double) -> Unit,
    onUpdateWaterDailyTotal: (List<WaterLogEntity>, Double) -> Unit,
    onUpdateWeightRecord: (WeightRecordEntity) -> Unit,
    onDeleteWeightRecord: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFmt  = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")) }
    val timeFmt  = remember { SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")) }

    var editingMedLog       by remember { mutableStateOf<MedicationLogEntity?>(null) }
    var editingFoodDay      by remember { mutableStateOf<FoodDayEditState?>(null) }
    var editingWaterDay     by remember { mutableStateOf<WaterDayEditState?>(null) }
    var editingWeightRecord by remember { mutableStateOf<WeightRecordEntity?>(null) }

    editingMedLog?.let { log ->
        EditMedicationLogDialog(
            log       = log,
            onConfirm = { updated -> onUpdateMedicationLog(updated); editingMedLog = null },
            onDelete  = { onDeleteMedicationLog(log.id); editingMedLog = null },
            onDismiss = { editingMedLog = null }
        )
    }
    editingFoodDay?.let { state ->
        EditFoodDayDialog(
            state     = state,
            onConfirm = { newTotal -> onUpdateFoodDailyTotal(state.mealLogs, state.sporadicLogs, newTotal); editingFoodDay = null },
            onDismiss = { editingFoodDay = null }
        )
    }
    editingWaterDay?.let { state ->
        EditWaterDayDialog(
            state     = state,
            onConfirm = { newTotal -> onUpdateWaterDailyTotal(state.logs, newTotal); editingWaterDay = null },
            onDismiss = { editingWaterDay = null }
        )
    }
    editingWeightRecord?.let { record ->
        EditWeightDialog(
            record    = record,
            onConfirm = { updated -> onUpdateWeightRecord(updated); editingWeightRecord = null },
            onDelete  = { onDeleteWeightRecord(record.id); editingWeightRecord = null },
            onDismiss = { editingWeightRecord = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = adaptiveHorizontalPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Seletor de período ────────────────────────────────────────────────
        val periods = listOf(7 to "7 dias", 14 to "14 dias", 30 to "30 dias")
        val currentDays = ((report.periodEnd - report.periodStart) / 86_400_000).toInt()

        Text("Período do relatório", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            periods.forEach { (days, label) ->
                FilterChip(
                    selected = currentDays in (days - 1)..(days + 1),
                    onClick  = { onChangePeriod(days) },
                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // ── Cabeçalho do relatório ────────────────────────────────────────────
        val (actualStart, actualEnd) = remember(report) { actualDataDateRange(report) }
        val periodLabel = if (dateFmt.format(Date(actualStart)) == dateFmt.format(Date(actualEnd)))
            dateFmt.format(Date(actualStart))
        else
            "${dateFmt.format(Date(actualStart))} a ${dateFmt.format(Date(actualEnd))}"

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Relatório de ${report.pet.name}",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    periodLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Resumo dos dados ──────────────────────────────────────────────────
        SummaryCard(title = "Medicamentos em uso") {
            if (report.activeMedications.isEmpty()) {
                Text("Nenhum", style = MaterialTheme.typography.bodySmall)
            } else {
                val timeFmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")) }
                report.activeMedications.forEach { med ->
                    Text(
                        "• ${med.name} — ${med.dose} ${med.doseUnit}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    val logsThisMed = report.medicationLogs
                        .filter { it.medicationId == med.id }
                        .sortedBy { it.scheduledAt }
                    if (logsThisMed.isNotEmpty()) {
                        logsThisMed.forEach { log ->
                            val statusLabel = when (log.status) {
                                "TAKEN"     -> "✓ Administrado"
                                "NOT_TAKEN" -> "✗ Não administrado"
                                "VOMITED"   -> "⚠ Administrado (vomitou)"
                                else        -> "Pendente"
                            }
                            Text(
                                "  ${timeFmt.format(Date(log.scheduledAt))} — $statusLabel  ✏",
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { editingMedLog = log }
                            )
                        }
                    }
                }
            }
        }

        SummaryCard(title = "Alimentação") {
            if (report.mealPlans.isEmpty()) {
                Text("Sem plano configurado.", style = MaterialTheme.typography.bodySmall)
            } else {
                report.mealPlans.forEach { plan ->
                    val foodLabel = when (plan.foodType) {
                        "DRY_KIBBLE"  -> "Ração seca"
                        "WET_FOOD"    -> "Ração úmida"
                        "NATURAL"     -> "Alimentação natural"
                        "THERAPEUTIC" -> "Dieta terapêutica"
                        else          -> "Outro"
                    }
                    val unitForPlan = report.meals.firstOrNull { it.mealPlanId == plan.id }?.quantityUnit ?: "g"
                    Text(
                        "• $foodLabel${if (plan.dailyQuantityGrams != null) " · ${plan.dailyQuantityGrams!!.toInt()}$unitForPlan/dia" else ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!plan.foodDetails.isNullOrBlank()) {
                        Text("  ${plan.foodDetails}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!plan.restrictions.isNullOrBlank()) {
                        Text("  Restrições: ${plan.restrictions}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        SummaryCard(title = "Alimentação no período") {
            if (report.mealLogs.isEmpty() && report.sporadicLogs.isEmpty()) {
                Text("Nenhuma refeição registrada no período.", style = MaterialTheme.typography.bodySmall)
            } else {
                val mealsById     = report.meals.associateBy { it.id }
                val planById      = report.mealPlans.associateBy { it.id }
                val mealsByDay    = report.mealLogs.sortedBy { it.date }
                    .groupBy { dateFmt.format(Date(it.date)) }
                val sporadicByDay = report.sporadicLogs.sortedBy { it.registeredAt }
                    .groupBy { dateFmt.format(Date(it.registeredAt)) }
                val orderedDays   = (report.mealLogs.map { it.date } + report.sporadicLogs.map { it.registeredAt })
                    .sorted().map { dateFmt.format(Date(it)) }.distinct()

                orderedDays.forEach { day ->
                    val dayMealLogs     = mealsByDay[day] ?: emptyList()
                    val daySporadicLogs = sporadicByDay[day] ?: emptyList()
                    val byUnit = dayMealLogs.groupBy { log ->
                        mealsById[log.mealId]?.quantityUnit ?: "g"
                    }
                    val unitLines = byUnit.entries.map { (unit, logs) ->
                        val total = logs.sumOf { log ->
                            (mealsById[log.mealId]?.quantityGrams ?: 0.0) * log.eatenPercentage / 100.0
                        }
                        val types = logs.mapNotNull { log ->
                            mealsById[log.mealId]?.mealPlanId?.let { planId ->
                                when (planById[planId]?.foodType) {
                                    "DRY_KIBBLE"  -> "Ração seca"
                                    "WET_FOOD"    -> "Ração úmida"
                                    "NATURAL"     -> "Alimentação natural"
                                    "THERAPEUTIC" -> "Dieta terapêutica"
                                    else          -> null
                                }
                            }
                        }.distinct()
                        val typeLabel = if (types.isNotEmpty()) " · ${types.joinToString(", ")}" else ""
                        Triple(total, unit, "${total.toInt()} $unit$typeLabel")
                    }
                    val sporadicTotal = daySporadicLogs.sumOf { it.amountGrams ?: 0.0 }
                    val editTotal = (unitLines.firstOrNull()?.first ?: 0.0) + sporadicTotal
                    val editUnit  = unitLines.firstOrNull()?.second ?: "g"

                    Column(modifier = Modifier.clickable {
                        editingFoodDay = FoodDayEditState(day, editTotal, editUnit, dayMealLogs, daySporadicLogs)
                    }) {
                        Text(
                            "$day  ✏",
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        unitLines.forEach { (_, _, label) ->
                            Text(
                                "  $label",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (sporadicTotal > 0) {
                            Text(
                                "  ${sporadicTotal.toInt()} g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

            }
        }

        SummaryCard(title = "Hidratação no período") {
            if (report.waterLogs.isEmpty()) {
                Text("Nenhum registro.", style = MaterialTheme.typography.bodySmall)
            } else {
                report.waterLogs
                    .sortedBy { it.registeredAt }
                    .groupBy { dateFmt.format(Date(it.registeredAt)) }
                    .forEach { (day, logs) ->
                        val totalMl = logs.sumOf { it.amountMl }.toInt()
                        Text(
                            "$day: $totalMl ml  ✏",
                            style      = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.clickable {
                                editingWaterDay = WaterDayEditState(day, totalMl, logs)
                            }
                        )
                    }
            }
        }

        SummaryCard(title = "Diário de saúde") {
            if (report.healthEntries.isEmpty()) {
                Text("Nenhum registro no período.", style = MaterialTheme.typography.bodySmall)
            } else {
                val withNotes = report.healthEntries
                    .filter { !it.observations.isNullOrBlank() }
                    .sortedByDescending { it.registeredAt }
                if (withNotes.isEmpty()) {
                    Text(
                        "${report.healthEntries.size} registros — sem anotações de texto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    withNotes.forEach { entry ->
                        Text(
                            "${dateFmt.format(Date(entry.registeredAt))} — ${entry.observations}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        SummaryCard(title = "Histórico de peso") {
            if (report.weightRecords.isEmpty()) {
                Text("Nenhuma pesagem registrada.", style = MaterialTheme.typography.bodySmall)
            } else {
                report.weightRecords.sortedByDescending { it.date }.forEach { w ->
                    val note = if (!w.notes.isNullOrBlank()) " — ${w.notes}" else ""
                    Text(
                        "${dateFmt.format(Date(w.date))}: ${w.weightKg} kg$note  ✏",
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { editingWeightRecord = w }
                    )
                }
            }
        }

        // ── Vacinas e Vermífugos ──────────────────────────────────────────────
        if (report.lastVaccineDoses.isNotEmpty()) {
            SummaryCard(title = "Vacinas e Vermífugos — última dose") {
                val vaccines  = report.lastVaccineDoses.filter { it.type == "VACCINE" }
                val dewormers = report.lastVaccineDoses.filter { it.type == "DEWORMER" }
                if (vaccines.isNotEmpty()) {
                    Text(
                        "Vacinas:",
                        style    = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    vaccines.forEach { v ->
                        val date = v.administeredAt?.let { dateFmt.format(Date(it)) } ?: "—"
                        val next = v.nextDueDate?.let { "  •  Próxima: ${dateFmt.format(Date(it))}" } ?: ""
                        Text("${v.name} — $date$next", style = MaterialTheme.typography.bodySmall)
                        if (!v.notes.isNullOrBlank())
                            Text(v.notes, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (dewormers.isNotEmpty()) {
                    Text(
                        "Vermífugos:",
                        style    = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                    dewormers.forEach { v ->
                        val date = v.administeredAt?.let { dateFmt.format(Date(it)) } ?: "—"
                        val next = v.nextDueDate?.let { "  •  Próxima: ${dateFmt.format(Date(it))}" } ?: ""
                        Text("${v.name} — $date$next", style = MaterialTheme.typography.bodySmall)
                        if (!v.notes.isNullOrBlank())
                            Text(v.notes, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ── Fotos ────────────────────────────────────────────────────────────
        if (report.photos.isNotEmpty()) {
            SummaryCard(title = "Fotos registradas") {
                val photosByDay = report.photos.groupBy { dateFmt.format(Date(it.entryDate)) }
                photosByDay.forEach { (date, photos) ->
                    Text(date, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        photos.forEach { photo ->
                            AsyncImage(
                                model = File(photo.filePath),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        // ── Aviso veterinário ─────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(
                "⚠️ Este relatório é um registro de observações do tutor e não substitui " +
                    "avaliação, diagnóstico ou prescrição veterinária.",
                modifier = Modifier.padding(12.dp),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        // ── Botões de compartilhamento ────────────────────────────────────────
        Text("Compartilhar relatório", style = MaterialTheme.typography.labelMedium)

        Button(
            onClick  = onShareText,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Share, contentDescription = null,
                modifier = Modifier.size(18.dp).padding(end = 4.dp))
            Text("Compartilhar como texto (WhatsApp)")
        }

        OutlinedButton(
            onClick   = onGeneratePdf,
            enabled   = !isPdfGenerating,
            modifier  = Modifier.fillMaxWidth(),
            colors    = ButtonDefaults.outlinedButtonColors()
        ) {
            if (isPdfGenerating) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Text("  Gerando PDF...")
            } else {
                Text("Gerar e compartilhar PDF")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// Retorna o intervalo real dos dados do relatório (min/max das datas com registros).
// Se não houver dados, retorna hoje como ponto único.
private fun actualDataDateRange(report: PetReport): Pair<Long, Long> {
    val timestamps = buildList {
        report.mealLogs.forEach      { add(it.date) }
        report.sporadicLogs.forEach  { add(it.registeredAt) }
        report.waterLogs.forEach     { add(it.registeredAt) }
        report.healthEntries.forEach { add(it.registeredAt) }
        report.weightRecords.forEach { add(it.date) }
    }
    val now = System.currentTimeMillis()
    return if (timestamps.isEmpty()) now to now
    else timestamps.min() to timestamps.max()
}

@Composable
private fun EditMedicationLogDialog(
    log: MedicationLogEntity,
    onConfirm: (MedicationLogEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedStatus by remember { mutableStateOf(log.status ?: "TAKEN") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar administração") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "TAKEN"     to "Administrado",
                    "NOT_TAKEN" to "Não administrado",
                    "VOMITED"   to "Administrado (vomitou)"
                ).forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selectedStatus = value }
                    ) {
                        RadioButton(selected = selectedStatus == value, onClick = { selectedStatus = value })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                TextButton(
                    onClick = onDelete,
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Excluir registro") }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(log.copy(status = selectedStatus)) }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun EditFoodDayDialog(
    state: FoodDayEditState,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var valueText by remember { mutableStateOf(state.totalGrams.toInt().toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar total — ${state.day}") },
        text = {
            OutlinedTextField(
                value           = valueText,
                onValueChange   = { if (it.all { c -> c.isDigit() }) valueText = it },
                label           = { Text("Total do dia (${state.unit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { valueText.toDoubleOrNull()?.let { onConfirm(it) } }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun EditWaterDayDialog(
    state: WaterDayEditState,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var valueText by remember { mutableStateOf(state.totalMl.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar total — ${state.day}") },
        text = {
            OutlinedTextField(
                value           = valueText,
                onValueChange   = { if (it.all { c -> c.isDigit() }) valueText = it },
                label           = { Text("Total do dia (ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { valueText.toDoubleOrNull()?.let { onConfirm(it) } }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun EditWeightDialog(
    record: WeightRecordEntity,
    onConfirm: (WeightRecordEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var weightText by remember { mutableStateOf(record.weightKg.toString()) }
    var notes      by remember { mutableStateOf(record.notes ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar pesagem") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value           = weightText,
                    onValueChange   = { weightText = it },
                    label           = { Text("Peso (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Observações (opcional)") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
                TextButton(
                    onClick  = onDelete,
                    colors   = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Excluir registro") }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kg = weightText.replace(",", ".").toDoubleOrNull() ?: return@Button
                    onConfirm(record.copy(weightKg = kg, notes = notes.ifBlank { null }))
                }
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// Card genérico para cada seção do resumo
@Composable
private fun SummaryCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
