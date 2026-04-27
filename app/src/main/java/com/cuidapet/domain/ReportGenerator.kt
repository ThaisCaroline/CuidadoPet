package com.cuidadopet.domain

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.cuidadopet.data.db.entity.HealthEntryEntity
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
import com.cuidadopet.data.db.entity.MedicationEntity
import com.cuidadopet.data.db.entity.PetEntity
import com.cuidadopet.data.db.entity.WaterLogEntity
import com.cuidadopet.data.db.entity.WeightRecordEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Reúne todos os dados necessários para gerar o relatório do pet.
// É preenchida pelo ReportRepository com consultas a todos os outros repositórios.
data class PetReport(
    val pet: PetEntity,
    val periodStart: Long,
    val periodEnd: Long,
    val activeMedications: List<MedicationEntity>,
    val mealPlan: MealPlanEntity?,
    val meals: List<MealEntity>,
    val mealLogs: List<MealLogEntity>,
    val waterLogs: List<WaterLogEntity>,
    val healthEntries: List<HealthEntryEntity>,
    val weightRecords: List<WeightRecordEntity>
)

// Gera relatórios em texto (WhatsApp/e-mail) e PDF (impressão/arquivo).
// object = singleton — não precisa de instância, chama ReportGenerator.generateText(...)
object ReportGenerator {

