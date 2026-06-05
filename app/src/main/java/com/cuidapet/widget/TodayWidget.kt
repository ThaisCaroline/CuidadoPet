package com.cuidadopet.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.unit.ColorProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.cuidadopet.MainActivity
import com.cuidadopet.R
import com.cuidadopet.data.db.entity.MedicationEntity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

private data class NextDose(
    val timeMs: Long,
    val medName: String,
    val petName: String,
    val dose: String,
    val doseUnit: String,
    val petPhotoPath: String? = null
)

class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            TodayWidgetEntryPoint::class.java
        )
        val now = System.currentTimeMillis()
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000L

        val pets = entryPoint.petRepository().getAllPets().first()
        var nextDose: NextDose? = null

        for (pet in pets) {
            val meds = entryPoint.medicationRepository().getActiveMedications(pet.id).first()
            val takenLogs = entryPoint.medicationRepository()
                .getLogsForPetInPeriod(pet.id, todayStart, todayEnd)
                .first()
                .filter { it.status == "TAKEN" }

            for (med in meds) {
                val medTaken = takenLogs.filter { it.medicationId == med.id }.map { it.scheduledAt }.toSet()
                val t = computeNextDoseTime(med, now, medTaken) ?: continue
                if (nextDose == null || t < nextDose.timeMs) {
                    nextDose = NextDose(t, med.name, pet.name, med.dose, med.doseUnit, pet.photoPath)
                }
            }
        }

        val petBitmap: Bitmap? = nextDose?.petPhotoPath?.let { path ->
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                val sampleSize = (bounds.outWidth / 144).coerceAtLeast(1)
                val raw = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
                raw?.let { centerCropSquare(it) }
            } catch (_: Exception) { null }
        }

        provideContent {
            GlanceTheme {
                WidgetContent(nextDose, petBitmap)
            }
        }
    }
}

private fun centerCropSquare(src: Bitmap): Bitmap {
    val side = minOf(src.width, src.height)
    val x = (src.width - side) / 2
    val y = (src.height - side) / 2
    return Bitmap.createBitmap(src, x, y, side, side)
}

private fun computeNextDoseTime(
    med: MedicationEntity,
    now: Long,
    takenScheduledTimes: Set<Long> = emptySet()
): Long? {
    // Retorna true se o horário candidato já foi administrado hoje (tolerância de 1h)
    fun isAlreadyTaken(candidateMs: Long): Boolean =
        takenScheduledTimes.any { kotlin.math.abs(it - candidateMs) < 3_600_000L }

    return when (med.frequencyType) {
        "INTERVAL" -> {
            val hours = med.frequencyHours ?: return null
            val ms = hours * 3_600_000L
            if (now < med.startDate) return med.startDate
            val elapsed = now - med.startDate
            var candidate = med.startDate + ((elapsed / ms) + 1) * ms
            // Avança se o próximo horário já foi administrado
            if (isAlreadyTaken(candidate)) candidate += ms
            candidate
        }
        "FIXED_TIMES" -> {
            val json = med.fixedTimes ?: return null
            try {
                json.trim().removeSurrounding("[", "]")
                    .split(",")
                    .mapNotNull { token ->
                        val t = token.trim().removeSurrounding("\"")
                        val parts = t.split(":")
                        if (parts.size < 2) return@mapNotNull null
                        val h = parts[0].toIntOrNull() ?: return@mapNotNull null
                        val m = parts[1].toIntOrNull() ?: return@mapNotNull null
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, h)
                            set(Calendar.MINUTE, m)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_MONTH, 1)
                        // Se o horário ainda está no futuro mas já foi administrado, vai pro dia seguinte
                        if (isAlreadyTaken(cal.timeInMillis)) cal.add(Calendar.DAY_OF_MONTH, 1)
                        cal.timeInMillis
                    }
                    .minOrNull()
            } catch (_: Exception) { null }
        }
        else -> null
    }
}

private fun formatTimeUntil(context: Context, nextMs: Long): String {
    val diff = (nextMs - System.currentTimeMillis()).coerceAtLeast(0L)
    val hours = diff / 3_600_000L
    val minutes = (diff % 3_600_000L) / 60_000L
    return when {
        hours >= 24              -> context.getString(R.string.widget_time_tomorrow)
        hours > 0 && minutes > 0 -> context.getString(R.string.widget_time_in_hours_minutes, hours, minutes)
        hours > 0                -> context.getString(R.string.widget_time_in_hours, hours)
        minutes > 0              -> context.getString(R.string.widget_time_in_minutes, minutes)
        else                     -> context.getString(R.string.widget_time_now)
    }
}

private fun formatDoseTime(nextMs: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = nextMs }
    return String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}

private fun pickBackgroundRes(): Int {
    val cal = Calendar.getInstance()
    val periodIndex = cal.get(Calendar.HOUR_OF_DAY) / 4
    val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
    return when (Random(dayOfYear * 6L + periodIndex).nextInt(6)) {
        0    -> R.drawable.widget_bg_1
        1    -> R.drawable.widget_bg_2
        2    -> R.drawable.widget_bg_3
        3    -> R.drawable.widget_bg_4
        4    -> R.drawable.widget_bg_5
        else -> R.drawable.widget_bg_6
    }
}

@Composable
private fun WidgetContent(nextDose: NextDose?, petBitmap: Bitmap?) {
    val context = LocalContext.current

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(pickBackgroundRes()))
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.Vertical.Top
    ) {
            Image(
                provider = if (petBitmap != null) ImageProvider(petBitmap)
                           else ImageProvider(R.mipmap.ic_launcher_round),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.size(36.dp).cornerRadius(18.dp)
            )

            Spacer(GlanceModifier.width(8.dp))

            Column(modifier = GlanceModifier.fillMaxWidth()) {
                if (nextDose == null) {
                    Text(
                        text  = context.getString(R.string.widget_all_up_to_date),
                        style = TextStyle(
                            color      = ColorProvider(android.R.color.white),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 14.sp
                        )
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text  = context.getString(R.string.widget_no_active_meds),
                        style = TextStyle(
                            color    = ColorProvider(android.R.color.white),
                            fontSize = 12.sp
                        )
                    )
                } else {
                    // Tempo relativo — destaque principal
                    Text(
                        text  = formatTimeUntil(context, nextDose.timeMs),
                        style = TextStyle(
                            color      = ColorProvider(android.R.color.white),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp
                        ),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(1.dp))
                    Text(
                        text     = "${formatDoseTime(nextDose.timeMs)} · ${nextDose.medName}",
                        style    = TextStyle(
                            color      = ColorProvider(android.R.color.white),
                            fontWeight = FontWeight.Medium,
                            fontSize   = 12.sp
                        ),
                        modifier = GlanceModifier.fillMaxWidth(),
                        maxLines = 1
                    )
                    Text(
                        text     = "${nextDose.petName} · ${nextDose.dose} ${nextDose.doseUnit}",
                        style    = TextStyle(
                            color    = ColorProvider(android.R.color.white),
                            fontSize = 11.sp
                        ),
                        modifier = GlanceModifier.fillMaxWidth(),
                        maxLines = 1
                    )
                }
            }
        }
}