package com.cuidadopet.ui.screens.report

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidadopet.domain.PetReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.cuidadopet.ui.utils.adaptiveHorizontalPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    petId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    // Carrega os dados assim que a tela abre com o período padrão (7 dias)
    LaunchedEffect(petId) { viewModel.load(petId) }

    val state   by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }

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
                    report         = state.report!!,
                    isPdfGenerating = state.isPdfGenerating,
                    onShareText     = {
                        val text   = viewModel.getShareText() ?: return@ReportContent
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartilhar relatório"))
                    },
                    onGeneratePdf = { viewModel.generatePdf() },
                    onChangePeriod = { days -> viewModel.load(petId, days) },
                    modifier      = Modifier.padding(innerPadding)
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

@Composable
private fun ReportContent(
    report: PetReport,
    isPdfGenerating: Boolean,
    onShareText: () -> Unit,
    onGeneratePdf: () -> Unit,
    onChangePeriod: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")) }

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
                report.activeMedications.forEach { med ->
                    Text("• ${med.name} — ${med.dose} ${med.doseUnit}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        SummaryCard(title = "Alimentação") {
            if (report.mealPlan == null) {
                Text("Sem plano configurado.", style = MaterialTheme.typography.bodySmall)
            } else {
                val foodLabel = when (report.mealPlan.foodType) {
                    "DRY_KIBBLE"  -> "Ração seca"
                    "WET_FOOD"    -> "Ração úmida"
                    "NATURAL"     -> "Alimentação natural"
                    "THERAPEUTIC" -> "Dieta terapêutica"
                    else          -> "Outro"
                }
                Text(
                    "$foodLabel${if (report.mealPlan.dailyQuantityGrams != null) " · ${report.mealPlan.dailyQuantityGrams!!.toInt()}g/dia" else ""}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (!report.mealPlan.restrictions.isNullOrBlank()) {
                    Text("Restrições: ${report.mealPlan.restrictions}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        SummaryCard(title = "Refeições administradas") {
            if (report.mealLogs.isEmpty()) {
                Text("Nenhuma refeição registrada no período.", style = MaterialTheme.typography.bodySmall)
            } else {
                val mealsById = report.meals.associateBy { it.id }
                report.mealLogs
                    .sortedWith(compareBy({ it.date }, { mealsById[it.mealId]?.timeOfDay ?: "" }))
                    .forEach { log ->
                        val time   = mealsById[log.mealId]?.timeOfDay ?: "?"
                        val status = when (log.appetiteStatus) {
                            "ALL"     -> "Comeu tudo"
                            "PARTIAL" -> "Parcial (${log.eatenPercentage}%)"
                            "REFUSED" -> "Recusou"
                            else      -> "${log.eatenPercentage}%"
                        }
                        Text(
                            "• ${dateFmt.format(Date(log.date))}  $time — $status",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!log.notes.isNullOrBlank()) {
                            Text(
                                "  ${log.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
            }
        }

        SummaryCard(title = "Hidratação no período") {
            if (report.waterLogs.isEmpty()) {
                Text("Nenhum registro.", style = MaterialTheme.typography.bodySmall)
            } else {
                val byDay = report.waterLogs
                    .sortedBy { it.registeredAt }
                    .groupBy { dateFmt.format(Date(it.registeredAt)) }
                byDay.forEach { (date, logs) ->
                    val total = logs.sumOf { it.amountMl }
                    Text(
                        "$date — ${total.toInt()} ml",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                val grandTotal = report.waterLogs.sumOf { it.amountMl }
                Text(
                    "Total: ${grandTotal.toInt()} ml",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        SummaryCard(title = "Diário de saúde") {
            Text(
                "${report.healthEntries.size} registros no período",
                style = MaterialTheme.typography.bodySmall
            )
        }

        SummaryCard(title = "Histórico de peso") {
            if (report.weightRecords.isEmpty()) {
                Text("Nenhuma pesagem registrada.", style = MaterialTheme.typography.bodySmall)
            } else {
                report.weightRecords.sortedByDescending { it.date }.forEach { w ->
                    val note = if (!w.notes.isNullOrBlank()) " — ${w.notes}" else ""
                    Text(
                        "${dateFmt.format(Date(w.date))}: ${w.weightKg} kg$note",
                        style = MaterialTheme.typography.bodySmall
                    )
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
        report.mealLogs.forEach     { add(it.date) }
        report.waterLogs.forEach    { add(it.registeredAt) }
        report.healthEntries.forEach { add(it.registeredAt) }
        report.weightRecords.forEach { add(it.date) }
    }
    val now = System.currentTimeMillis()
    return if (timestamps.isEmpty()) now to now
    else timestamps.min() to timestamps.max()
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