    private val dateFmt  = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))

    // ─── Texto para WhatsApp / e-mail ─────────────────────────────────────────

    // Gera um relatório formatado em texto com marcação do WhatsApp (*negrito*, _itálico_).
    fun generateText(report: PetReport): String {
        val sb  = StringBuilder()
        val pet = report.pet

        val (actualStart, actualEnd) = actualDataPeriod(report)
        val periodLabel = if (dateFmt.format(Date(actualStart)) == dateFmt.format(Date(actualEnd)))
            dateFmt.format(Date(actualStart))
        else "${dateFmt.format(Date(actualStart))} a ${dateFmt.format(Date(actualEnd))}"

        sb.appendLine("🐾 *CuidadoPet — Relatório de ${pet.name}*")
        sb.appendLine("Período: $periodLabel")
        sb.appendLine()

        // Informações do pet
        sb.appendLine("*🐾 Informações do pet*")
        val speciesLabel  = if (pet.species == "DOG") "Cão" else "Gato"
        val sexLabel      = if (pet.sex == "MALE") "Macho" else "Fêmea"
        val neuteredLabel = if (pet.isNeutered) " (castrado)" else ""
        sb.appendLine("$speciesLabel${if (!pet.breed.isNullOrBlank()) " · ${pet.breed}" else ""}")
        sb.appendLine("$sexLabel$neuteredLabel · ${pet.weightKg} kg")
        sb.appendLine()

        // Medicamentos
        sb.appendLine("*💊 Medicamentos em uso*")
        if (report.activeMedications.isEmpty()) {
            sb.appendLine("Nenhum medicamento ativo.")
        } else {
            report.activeMedications.forEach { med ->
                sb.appendLine("• ${med.name} — ${med.dose} ${med.doseUnit}")
                val freq = if (med.frequencyType == "INTERVAL")
                    "  A cada ${med.frequencyHours}h"
                else
                    "  Horários: ${med.fixedTimes?.cleanJson() ?: ""}"
                sb.appendLine(freq)
                if (!med.observations.isNullOrBlank()) sb.appendLine("  ${med.observations}")
            }
        }
        sb.appendLine()

        // Alimentação
        sb.appendLine("*🍽️ Plano alimentar*")
        if (report.mealPlan == null) {
            sb.appendLine("Sem plano configurado.")
        } else {
            val foodLabel = foodTypeLabel(report.mealPlan.foodType)
            sb.appendLine("$foodLabel${if (report.mealPlan.dailyQuantityGrams != null) " · ${report.mealPlan.dailyQuantityGrams!!.toInt()}g/dia" else ""}")
            if (!report.mealPlan.restrictions.isNullOrBlank()) sb.appendLine("Restrições: ${report.mealPlan.restrictions}")
        }
        if (report.mealLogs.isEmpty()) {
            sb.appendLine("Nenhuma refeição registrada no período.")
        } else {
            val mealsById = report.meals.associateBy { it.id }
            sb.appendLine("Refeições administradas:")
            report.mealLogs
                .sortedWith(compareBy({ it.date }, { mealsById[it.mealId]?.timeOfDay ?: "" }))
                .forEach { log ->
                    val time   = mealsById[log.mealId]?.timeOfDay ?: "?"
                    val status = appetiteLabel(log.appetiteStatus, log.eatenPercentage)
                    val notes  = if (!log.notes.isNullOrBlank()) " · ${log.notes}" else ""
                    sb.appendLine("  ${dateFmt.format(Date(log.date))} $time — $status$notes")
                }
        }
        sb.appendLine()

        // Hidratação
        sb.appendLine("*💧 Hidratação no período*")
        if (report.waterLogs.isEmpty()) {
            sb.appendLine("Nenhum registro de água.")
        } else {
            val byDay = report.waterLogs
                .sortedBy { it.registeredAt }
                .groupBy { dateFmt.format(Date(it.registeredAt)) }
            byDay.forEach { (date, logs) ->
                sb.appendLine("  $date — ${logs.sumOf { it.amountMl }.toInt()} ml")
            }
            val totalMl = report.waterLogs.sumOf { it.amountMl }
            sb.appendLine("  Total: ${totalMl.toInt()} ml")
        }
        sb.appendLine()

        // Diário de saúde
        sb.appendLine("*❤️ Diário de saúde (${report.healthEntries.size} registros)*")
        if (report.healthEntries.isEmpty()) {
            sb.appendLine("Nenhum registro no período.")
        } else {
            // Exibe no máximo os 5 registros mais recentes para não sobrecarregar o texto
            report.healthEntries.takeLast(5).forEach { entry ->
                sb.appendLine("• ${dateFmt.format(Date(entry.registeredAt))}")
                entry.behavior?.let    { sb.appendLine("  Comportamento: ${behaviorLabel(it)}") }
                entry.fecesStatus?.let { sb.appendLine("  Fezes: ${fecesLabel(it)}") }
                entry.urineStatus?.let { sb.appendLine("  Urina: ${urineLabel(it)}") }
                entry.vomitCount?.let  { if (it > 0) sb.appendLine("  Vômitos: ${it}x") }
                entry.painSigns?.let   { if (it != "NONE") sb.appendLine("  Dor: ${painLabel(it)}") }
                if (!entry.observations.isNullOrBlank()) sb.appendLine("  ${entry.observations}")
            }
            if (report.healthEntries.size > 5)
                sb.appendLine("  ... e mais ${report.healthEntries.size - 5} registros anteriores.")
        }
        sb.appendLine()

        // Peso
        if (report.weightRecords.isNotEmpty()) {
            sb.appendLine("*⚖️ Últimas pesagens*")
            report.weightRecords.takeLast(5).forEach { w ->
                sb.appendLine("• ${dateFmt.format(Date(w.date))}: ${w.weightKg} kg${if (!w.notes.isNullOrBlank()) " (${w.notes})" else ""}")
            }
            sb.appendLine()
        }

        sb.appendLine("_Gerado pelo app CuidadoPet_")
        sb.appendLine("_⚠️ Este relatório é um registro de observações do tutor._")
        sb.appendLine("_Não substitui avaliação, diagnóstico ou prescrição veterinária._")

        return sb.toString()
    }

    // ─── PDF ─────────────────────────────────────────────────────────────────

    // Gera um PDF A4 e salva no cache do app, retornando o File.
    // O chamador usará FileProvider para transformar esse File em Uri compartilhável.
    fun generatePdf(context: Context, report: PetReport): File {
        // PdfDocument é a classe nativa do Android para criar PDFs
        val doc = PdfDocument()

        // Dimensões A4 em "pontos" (1 ponto = 1/72 de polegada, padrão PDF)
        val pageW = 595
        val pageH = 842
        val ml    = 40f   // margem esquerda
        val mr    = 40f   // margem direita
        val usable = pageW - ml - mr  // largura útil

        // Objetos Paint definem a aparência de cada tipo de texto
        val paintTitle = Paint().apply {
            textSize  = 18f
            typeface  = Typeface.DEFAULT_BOLD
            color     = Color.parseColor("#00695C")  // Teal do app
        }
        val paintHead = Paint().apply {
            textSize  = 13f
            typeface  = Typeface.DEFAULT_BOLD
            color     = Color.parseColor("#004D40")
        }
        val paintBody = Paint().apply {
            textSize = 11f
            color    = Color.BLACK
        }
        val paintSmall = Paint().apply {
            textSize = 9f
            color    = Color.DKGRAY
        }
        val paintLine = Paint().apply {
            strokeWidth = 0.5f
            color       = Color.LTGRAY
            style       = Paint.Style.STROKE
        }

        // Estado mutável do canvas — avançamos y a cada linha desenhada
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
        var page    = doc.startPage(pageInfo)
        var canvas  = page.canvas
        var y       = ml

        // Encerra a página atual e começa uma nova quando o y chegar perto do final
        fun breakPageIfNeeded(need: Float) {
            if (y + need > pageH - ml) {
                doc.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create()
                page     = doc.startPage(pageInfo)
                canvas   = page.canvas
                y        = ml
            }
        }

        // Desenha uma linha de texto na posição y atual e avança y
        fun text(txt: String, paint: Paint, indent: Float = 0f) {
            val lh = paint.textSize * 1.45f
            breakPageIfNeeded(lh)
            canvas.drawText(txt, ml + indent, y + paint.textSize, paint)
            y += lh
        }

        // Desenha separador horizontal + título de seção
        fun section(title: String) {
            breakPageIfNeeded(28f)
            y += 8f
            canvas.drawLine(ml, y, pageW - mr, y, paintLine)
            y += 5f
            text(title, paintHead)
        }

        // ── Cabeçalho ────────────────────────────────────────────────────────
        val (pdfStart, pdfEnd) = actualDataPeriod(report)
        val pdfPeriod = if (dateFmt.format(Date(pdfStart)) == dateFmt.format(Date(pdfEnd)))
            dateFmt.format(Date(pdfStart))
        else "${dateFmt.format(Date(pdfStart))} a ${dateFmt.format(Date(pdfEnd))}"

        text("CuidadoPet — Relatório de ${report.pet.name}", paintTitle)
        text("Período: $pdfPeriod", paintSmall)
        y += 4f

        // ── Pet ──────────────────────────────────────────────────────────────
        section("Informações do pet")
        val pet          = report.pet
        val speciesLabel = if (pet.species == "DOG") "Cão" else "Gato"
        val sexLabel     = if (pet.sex == "MALE") "Macho" else "Fêmea"
        text("Nome: ${pet.name}   Espécie: $speciesLabel   Peso: ${pet.weightKg} kg", paintBody)
        val breedSex = buildString {
            if (!pet.breed.isNullOrBlank()) append("Raça: ${pet.breed}   ")
            append("Sexo: $sexLabel")
            if (pet.isNeutered) append(" (castrado)")
        }
        text(breedSex, paintBody)

        // ── Medicamentos ─────────────────────────────────────────────────────
        section("Medicamentos em uso")
        if (report.activeMedications.isEmpty()) {
            text("Nenhum medicamento ativo.", paintBody)
        } else {
            report.activeMedications.forEach { med ->
                text("• ${med.name}   ${med.dose} ${med.doseUnit}", paintBody)
                val freq = if (med.frequencyType == "INTERVAL")
                    "A cada ${med.frequencyHours}h"
                else
                    "Horários: ${med.fixedTimes?.cleanJson() ?: ""}"
                text(freq, paintSmall, 12f)
                if (!med.observations.isNullOrBlank()) text("Obs: ${med.observations}", paintSmall, 12f)
            }
        }

        // ── Alimentação ───────────────────────────────────────────────────────
        section("Plano alimentar")
        if (report.mealPlan == null) {
            text("Sem plano configurado.", paintBody)
        } else {
            val plan = report.mealPlan
            text("Tipo: ${foodTypeLabel(plan.foodType)}${if (plan.dailyQuantityGrams != null) "   ${plan.dailyQuantityGrams!!.toInt()}g/dia" else ""}", paintBody)
            if (!plan.restrictions.isNullOrBlank()) text("Restrições: ${plan.restrictions}", paintSmall)
        }
        if (report.mealLogs.isEmpty()) {
            text("Nenhuma refeição registrada no período.", paintSmall)
        } else {
            text("Refeições administradas:", paintBody)
            val mealsById = report.meals.associateBy { it.id }
            report.mealLogs
                .sortedWith(compareBy({ it.date }, { mealsById[it.mealId]?.timeOfDay ?: "" }))
                .forEach { log ->
                    val time   = mealsById[log.mealId]?.timeOfDay ?: "?"
                    val status = appetiteLabel(log.appetiteStatus, log.eatenPercentage)
                    val notes  = if (!log.notes.isNullOrBlank()) " · ${log.notes}" else ""
                    text("${dateFmt.format(Date(log.date))} $time — $status$notes", paintSmall, 12f)
                }
        }

        // ── Hidratação ────────────────────────────────────────────────────────
        section("Hidratação no período")
        if (report.waterLogs.isEmpty()) {
            text("Nenhum registro de água.", paintBody)
        } else {
            val byDay = report.waterLogs
                .sortedBy { it.registeredAt }
                .groupBy { dateFmt.format(Date(it.registeredAt)) }
            byDay.forEach { (date, logs) ->
                text("$date — ${logs.sumOf { it.amountMl }.toInt()} ml", paintSmall, 12f)
            }
            val totalMl = report.waterLogs.sumOf { it.amountMl }
            text("Total: ${totalMl.toInt()} ml", paintBody)
        }

        // ── Diário de saúde ───────────────────────────────────────────────────
        section("Diário de saúde (${report.healthEntries.size} registros)")
        if (report.healthEntries.isEmpty()) {
            text("Nenhum registro no período.", paintBody)
        } else {
            report.healthEntries.forEach { entry ->
                text(dateFmt.format(Date(entry.registeredAt)), paintBody)
                entry.behavior?.let    { text("Comportamento: ${behaviorLabel(it)}", paintSmall, 12f) }
                entry.fecesStatus?.let { text("Fezes: ${fecesLabel(it)}", paintSmall, 12f) }
                entry.urineStatus?.let { text("Urina: ${urineLabel(it)}", paintSmall, 12f) }
                entry.vomitCount?.let  { if (it > 0) text("Vômitos: ${it}x", paintSmall, 12f) }
                entry.painSigns?.let   { if (it != "NONE") text("Sinais de dor: ${painLabel(it)}", paintSmall, 12f) }
                if (!entry.observations.isNullOrBlank()) text("Obs: ${entry.observations}", paintSmall, 12f)
                y += 3f
            }
        }

        // ── Histórico de peso ─────────────────────────────────────────────────
        if (report.weightRecords.isNotEmpty()) {
            section("Histórico de peso")
            report.weightRecords.forEach { w ->
                val note = if (!w.notes.isNullOrBlank()) "  (${w.notes})" else ""
                text("• ${dateFmt.format(Date(w.date))}: ${w.weightKg} kg$note", paintBody)
            }
        }

        // ── Rodapé ────────────────────────────────────────────────────────────
        breakPageIfNeeded(50f)
        y += 14f
        canvas.drawLine(ml, y, pageW - mr, y, paintLine)
        y += 6f
        text("Gerado pelo app CuidadoPet", paintSmall)
        text("Este relatório é um registro de observações do tutor.", paintSmall)
        text("Não substitui avaliação, diagnóstico ou prescrição veterinária.", paintSmall)

        doc.finishPage(page)

        // Salva o PDF na pasta /cache/relatorios/ — exposta pelo FileProvider
        val dir  = File(context.cacheDir, "relatorios").also { it.mkdirs() }
        val safe = report.pet.name.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]"), "_")
        val file = File(dir, "relatorio_$safe.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        return file
    }

    // ─── Helpers de tradução ──────────────────────────────────────────────────

    private fun appetiteLabel(status: String, pct: Int): String = when (status) {
        "ALL"     -> "Comeu tudo"
        "PARTIAL" -> "Parcial ($pct%)"
        "REFUSED" -> "Recusou"
        else      -> "$pct%"
    }

    // Intervalo real dos dados — evita exibir "01/01 a 26/04" quando só há dados de hoje
    private fun actualDataPeriod(report: PetReport): Pair<Long, Long> {
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

    // Remove colchetes e aspas do JSON de fixedTimes para exibição legível
    // Ex: ["08:00","20:00"] → 08:00, 20:00
    private fun String.cleanJson() =
        removePrefix("[").removeSuffix("]").replace("\"", "").replace(",", ", ")

    private fun foodTypeLabel(code: String) = when (code) {
        "DRY_KIBBLE"  -> "Ração seca"
        "WET_FOOD"    -> "Ração úmida"
        "NATURAL"     -> "Alimentação natural"
        "THERAPEUTIC" -> "Dieta terapêutica"
        else          -> "Outro"
    }

    private fun behaviorLabel(code: String) = when (code) {
        "NORMAL"    -> "Normal"
        "LETHARGIC" -> "Apático"
        "AGITATED"  -> "Agitado"
        "SLEEPY"    -> "Sonolento"
        else        -> code
    }

    private fun fecesLabel(code: String) = when (code) {
        "NORMAL"   -> "Normal"
        "SOFT"     -> "Amolecidas"
        "DIARRHEA" -> "Diarreia"
        "ABSENT"   -> "Ausentes"
        "BLOOD"    -> "Com sangue ⚠️"
        else       -> code
    }

    private fun urineLabel(code: String) = when (code) {
        "NORMAL"    -> "Normal"
        "INCREASED" -> "Aumentada"
        "REDUCED"   -> "Reduzida"
        "ABSENT"    -> "Ausente"
        "BLOOD"     -> "Com sangue ⚠️"
        else        -> code
    }

    private fun painLabel(code: String) = when (code) {
        "NONE"     -> "Sem sinais"
        "APPARENT" -> "Aparente"
        "EVIDENT"  -> "Evidente"
        else       -> code
    }
}
