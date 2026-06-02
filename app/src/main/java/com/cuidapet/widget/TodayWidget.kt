package com.cuidadopet.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
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

private data class NextDose(
    val timeMs: Long,
    val medName: String,
    val petName: String,
    val dose: String,
    val doseUnit: String
)

class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            TodayWidgetEntryPoint::class.java
        )
        val now = System.currentTimeMillis()
        val pets = entryPoint.petRepository().getAllPets().first()
        var nextDose: NextDose? = null

        for (pet in pets) {
            val meds = entryPoint.medicationRepository().getActiveMedications(pet.id).first()
            for (med in meds) {
                val t = computeNextDoseTime(med, now) ?: continue
                if (nextDose == null || t < nextDose.timeMs) {
                    nextDose = NextDose(t, med.name, pet.name, med.dose, med.doseUnit)
                }
            }
        }

        provideContent {
            GlanceTheme {
                WidgetContent(nextDose)
            }
        }
    }
}

private fun computeNextDoseTime(med: MedicationEntity, now: Long): Long? {
    return when (med.frequencyType) {
        "INTERVAL" -> {
            val hours = med.frequencyHours ?: return null
            val ms = hours * 3_600_000L
            if (now < med.startDate) return med.startDate
            val elapsed = now - med.startDate
            med.startDate + ((elapsed / ms) + 1) * ms
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

@Composable
private fun WidgetContent(nextDose: NextDose?) {
    val context = LocalContext.current

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.primary)
            .padding(horizontal = 10.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
            // Logo do app
            Image(
                provider = ImageProvider(R.mipmap.ic_launcher_round),
                contentDescription = null,
                modifier = GlanceModifier.size(36.dp)
            )

            Spacer(GlanceModifier.width(8.dp))

            Column(modifier = GlanceModifier.fillMaxWidth()) {
                if (nextDose == null) {
                    Text(
                        text  = context.getString(R.string.widget_all_up_to_date),
                        style = TextStyle(
                            color      = GlanceTheme.colors.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 12.sp
                        )
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text  = context.getString(R.string.widget_no_active_meds),
                        style = TextStyle(
                            color    = GlanceTheme.colors.onPrimary,
                            fontSize = 10.sp
                        )
                    )
                } else {
                    // Tempo relativo — destaque principal
                    Text(
                        text  = formatTimeUntil(context, nextDose.timeMs),
                        style = TextStyle(
                            color      = GlanceTheme.colors.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 13.sp
                        ),
                        maxLines = 1
                    )
                    Spacer(GlanceModifier.height(1.dp))
                    // Horário + nome do remédio
                    Text(
                        text     = "${formatDoseTime(nextDose.timeMs)} · ${nextDose.medName}",
                        style    = TextStyle(
                            color      = GlanceTheme.colors.onPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 10.sp
                        ),
                        modifier = GlanceModifier.fillMaxWidth(),
                        maxLines = 1
                    )
                    // Pet + dose
                    Text(
                        text     = "${nextDose.petName} · ${nextDose.dose} ${nextDose.doseUnit}",
                        style    = TextStyle(
                            color    = GlanceTheme.colors.onPrimary,
                            fontSize = 10.sp
                        ),
                        modifier = GlanceModifier.fillMaxWidth(),
                        maxLines = 1
                    )
                }
            }
        }
}