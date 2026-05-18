package com.cuidadopet.domain

import android.content.Context
import com.cuidadopet.data.db.entity.HealthPhotoEntity
import com.cuidadopet.data.db.entity.MedicationLogEntity
import com.cuidadopet.data.db.entity.VaccineEntity
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
import com.cuidadopet.data.db.entity.SporadicMealLogEntity
import com.cuidadopet.data.db.entity.WaterLogEntity
import com.cuidadopet.data.db.entity.WeightRecordEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Reúne todos os dados necessários para gerar o relatório do pet.
// É preenchida pelo ReportRepository com consultas a todos os outros repositórios.
data class PetReport(
    val pet: PetEntity,
    val periodStart: Long,
    val periodEnd: Long,
    val activeMedications: List<MedicationEntity>,
    val medicationLogs: List<MedicationLogEntity>,
    val mealPlans: List<MealPlanEntity>,
    val meals: List<MealEntity>,
    val mealLogs: List<MealLogEntity>,
    val sporadicLogs: List<SporadicMealLogEntity>,
    val waterLogs: List<WaterLogEntity>,
    val healthEntries: List<HealthEntryEntity>,
    val weightRecords: List<WeightRecordEntity>,
    val photos: List<HealthPhotoEntity>,
    val lastVaccineDoses: List<VaccineEntity> = emptyList()
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
        val speciesLabel  = speciesLabel(pet.species, pet.customSpecies)
        val sexLabel      = if (pet.sex == "MALE") "Macho" else "Fêmea"
        val neuteredLabel = if (pet.isNeutered) " (castrado)" else ""
        sb.appendLine("$speciesLabel${if (!pet.breed.isNullOrBlank()) " · ${pet.breed}" else ""}")
        pet.birthDate?.let { sb.appendLine("Idade: ${calculateAge(it)}") }
        sb.appendLine("$sexLabel$neuteredLabel · ${pet.weightKg} kg")
        sb.appendLine()

        // Medicamentos
        sb.appendLine("*💊 Medicamentos em uso*")
        if (report.activeMedications.isEmpty()) {
            sb.appendLine("Nenhum medicamento ativo.")
        } else {
            val timeFmt = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))
            report.activeMedications.forEach { med ->
                sb.appendLine("• ${med.name} — ${med.dose} ${med.doseUnit}")
                val freq = if (med.frequencyType == "INTERVAL")
                    "  A cada ${med.frequencyHours}h"
                else
                    "  Horários: ${med.fixedTimes?.cleanJson() ?: ""}"
                sb.appendLine(freq)
                if (!med.observations.isNullOrBlank()) sb.appendLine("  ${med.observations}")
                val logsThisMed = report.medicationLogs
                    .filter { it.medicationId == med.id }
                    .sortedBy { it.scheduledAt }
                if (logsThisMed.isNotEmpty()) {
                    sb.appendLine("  _Administrações no período:_")
                    logsThisMed.forEach { log ->
                        val statusLabel = when (log.status) {
                            "TAKEN"     -> "Administrado"
                            "NOT_TAKEN" -> "Não administrado"
                            "VOMITED"   -> "Administrado (vomitou)"
                            else        -> "Pendente"
                        }
                        sb.appendLine("    ${timeFmt.format(Date(log.scheduledAt))} — $statusLabel")
                    }
                }
            }
        }
        sb.appendLine()

        // Alimentação
        sb.appendLine("*🍽️ Plano alimentar*")
        if (report.mealPlans.isEmpty()) {
            sb.appendLine("Sem plano configurado.")
        } else {
            report.mealPlans.forEach { plan ->
                val unitForPlan = report.meals.firstOrNull { it.mealPlanId == plan.id }?.quantityUnit ?: "g"
                val qty = plan.dailyQuantityGrams?.let { " · ${it.toInt()}$unitForPlan/dia" } ?: ""
                sb.appendLine("• ${foodTypeLabel(plan.foodType)}$qty")
                if (!plan.foodDetails.isNullOrBlank()) sb.appendLine("  Alimento: ${plan.foodDetails}")
                if (!plan.restrictions.isNullOrBlank()) sb.appendLine("  Restrições: ${plan.restrictions}")
            }
        }
        if (report.mealLogs.isEmpty() && report.sporadicLogs.isEmpty()) {
            sb.appendLine("Nenhuma refeição registrada no período.")
        } else {
            val mealsById = report.meals.associateBy { it.id }
            if (report.mealLogs.isNotEmpty()) {
                report.mealLogs
                    .sortedBy { it.date }
                    .groupBy { dateFmt.format(Date(it.date)) }
                    .forEach { (date, logs) ->
                        val unit = logs.firstOrNull()?.let { mealsById[it.mealId]?.quantityUnit } ?: "g"
                        val total = logs.sumOf { log ->
                            (mealsById[log.mealId]?.quantityGrams ?: 0.0) * log.eatenPercentage / 100.0
                        }
                        sb.appendLine("  $date - ${total.toInt()}$unit plano")
                    }
            }
            if (report.sporadicLogs.isNotEmpty()) {
                sb.appendLine("Extras:")
                report.sporadicLogs
                    .sortedBy { it.registeredAt }
                    .groupBy { dateFmt.format(Date(it.registeredAt)) }
                    .forEach { (date, logs) ->
                        val byUnit = logs
                            .filter { it.amountGrams != null }
                            .groupBy { it.amountUnit }
                            .mapValues { (_, e) -> e.sumOf { it.amountGrams ?: 0.0 } }
                        val amounts = byUnit.entries.joinToString(" + ") { (u, v) -> "${v.toInt()}$u" }
                        val extra = if (amounts.isNotBlank()) " ($amounts)" else ""
                        sb.appendLine("  $date - ${logs.size} extra(s)$extra")
                    }
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

        // Vacinas e Vermífugos
        if (report.lastVaccineDoses.isNotEmpty()) {
            val vaccines  = report.lastVaccineDoses.filter { it.type == "VACCINE" }
            val dewormers = report.lastVaccineDoses.filter { it.type == "DEWORMER" }
            sb.appendLine("*💉 Vacinas e Vermífugos — última dose*")
            if (vaccines.isNotEmpty()) {
                sb.appendLine("_Vacinas:_")
                vaccines.forEach { v ->
                    val date = v.administeredAt?.let { dateFmt.format(Date(it)) } ?: "—"
                    val next = v.nextDueDate?.let { "  próxima: ${dateFmt.format(Date(it))}" } ?: ""
                    sb.appendLine("• ${v.name} — $date$next")
                    if (!v.notes.isNullOrBlank()) sb.appendLine("  ${v.notes}")
                }
            }
            if (dewormers.isNotEmpty()) {
                sb.appendLine("_Vermífugos:_")
                dewormers.forEach { v ->
                    val date = v.administeredAt?.let { dateFmt.format(Date(it)) } ?: "—"
                    val next = v.nextDueDate?.let { "  próxima: ${dateFmt.format(Date(it))}" } ?: ""
                    sb.appendLine("• ${v.name} — $date$next")
                    if (!v.notes.isNullOrBlank()) sb.appendLine("  ${v.notes}")
                }
            }
            sb.appendLine()
        }

        // Peso
        if (report.weightRecords.isNotEmpty()) {
            sb.appendLine("*⚖️ Últimas pesagens*")
            report.weightRecords.takeLast(5).forEach { w ->
                sb.appendLine("• ${dateFmt.format(Date(w.date))}: ${w.weightKg} kg${if (!w.notes.isNullOrBlank()) " (${w.notes})" else ""}")
            }
            sb.appendLine()
        }

        // Fotos
        if (report.photos.isNotEmpty()) {
            val photosByDay = report.photos.groupBy { dateFmt.format(Date(it.entryDate)) }
            sb.appendLine("*📷 Fotos registradas*")
            photosByDay.forEach { (date, photos) ->
                val n = photos.size
                sb.appendLine("  $date — $n ${if (n == 1) "foto" else "fotos"} (disponível no app)")
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

        // Desenha texto com quebra de linha automática — textos longos não passam da borda
        fun text(txt: String, paint: Paint, indent: Float = 0f) {
            val maxWidth = usable - indent
            val lh = paint.textSize * 1.45f
            val words = txt.split(" ")
            var line = ""
            for (word in words) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    line = candidate
                } else {
                    if (line.isNotEmpty()) {
                        breakPageIfNeeded(lh)
                        canvas.drawText(line, ml + indent, y + paint.textSize, paint)
                        y += lh
                    }
                    line = word
                }
            }
            if (line.isNotEmpty()) {
                breakPageIfNeeded(lh)
                canvas.drawText(line, ml + indent, y + paint.textSize, paint)
                y += lh
            }
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
        val speciesLabelPdf = speciesLabel(pet.species, pet.customSpecies)
        val sexLabel     = if (pet.sex == "MALE") "Macho" else "Fêmea"
        text("Nome: ${pet.name}   Espécie: $speciesLabelPdf   Peso: ${pet.weightKg} kg", paintBody)
        val breedSex = buildString {
            if (!pet.breed.isNullOrBlank()) append("Raça: ${pet.breed}   ")
            append("Sexo: $sexLabel")
            if (pet.isNeutered) append(" (castrado)")
        }
        text(breedSex, paintBody)
        pet.birthDate?.let { text("Idade: ${calculateAge(it)}", paintBody) }

        // ── Medicamentos ─────────────────────────────────────────────────────
        section("Medicamentos em uso")
        if (report.activeMedications.isEmpty()) {
            text("Nenhum medicamento ativo.", paintBody)
        } else {
            val timeFmtPdf = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))
            report.activeMedications.forEach { med ->
                text("• ${med.name}   ${med.dose} ${med.doseUnit}", paintBody)
                val freq = if (med.frequencyType == "INTERVAL")
                    "A cada ${med.frequencyHours}h"
                else
                    "Horários: ${med.fixedTimes?.cleanJson() ?: ""}"
                text(freq, paintSmall, 12f)
                if (!med.observations.isNullOrBlank()) text("Obs: ${med.observations}", paintSmall, 12f)
                val logsThisMed = report.medicationLogs
                    .filter { it.medicationId == med.id }
                    .sortedBy { it.scheduledAt }
                if (logsThisMed.isNotEmpty()) {
                    text("Administrações no período:", paintSmall, 12f)
                    logsThisMed.forEach { log ->
                        val statusLabel = when (log.status) {
                            "TAKEN"     -> "Administrado"
                            "NOT_TAKEN" -> "Não administrado"
                            "VOMITED"   -> "Administrado (vomitou)"
                            else        -> "Pendente"
                        }
                        text("${timeFmtPdf.format(Date(log.scheduledAt))} — $statusLabel", paintSmall, 24f)
                    }
                }
            }
        }

        // ── Alimentação ───────────────────────────────────────────────────────
        section("Plano alimentar")
        if (report.mealPlans.isEmpty()) {
            text("Sem plano configurado.", paintBody)
        } else {
            report.mealPlans.forEach { plan ->
                val unitForPlan = report.meals.firstOrNull { it.mealPlanId == plan.id }?.quantityUnit ?: "g"
                val qty = plan.dailyQuantityGrams?.let { "   ${it.toInt()}$unitForPlan/dia" } ?: ""
                text("• ${foodTypeLabel(plan.foodType)}$qty", paintBody)
                if (!plan.foodDetails.isNullOrBlank()) text("Alimento: ${plan.foodDetails}", paintSmall, 12f)
                if (!plan.restrictions.isNullOrBlank()) text("Restrições: ${plan.restrictions}", paintSmall, 12f)
            }
        }
        if (report.mealLogs.isEmpty() && report.sporadicLogs.isEmpty()) {
            text("Nenhuma refeição registrada no período.", paintSmall)
        } else {
            val mealsById = report.meals.associateBy { it.id }
            if (report.mealLogs.isNotEmpty()) {
                report.mealLogs
                    .sortedBy { it.date }
                    .groupBy { dateFmt.format(Date(it.date)) }
                    .forEach { (date, logs) ->
                        val unit = logs.firstOrNull()?.let { mealsById[it.mealId]?.quantityUnit } ?: "g"
                        val total = logs.sumOf { log ->
                            (mealsById[log.mealId]?.quantityGrams ?: 0.0) * log.eatenPercentage / 100.0
                        }
                        text("$date - ${total.toInt()}$unit plano", paintSmall, 12f)
                    }
            }
            if (report.sporadicLogs.isNotEmpty()) {
                text("Extras:", paintSmall)
                report.sporadicLogs
                    .sortedBy { it.registeredAt }
                    .groupBy { dateFmt.format(Date(it.registeredAt)) }
                    .forEach { (date, logs) ->
                        val byUnit = logs
                            .filter { it.amountGrams != null }
                            .groupBy { it.amountUnit }
                            .mapValues { (_, e) -> e.sumOf { it.amountGrams ?: 0.0 } }
                        val amounts = byUnit.entries.joinToString(" + ") { (u, v) -> "${v.toInt()}$u" }
                        val extra = if (amounts.isNotBlank()) " ($amounts)" else ""
                        text("$date - ${logs.size} extra(s)$extra", paintSmall, 12f)
                    }
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

        // ── Vacinas e Vermífugos ──────────────────────────────────────────────
        if (report.lastVaccineDoses.isNotEmpty()) {
            section("Vacinas e Vermífugos — última dose")
            val vaccines  = report.lastVaccineDoses.filter { it.type == "VACCINE" }
            val dewormers = report.lastVaccineDoses.filter { it.type == "DEWORMER" }
            if (vaccines.isNotEmpty()) {
                text("Vacinas:", paintBody)
                vaccines.forEach { v ->
                    val date = v.administeredAt?.let { dateFmt.format(Date(it)) } ?: "—"
                    val next = v.nextDueDate?.let { "   próxima: ${dateFmt.format(Date(it))}" } ?: ""
                    text("• ${v.name} — $date$next", paintSmall, 12f)
                    if (!v.notes.isNullOrBlank()) text("Obs: ${v.notes}", paintSmall, 24f)
                }
            }
            if (dewormers.isNotEmpty()) {
                text("Vermífugos:", paintBody)
                dewormers.forEach { v ->
                    val date = v.administeredAt?.let { dateFmt.format(Date(it)) } ?: "—"
                    val next = v.nextDueDate?.let { "   próxima: ${dateFmt.format(Date(it))}" } ?: ""
                    text("• ${v.name} — $date$next", paintSmall, 12f)
                    if (!v.notes.isNullOrBlank()) text("Obs: ${v.notes}", paintSmall, 24f)
                }
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

        // ── Fotos ────────────────────────────────────────────────────────────
        if (report.photos.isNotEmpty()) {
            val timeFmtPdf = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
            section("Fotos registradas")
            val photosByDay = report.photos.groupBy { timeFmtPdf.format(Date(it.entryDate)) }
            photosByDay.forEach { (date, photos) ->
                val n = photos.size
                text("• $date — $n ${if (n == 1) "foto" else "fotos"} (disponível no app)", paintBody)
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

    // Remove colchetes e aspas do JSON de fixedTimes para exibição legível
    // Ex: ["08:00","20:00"] → 08:00, 20:00
    private fun String.cleanJson() =
        removePrefix("[").removeSuffix("]").replace("\"", "").replace(",", ", ")

    private fun speciesLabel(species: String, customSpecies: String?) = when (species) {
        "DOG"     -> "Cão"
        "CAT"     -> "Gato"
        "RABBIT"  -> "Coelho"
        "BIRD"    -> "Pássaro"
        "HAMSTER" -> "Hamster"
        "TURTLE"  -> "Tartaruga"
        "FISH"    -> "Peixe"
        "OTHER"   -> customSpecies?.takeIf { it.isNotBlank() } ?: "Outro"
        else      -> species
    }

    private fun calculateAge(birthDateMs: Long): String {
        val birth = Calendar.getInstance().also { it.timeInMillis = birthDateMs }
        val today = Calendar.getInstance()
        var years  = today.get(Calendar.YEAR)  - birth.get(Calendar.YEAR)
        var months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
        if (months < 0) { years--; months += 12 }
        if (today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)) {
            if (months == 0) { years--; months = 11 } else months--
        }
        return when {
            years > 0 && months > 0 -> "$years ${if (years == 1) "ano" else "anos"} e $months ${if (months == 1) "mês" else "meses"}"
            years > 0               -> "$years ${if (years == 1) "ano" else "anos"}"
            months > 0              -> "$months ${if (months == 1) "mês" else "meses"}"
            else                    -> "menos de 1 mês"
        }
    }

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
