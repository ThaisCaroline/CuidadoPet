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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.cuidadopet.R
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

    LaunchedEffect(state.pdfFile) {
        val file = state.pdfFile ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type    = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.report_share_pdf_chooser)))
        viewModel.clearPdfFile()
    }

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
                title = { Text(stringResource(R.string.report_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.report_share_chooser)))
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
                        stringResource(R.string.report_loading_error),
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

        val periods = listOf(
            7  to stringResource(R.string.report_period_7d),
            14 to stringResource(R.string.report_period_14d),
            30 to stringResource(R.string.report_period_30d)
        )
        val currentDays = ((report.periodEnd - report.periodStart) / 86_400_000).toInt()

        Text(stringResource(R.string.report_period_label), style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            periods.forEach { (days, label) ->
                FilterChip(
                    selected = currentDays in (days - 1)..(days + 1),
                    onClick  = { onChangePeriod(days) },
                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

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
                    stringResource(R.string.report_header, report.pet.name),
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

        SummaryCard(title = stringResource(R.string.report_section_medications)) {
            if (report.activeMedications.isEmpty()) {
                Text(stringResource(R.string.report_no_medications), style = MaterialTheme.typography.bodySmall)
            } else {
                val timeFmtLocal = remember { SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")) }
                val dateFmtLocal = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")) }
                report.activeMedications.forEach { med ->
                    Text(
                        "• ${med.name} — ${med.dose} ${med.doseUnit}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    val freqText = if (med.frequencyType == "INTERVAL")
                        "A cada ${med.frequencyHours}h"
                    else
                        "Horários: ${med.fixedTimes?.removeSurrounding("[", "]")?.replace("\"", "")?.replace(",", ", ") ?: ""}"
                    val periodLabel = if (med.isContinuous)
                        "desde ${dateFmtLocal.format(Date(med.startDate))}"
                    else if (med.endDate != null)
                        "${dateFmtLocal.format(Date(med.startDate))} a ${dateFmtLocal.format(Date(med.endDate))}"
                    else
                        "início: ${dateFmtLocal.format(Date(med.startDate))}"
                    Text(
                        "  $freqText · $periodLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val logsThisMed = report.medicationLogs
                        .filter { it.medicationId == med.id }
                        .sortedBy { it.scheduledAt }
                    if (logsThisMed.isNotEmpty()) {
                        logsThisMed.forEach { log ->
                            val statusLabel = when (log.status) {
                                "TAKEN"     -> stringResource(R.string.dose_status_taken_icon)
                                "NOT_TAKEN" -> stringResource(R.string.dose_status_not_taken_icon)
                                "VOMITED"   -> stringResource(R.string.dose_status_vomited_icon)
                                else        -> stringResource(R.string.dose_status_pending)
                            }
                            Text(
                                "  ${timeFmtLocal.format(Date(log.scheduledAt))} — $statusLabel  ✏",
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { editingMedLog = log }
                            )
                        }
                    }
                }
            }
        }

        SummaryCard(title = stringResource(R.string.report_section_feeding)) {
            if (report.mealPlans.isEmpty()) {
                Text(stringResource(R.string.report_no_plan), style = MaterialTheme.typography.bodySmall)
            } else {
                report.mealPlans.forEach { plan ->
                    val foodLabel = when (plan.foodType) {
                        "DRY_KIBBLE"  -> stringResource(R.string.food_type_dry_kibble)
                        "WET_FOOD"    -> stringResource(R.string.food_type_wet_food)
                        "NATURAL"     -> stringResource(R.string.food_type_natural)
                        "THERAPEUTIC" -> stringResource(R.string.food_type_therapeutic)
                        else          -> stringResource(R.string.food_type_other)
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
                        Text(stringResource(R.string.report_restrictions, plan.restrictions!!), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        SummaryCard(title = stringResource(R.string.report_section_feeding_period)) {
            if (report.mealLogs.isEmpty() && report.sporadicLogs.isEmpty()) {
                Text(stringResource(R.string.report_no_meals), style = MaterialTheme.typography.bodySmall)
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
                                    "DRY_KIBBLE"  -> stringResource(R.string.food_type_dry_kibble)
                                    "WET_FOOD"    -> stringResource(R.string.food_type_wet_food)
                                    "NATURAL"     -> stringResource(R.string.food_type_natural)
                                    "THERAPEUTIC" -> stringResource(R.string.food_type_therapeutic)
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
                        val sporadicGroups = daySporadicLogs
                            .groupBy { (it.description?.trim() ?: "") to it.amountUnit }
                            .entries.toList()
                        sporadicGroups.forEachIndexed { index, (key, logs) ->
                            val (desc, unit) = key
                            val total  = logs.sumOf { it.amountGrams ?: 0.0 }
                            val amount = if (total > 0) "${total.toInt()} $unit" else ""
                            val line   = when {
                                amount.isNotBlank() && desc.isNotBlank() -> "$amount $desc"
                                amount.isNotBlank() -> amount
                                desc.isNotBlank()   -> desc
                                else                -> return@forEachIndexed
                            }
                            if (index > 0) Spacer(Modifier.height(4.dp))
                            Text(
                                "  $line",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        SummaryCard(title = stringResource(R.string.report_section_water)) {
            if (report.waterLogs.isEmpty()) {
                Text(stringResource(R.string.report_no_water), style = MaterialTheme.typography.bodySmall)
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

        SummaryCard(title = stringResource(R.string.report_section_health)) {
            if (report.healthEntries.isEmpty()) {
                Text(stringResource(R.string.report_no_water), style = MaterialTheme.typography.bodySmall)
            } else {
                val withNotes = report.healthEntries
                    .filter { !it.observations.isNullOrBlank() }
                    .sortedByDescending { it.registeredAt }
                if (withNotes.isEmpty()) {
                    Text(
                        stringResource(R.string.report_health_entries_no_notes, report.healthEntries.size),
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

        SummaryCard(title = stringResource(R.string.report_section_weight)) {
            if (report.weightRecords.isEmpty()) {
                Text(stringResource(R.string.report_no_weight), style = MaterialTheme.typography.bodySmall)
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

        if (report.lastVaccineDoses.isNotEmpty()) {
            SummaryCard(title = stringResource(R.string.report_section_vaccines)) {
                val vaccines  = report.lastVaccineDoses.filter { it.type == "VACCINE" }
                val dewormers = report.lastVaccineDoses.filter { it.type == "DEWORMER" }
                if (vaccines.isNotEmpty()) {
                    Text(
                        stringResource(R.string.report_vaccines_label),
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
                        stringResource(R.string.report_dewormers_label),
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

        if (report.photos.isNotEmpty()) {
            SummaryCard(title = stringResource(R.string.report_section_photos)) {
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(
                stringResource(R.string.report_disclaimer),
                modifier = Modifier.padding(12.dp),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        Text(stringResource(R.string.report_share_chooser), style = MaterialTheme.typography.labelMedium)

        Button(
            onClick  = onShareText,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Share, contentDescription = null,
                modifier = Modifier.size(18.dp).padding(end = 4.dp))
            Text(stringResource(R.string.report_share_text_btn))
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
                Text(stringResource(R.string.report_generating_pdf))
            } else {
                Text(stringResource(R.string.report_generate_pdf_btn))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

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
        title = { Text(stringResource(R.string.report_edit_med_log_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "TAKEN"     to R.string.dose_status_taken,
                    "NOT_TAKEN" to R.string.dose_status_not_taken,
                    "VOMITED"   to R.string.dose_status_vomited
                ).forEach { (value, labelRes) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { selectedStatus = value }
                    ) {
                        RadioButton(selected = selectedStatus == value, onClick = { selectedStatus = value })
                        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                TextButton(
                    onClick = onDelete,
                    colors  = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.report_delete_log_btn)) }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(log.copy(status = selectedStatus)) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
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
        title = { Text(stringResource(R.string.report_edit_food_total_title, state.day)) },
        text = {
            OutlinedTextField(
                value           = valueText,
                onValueChange   = { if (it.all { c -> c.isDigit() }) valueText = it },
                label           = { Text(stringResource(R.string.report_food_total_label, state.unit)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { valueText.toDoubleOrNull()?.let { onConfirm(it) } }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
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
        title = { Text(stringResource(R.string.report_edit_water_total_title, state.day)) },
        text = {
            OutlinedTextField(
                value           = valueText,
                onValueChange   = { if (it.all { c -> c.isDigit() }) valueText = it },
                label           = { Text(stringResource(R.string.report_water_total_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine      = true,
                modifier        = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { valueText.toDoubleOrNull()?.let { onConfirm(it) } }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
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
        title = { Text(stringResource(R.string.report_edit_weight_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value           = weightText,
                    onValueChange   = { weightText = it },
                    label           = { Text(stringResource(R.string.report_weight_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text(stringResource(R.string.report_weight_notes_label)) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                HorizontalDivider()
                TextButton(
                    onClick  = onDelete,
                    colors   = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.report_delete_log_btn)) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kg = weightText.replace(",", ".").toDoubleOrNull() ?: return@Button
                    onConfirm(record.copy(weightKg = kg, notes = notes.ifBlank { null }))
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

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
