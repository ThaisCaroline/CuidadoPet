package com.cuidadopet.domain

import android.content.Context
import com.cuidadopet.R
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
    fun generateText(context: Context, report: PetReport): String {
        val sb  = StringBuilder()
        val pet = report.pet

        val (actualStart, actualEnd) = actualDataPeriod(report)
        val periodLabel = if (dateFmt.format(Date(actualStart)) == dateFmt.format(Date(actualEnd)))
            dateFmt.format(Date(actualStart))
        else context.getString(R.string.report_date_range, dateFmt.format(Date(actualStart)), dateFmt.format(Date(actualEnd)))

        sb.appendLine(context.getString(R.string.report_text_title, pet.name))
        sb.appendLine(context.getString(R.string.report_period, periodLabel))
        sb.appendLine()

        // Informações do pet
        sb.appendLine(context.getString(R.string.report_text_section_pet))
        val speciesLabel  = speciesLabel(context, pet.species, pet.customSpecies)
        val sexLabel      = sexLabel(context, pet.sex)
        val neuteredLabel = if (pet.isNeutered) " ${context.getString(R.string.report_neutered)}" else ""
        sb.appendLine("$speciesLabel${if (!pet.breed.isNullOrBlank()) " · ${pet.breed}" else ""}")
        pet.birthDate?.let { sb.appendLine(context.getString(R.string.report_age, calculateAge(context, it))) }
        sb.appendLine("$sexLabel$neuteredLabel · ${pet.weightKg} kg")
        sb.appendLine()

        // Medicamentos
        sb.appendLine(context.getString(R.string.report_text_section_meds))
        if (report.activeMedications.isEmpty()) {
            sb.appendLine(context.getString(R.string.report_no_active_meds))
        } else {
            val timeFmt = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))
            report.activeMedications.forEach { med ->
                sb.appendLine("• " + context.getString(R.string.report_medication_line, med.name, med.dose, med.doseUnit))
                val freq = if (med.frequencyType == "INTERVAL")
                    "  " + context.getString(R.string.report_every_hours, med.frequencyHours)
                else
                    "  " + context.getString(R.string.report_schedule_times, med.fixedTimes?.cleanJson() ?: "")
                val periodLabel = if (med.isContinuous)
                    context.getString(R.string.report_since, dateFmt.format(Date(med.startDate)))
                else if (med.endDate != null)
                    context.getString(R.string.report_date_range, dateFmt.format(Date(med.startDate)), dateFmt.format(Date(med.endDate)))
                else
                    context.getString(R.string.report_starting_on, dateFmt.format(Date(med.startDate)))
                sb.appendLine("$freq · $periodLabel")
                if (!med.observations.isNullOrBlank()) sb.appendLine("  ${med.observations}")
                val logsThisMed = report.medicationLogs
                    .filter { it.medicationId == med.id }
                    .sortedBy { it.scheduledAt }
                if (logsThisMed.isNotEmpty()) {
                    sb.appendLine("  _${context.getString(R.string.report_administrations)}_")
                    logsThisMed.forEach { log ->
                        val statusLabel = medicationStatusLabel(context, log.status.orEmpty())
                        sb.appendLine("    " + context.getString(R.string.report_admin_line, timeFmt.format(Date(log.scheduledAt)), statusLabel))
                    }
                }
            }
        }
        sb.appendLine()

        // Alimentação
        sb.appendLine(context.getString(R.string.report_text_section_meals))
        if (report.mealPlans.isEmpty()) {
            sb.appendLine(context.getString(R.string.report_no_meal_plan))
        } else {
            report.mealPlans.forEach { plan ->
                val unitForPlan = report.meals.firstOrNull { it.mealPlanId == plan.id }?.quantityUnit ?: "g"
                val qty = plan.dailyQuantityGrams?.let { " · " + context.getString(R.string.report_daily_quantity, it.toInt(), unitForPlan) } ?: ""
                sb.appendLine("• ${foodTypeLabel(context, plan.foodType)}$qty")
                if (!plan.foodDetails.isNullOrBlank()) sb.appendLine("  " + context.getString(R.string.report_food_details, plan.foodDetails))
                if (!plan.restrictions.isNullOrBlank()) sb.appendLine("  " + context.getString(R.string.report_restrictions, plan.restrictions))
            }
        }
        if (report.mealLogs.isEmpty() && report.sporadicLogs.isEmpty()) {
            sb.appendLine(context.getString(R.string.report_no_meals))
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
                        sb.appendLine("  " + context.getString(R.string.report_meal_day_line, date, total.toInt(), unit))
                    }
            }
            if (report.sporadicLogs.isNotEmpty()) {
                sb.appendLine(context.getString(R.string.report_extras))
                report.sporadicLogs
                    .sortedBy { it.registeredAt }
                    .groupBy { dateFmt.format(Date(it.registeredAt)) }
                    .forEach { (date, logs) ->
                        val byUnit = logs
                            .filter { it.amountGrams != null }
                            .groupBy { it.amountUnit }
                            .mapValues { (_, e) -> e.sumOf { it.amountGrams ?: 0.0 } }
                        val amounts = byUnit.entries.joinToString(" + ") { (u, v) -> "${v.toInt()}$u" }
                        val extra = if (amounts.isNotBlank()) " " + context.getString(R.string.report_extra_amounts, amounts) else ""
                        val count = context.resources.getQuantityString(R.plurals.report_extra_count, logs.size, logs.size)
                        sb.appendLine("  " + context.getString(R.string.report_extra_day_line, date, count, extra))
                    }
            }
        }
        sb.appendLine()

        // Hidratação
        sb.appendLine(context.getString(R.string.report_text_section_water))
        if (report.waterLogs.isEmpty()) {
            sb.appendLine(context.getString(R.string.report_no_water))
        } else {
            val byDay = report.waterLogs
                .sortedBy { it.registeredAt }
                .groupBy { dateFmt.format(Date(it.registeredAt)) }
            byDay.forEach { (date, logs) ->
                sb.appendLine("  $date — ${logs.sumOf { it.amountMl }.toInt()} ml")
            }
            val totalMl = report.waterLogs.sumOf { it.amountMl }
            sb.appendLine("  " + context.getString(R.string.report_water_total, totalMl.toInt()))
        }
        sb.appendLine()

        // Diário de saúde
        sb.appendLine(context.getString(R.string.report_text_section_health, report.healthEntries.size))
        if (report.healthEntries.isEmpty()) {
            sb.appendLine(context.getString(R.string.report_no_health))
        } else {
            // Exibe no máximo os 5 registros mais recentes para não sobrecarregar o texto
            report.healthEntries.takeLast(5).forEach { entry ->
                sb.appendLine("• ${dateFmt.format(Date(entry.registeredAt))}")
                entry.behavior?.let    { sb.appendLine("  " + context.getString(R.string.report_behavior, behaviorLabel(context, it))) }
                entry.fecesStatus?.let { sb.appendLine("  " + context.getString(R.string.report_feces, fecesLabel(context, it))) }
                entry.urineStatus?.let { sb.appendLine("  " + context.getString(R.string.report_urine, urineLabel(context, it))) }
                entry.vomitCount?.let  { if (it > 0) sb.appendLine("  " + context.getString(R.string.report_vomits, it)) }
                entry.painSigns?.let   { if (it != "NONE") sb.appendLine("  " + context.getString(R.string.report_pain_text, painLabel(context, it))) }
                if (!entry.observations.isNullOrBlank()) sb.appendLine("  ${entry.observations}")
            }
            if (report.healthEntries.size > 5)
                sb.appendLine("  " + context.getString(R.string.report_health_more, report.healthEntries.size - 5))
        }
        sb.appendLine()

        // Vacinas e Vermífugos
        if (report.lastVaccineDoses.isNotEmpty()) {
            val vaccines  = report.lastVaccineDoses.filter { it.type == "VACCINE" }
            val dewormers = report.lastVaccineDoses.filter { it.type == "DEWORMER" }
            sb.appendLine(context.getString(R.string.report_text_section_vaccines))
            if (vaccines.isNotEmpty()) {
                sb.appendLine("_${context.getString(R.string.report_vaccines_label)}_")
                vaccines.forEach { v ->
                    val date = v.administeredAt?.let { dateFmt.format(Date(it)) } ?: "—"
                    val next = v.nextDueDate?.let { "  " + context.getString(R.string.report_next_due, dateFmt.format(Date(it))) } ?: ""
                    sb.appendLine("• " + context.getString(R.string.report_vaccine_line, v.name, date, next))
                    if (!v.notes.isNullOrBlank()) sb.appendLine("  ${v.notes}")
                }
            }
            if (dewormers.isNotEmpty()) {
                sb.appendLine("_${context.getString(R.string.report_dewormers_label)}_")
                dewormers.forEach { v ->
                    val date = v.administeredAt?.let { dateFmt.format(Date(it)) } ?: "—"
                    val next = v.nextDueDate?.let { "  " + context.getString(R.string.report_next_due, dateFmt.format(Date(it))) } ?: ""
                    sb.appendLine("• " + context.getString(R.string.report_vaccine_line, v.name, date, next))
                    if (!v.notes.isNullOrBlank()) sb.appendLine("  ${v.notes}")
                }
            }
            sb.appendLine()
        }

        // Peso
        if (report.weightRecords.isNotEmpty()) {
            sb.appendLine(context.getString(R.string.report_text_section_weights))
            report.weightRecords.takeLast(5).forEach { w ->
                sb.appendLine("• ${dateFmt.format(Date(w.date))}: ${w.weightKg} kg${if (!w.notes.isNullOrBlank()) " (${w.notes})" else ""}")
            }
            sb.appendLine()
        }

        // Fotos
        if (report.photos.isNotEmpty()) {
            val photosByDay = report.photos.groupBy { dateFmt.format(Date(it.entryDate)) }
            sb.appendLine(context.getString(R.string.report_text_section_photos))
            photosByDay.forEach { (date, photos) ->
                val n = photos.size
                val count = context.resources.getQuantityString(R.plurals.report_photo_count, n, n)
                sb.appendLine("  " + context.getString(R.string.report_photo_line, date, count))
            }
            sb.appendLine()
        }

        sb.appendLine(context.getString(R.string.report_footer_generated))
        sb.appendLine(context.getString(R.string.report_footer_disclaimer))
        sb.appendLine(context.getString(R.string.report_footer_not_substitute))

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
        else context.getString(R.string.report_date_range, dateFmt.format(Date(pdfStart)), dateFmt.format(Date(pdfEnd)))

        text(context.getString(R.string.report_pdf_title, report.pet.name), paintTitle)
        text(context.getString(R.string.report_period, pdfPeriod), paintSmall)
        y += 4f

        // ── Pet ──────────────────────────────────────────────────────────────
        section(context.getString(R.string.report_section_pet))
        val pet          = report.pet
        val speciesLabelPdf = speciesLabel(context, pet.species, pet.customSpecies)
        val sexLabel     = sexLabel(context, pet.sex)
        text(context.getString(R.string.report_name_species_weight, pet.name, speciesLabelPdf, pet.weightKg.toString()), paintBody)
        val breedSex = buildString {
            if (!pet.breed.isNullOrBlank()) append(context.getString(R.string.report_breed, pet.breed) + "   ")
            append(context.getString(R.string.report_sex, sexLabel))
            if (pet.isNeutered) append(" " + context.getString(R.string.report_neutered))
        }
        text(breedSex, paintBody)
        pet.birthDate?.let { text(context.getString(R.string.report_age, calculateAge(context, it)), paintBody) }

        // ── Medicamentos ─────────────────────────────────────────────────────
        section(context.getString(R.string.report_section_meds))
        if (report.activeMedications.isEmpty()) {
            text(context.getString(R.string.report_no_active_meds), paintBody)
        } else {
            val timeFmtPdf = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))
            report.activeMedications.forEach { med ->
                text("• " + context.getString(R.string.report_medication_line_pdf, med.name, med.dose, med.doseUnit), paintBody)
                val freq = if (med.frequencyType == "INTERVAL")
                    context.getString(R.string.report_every_hours, med.frequencyHours)
                else
                    context.getString(R.string.report_schedule_times, med.fixedTimes?.cleanJson() ?: "")
                val periodLabel = if (med.isContinuous)
                    context.getString(R.string.report_since, dateFmt.format(Date(med.startDate)))
                else if (med.endDate != null)
                    context.getString(R.string.report_date_range, dateFmt.format(Date(med.startDate)), dateFmt.format(Date(med.endDate)))
                else
                    context.getString(R.string.report_starting_on, dateFmt.format(Date(med.startDate)))
                text("$freq · $periodLabel", paintSmall, 12f)
                if (!med.observations.isNullOrBlank()) text(context.getString(R.string.report_obs, med.observations), paintSmall, 12f)
                val logsThisMed = report.medicationLogs
                    .filter { it.medicationId == med.id }
                    .sortedBy { it.scheduledAt }
                if (logsThisMed.isNotEmpty()) {
                    text(context.getString(R.string.report_administrations), paintSmall, 12f)
                    logsThisMed.forEach { log ->
                        val statusLabel = medicationStatusLabel(context, log.status.orEmpty())
                        text(context.getString(R.string.report_admin_line, timeFmtPdf.format(Date(log.scheduledAt)), statusLabel), paintSmall, 24f)
                    }
                }
            }
        }

        // ── Alimentação ───────────────────────────────────────────────────────
        section(context.getString(R.string.report_section_meals))
        if (report.mealPlans.isEmpty()) {
            text(context.getString(R.string.report_no_meal_plan), paintBody)
        } else {
            report.mealPlans.forEach { plan ->
                val unitForPlan = report.meals.firstOrNull { it.mealPlanId == plan.id }?.quantityUnit ?: "g"
                val qty = plan.dailyQuantityGrams?.let { "   " + context.getString(R.string.report_daily_quantity, it.toInt(), unitForPlan) } ?: ""
                text("• ${foodTypeLabel(context, plan.foodType)}$qty", paintBody)
                if (!plan.foodDetails.isNullOrBlank()) text(context.getString(R.string.report_food_details, plan.foodDetails), paintSmall, 12f)
                if (!plan.restrictions.isNullOrBlank()) text(context.getString(R.string.report_restrictions, plan.restrictions), paintSmall, 12f)
            }
        }
        if (report.mealLogs.isEmpty() && report.sporadicLogs.isEmpty()) {
            text(context.getString(R.string.report_no_meals), paintSmall)
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
                        text(context.getString(R.string.report_meal_day_line, date, total.toInt(), unit), paintSmall, 12f)
                    }
            }
            if (report.sporadicLogs.isNotEmpty()) {
                text(context.getString(R.string.report_extras), paintSmall)
                report.sporadicLogs
                    .sortedBy { it.registeredAt }
                    .groupBy { dateFmt.format(Date(it.registeredAt)) }
                    .forEach { (date, logs) ->
                        val byUnit = logs
                            .filter { it.amountGrams != null }
                            .groupBy { it.amountUnit }
                            .mapValues { (_, e) -> e.sumOf { it.amountGrams ?: 0.0 } }
                        val amounts = byUnit.entries.joinToString(" + ") { (u, v) -> "${v.toInt()}$u" }
                        val extra = if (amounts.isNotBlank()) " " + context.getString(R.string.report_extra_amounts, amounts) else ""
                        val count = context.resources.getQuantityString(R.plurals.report_extra_count, logs.size, logs.size)
                        text(context.getString(R.string.report_extra_day_line, date, count, extra), paintSmall, 12f)
                    }
            }
        }

        // ── Hidratação ────────────────────────────────────────────────────────
        section(context.getString(R.string.report_section_water))
        if (report.waterLogs.isEmpty()) {
            text(context.getString(R.string.report_no_water), paintBody)
        } else {
            val byDay = report.waterLogs
                .sortedBy { it.registeredAt }
                .groupBy { dateFmt.format(Date(it.registeredAt)) }
            byDay.forEach { (date, logs) ->
                text("$date — ${logs.sumOf { it.amountMl }.toInt()} ml", paintSmall, 12f)
            }
            val totalMl = report.waterLogs.sumOf { it.amountMl }
            text(context.getString(R.string.report_water_total, totalMl.toInt()), paintBody)
        }

        // ── Diário de saúde ───────────────────────────────────────────────────
        section(context.getString(R.string.report_section_health, report.healthEntries.size))
        if (report.healthEntries.isEmpty()) {
            text(context.getString(R.string.report_no_health), paintBody)
        } else {
            report.healthEntries.forEach { entry ->
                text(dateFmt.format(Date(entry.registeredAt)), paintBody)
                entry.behavior?.let    { text(context.getString(R.string.report_behavior, behaviorLabel(context, it)), paintSmall, 12f) }
                entry.fecesStatus?.let { text(context.getString(R.string.report_feces, fecesLabel(context, it)), paintSmall, 12f) }
                entry.urineStatus?.let { text(context.getString(R.string.report_urine, urineLabel(context, it)), paintSmall, 12f) }
                entry.vomitCount?.let  { if (it > 0) text(context.getString(R.string.report_vomits, it), paintSmall, 12f) }
                entry.painSigns?.let   { if (it != "NONE") text(context.getString(R.string.report_pain_pdf, painLabel(context, it)), paintSmall, 12f) }
                if (!entry.observations.isNullOrBlank()) text(context.getString(R.string.report_obs, entry.observations), paintSmall, 12f)
                y += 3f
            }
        }

        // ── Vacinas e Vermífugos ──────────────────────────────────────────────
        if (report.lastVaccineDoses.isNotEmpty()) {
            section(context.getString(R.string.report_section_vaccines))
            val vaccines  = report.lastVaccineDoses.filter { it.type == "VACCINE" }
            val dewormers = report.lastVaccineDoses.filter { it.type == "DEWORMER" }
            if (vaccines.isNotEmpty()) {
                text(context.getString(R.string.report_vaccines_label), paintBody)
                vaccines.forEach { v ->
                    val date = v.administeredAt?.let { dateFmt.format(Date(it)) } ?: "—"
                    val next = v.nextDueDate?.let { "   " + context.getString(R.string.report_next_due, dateFmt.format(Date(it))) } ?: ""
                    text("• " + context.getString(R.string.report_vaccine_line, v.name, date, next), paintSmall, 12f)
                    if (!v.notes.isNullOrBlank()) text(context.getString(R.string.report_obs, v.notes), paintSmall, 24f)
                }
            }
            if (dewormers.isNotEmpty()) {
                text(context.getString(R.string.report_dewormers_label), paintBody)
                dewormers.forEach { v ->
                    val date = v.administeredAt?.let { dateFmt.format(Date(it)) } ?: "—"
                    val next = v.nextDueDate?.let { "   " + context.getString(R.string.report_next_due, dateFmt.format(Date(it))) } ?: ""
                    text("• " + context.getString(R.string.report_vaccine_line, v.name, date, next), paintSmall, 12f)
                    if (!v.notes.isNullOrBlank()) text(context.getString(R.string.report_obs, v.notes), paintSmall, 24f)
                }
            }
        }

        // ── Histórico de peso ─────────────────────────────────────────────────
        if (report.weightRecords.isNotEmpty()) {
            section(context.getString(R.string.report_section_weights))
            report.weightRecords.forEach { w ->
                val note = if (!w.notes.isNullOrBlank()) "  (${w.notes})" else ""
                text("• ${dateFmt.format(Date(w.date))}: ${w.weightKg} kg$note", paintBody)
            }
        }

        // ── Fotos ────────────────────────────────────────────────────────────
        if (report.photos.isNotEmpty()) {
            val timeFmtPdf = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
            section(context.getString(R.string.report_section_photos))
            val photosByDay = report.photos.groupBy { timeFmtPdf.format(Date(it.entryDate)) }
            photosByDay.forEach { (date, photos) ->
                val n = photos.size
                val count = context.resources.getQuantityString(R.plurals.report_photo_count, n, n)
                text("• " + context.getString(R.string.report_photo_line, date, count), paintBody)
            }
        }

        // ── Rodapé ────────────────────────────────────────────────────────────
        breakPageIfNeeded(50f)
        y += 14f
        canvas.drawLine(ml, y, pageW - mr, y, paintLine)
        y += 6f
        text(context.getString(R.string.report_pdf_footer_generated), paintSmall)
        text(context.getString(R.string.report_pdf_footer_disclaimer), paintSmall)
        text(context.getString(R.string.report_pdf_footer_not_substitute), paintSmall)

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

    private fun appetiteLabel(context: Context, status: String, pct: Int): String = when (status) {
        "ALL"     -> context.getString(R.string.report_appetite_all)
        "PARTIAL" -> context.getString(R.string.report_appetite_partial, pct)
        "REFUSED" -> context.getString(R.string.report_appetite_refused)
        else      -> context.getString(R.string.report_appetite_pct, pct)
    }

    private fun sexLabel(context: Context, sex: String): String =
        if (sex == "MALE") context.getString(R.string.report_male) else context.getString(R.string.report_female)

    private fun medicationStatusLabel(context: Context, status: String): String = context.getString(
        when (status) {
            "TAKEN"     -> R.string.report_status_taken
            "NOT_TAKEN" -> R.string.report_status_not_taken
            "VOMITED"   -> R.string.report_status_vomited
            else        -> R.string.report_status_pending
        }
    )

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

    private fun speciesLabel(context: Context, species: String, customSpecies: String?) = when (species) {
        "DOG"     -> context.getString(R.string.report_species_dog)
        "CAT"     -> context.getString(R.string.report_species_cat)
        "RABBIT"  -> context.getString(R.string.report_species_rabbit)
        "BIRD"    -> context.getString(R.string.report_species_bird)
        "HAMSTER" -> context.getString(R.string.report_species_hamster)
        "TURTLE"  -> context.getString(R.string.report_species_turtle)
        "FISH"    -> context.getString(R.string.report_species_fish)
        "OTHER"   -> customSpecies?.takeIf { it.isNotBlank() } ?: context.getString(R.string.report_other)
        else      -> species
    }

    private fun calculateAge(context: Context, birthDateMs: Long): String {
        val birth = Calendar.getInstance().also { it.timeInMillis = birthDateMs }
        val today = Calendar.getInstance()
        var years  = today.get(Calendar.YEAR)  - birth.get(Calendar.YEAR)
        var months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
        if (months < 0) { years--; months += 12 }
        if (today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)) {
            if (months == 0) { years--; months = 11 } else months--
        }
        val yearsText  = context.resources.getQuantityString(R.plurals.report_age_years, years, years)
        val monthsText = context.resources.getQuantityString(R.plurals.report_age_months, months, months)
        return when {
            years > 0 && months > 0 -> context.getString(R.string.report_age_years_months, yearsText, monthsText)
            years > 0               -> yearsText
            months > 0              -> monthsText
            else                    -> context.getString(R.string.report_age_under_month)
        }
    }

    private fun foodTypeLabel(context: Context, code: String) = when (code) {
        "DRY_KIBBLE"  -> context.getString(R.string.report_food_dry)
        "WET_FOOD"    -> context.getString(R.string.report_food_wet)
        "NATURAL"     -> context.getString(R.string.report_food_natural)
        "THERAPEUTIC" -> context.getString(R.string.report_food_therapeutic)
        else          -> context.getString(R.string.report_other)
    }

    private fun behaviorLabel(context: Context, code: String) = when (code) {
        "NORMAL"    -> context.getString(R.string.report_normal)
        "LETHARGIC" -> context.getString(R.string.report_lethargic)
        "AGITATED"  -> context.getString(R.string.report_agitated)
        "SLEEPY"    -> context.getString(R.string.report_sleepy)
        else        -> code
    }

    private fun fecesLabel(context: Context, code: String) = when (code) {
        "NORMAL"   -> context.getString(R.string.report_normal)
        "SOFT"     -> context.getString(R.string.report_feces_soft)
        "DIARRHEA" -> context.getString(R.string.report_feces_diarrhea)
        "ABSENT"   -> context.getString(R.string.report_feces_absent)
        "BLOOD"    -> context.getString(R.string.report_bloody)
        else       -> code
    }

    private fun urineLabel(context: Context, code: String) = when (code) {
        "NORMAL"    -> context.getString(R.string.report_normal)
        "INCREASED" -> context.getString(R.string.report_urine_increased)
        "REDUCED"   -> context.getString(R.string.report_urine_reduced)
        "ABSENT"    -> context.getString(R.string.report_urine_absent)
        "BLOOD"     -> context.getString(R.string.report_bloody)
        else        -> code
    }

    private fun painLabel(context: Context, code: String) = when (code) {
        "NONE"     -> context.getString(R.string.report_pain_none)
        "APPARENT" -> context.getString(R.string.report_pain_apparent)
        "EVIDENT"  -> context.getString(R.string.report_pain_evident)
        else       -> code
    }
}
