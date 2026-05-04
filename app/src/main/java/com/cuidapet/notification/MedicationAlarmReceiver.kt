package com.cuidadopet.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cuidadopet.MainActivity
import com.cuidadopet.R
import com.cuidadopet.data.db.entity.MedicationEntity

// BroadcastReceiver é um componente Android que recebe "broadcasts" — mensagens do sistema.
// Quando o AlarmManager dispara um alarme, ele envia um broadcast.
// Este receiver escuta esse broadcast, exibe a notificação e reagenda o próximo alarme.
//
// Funciona mesmo com o app fechado — o Android acorda o processo só para executar onReceive().
class MedicationAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MEDICATION_ID    = "medication_id"
        const val EXTRA_MEDICATION_NAME  = "medication_name"
        const val EXTRA_PET_NAME         = "pet_name"
        const val EXTRA_DOSE             = "dose"
        const val EXTRA_DOSE_UNIT        = "dose_unit"
        const val EXTRA_PET_ID           = "pet_id"
        const val EXTRA_FORM             = "form"
        const val EXTRA_FREQUENCY_TYPE   = "frequency_type"
        const val EXTRA_FREQUENCY_HOURS  = "frequency_hours"
        const val EXTRA_FIXED_TIMES      = "fixed_times"
        const val EXTRA_START_DATE       = "start_date"
        const val EXTRA_END_DATE         = "end_date"
        const val EXTRA_IS_CONTINUOUS    = "is_continuous"
        const val EXTRA_REMINDER_ENABLED = "reminder_enabled"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId   = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medicamento"
        val petName        = intent.getStringExtra(EXTRA_PET_NAME) ?: "seu pet"
        val dose           = intent.getStringExtra(EXTRA_DOSE) ?: ""
        val doseUnit       = intent.getStringExtra(EXTRA_DOSE_UNIT) ?: ""

        if (medicationId == -1L) return

        // ── Exibe a notificação ───────────────────────────────────────────────

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            medicationId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doseText    = if (dose.isNotBlank()) " • $dose $doseUnit" else ""
        val contentText = "$petName — $medicationName$doseText"

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_MEDICATIONS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hora do medicamento!")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            NotificationChannels.NOTIFICATION_BASE_MEDICATION + medicationId.toInt(),
            notification
        )

        // ── Reagenda o próximo alarme ─────────────────────────────────────────
        // Sem isso o alarme dispararia só uma vez e nunca mais voltaria.

        val reminderEnabled = intent.getBooleanExtra(EXTRA_REMINDER_ENABLED, true)
        if (!reminderEnabled) return

        val endDate      = intent.getLongExtra(EXTRA_END_DATE, -1L)
        val isContinuous = intent.getBooleanExtra(EXTRA_IS_CONTINUOUS, false)

        // Não reagenda se o tratamento já terminou
        if (!isContinuous && endDate != -1L && endDate < System.currentTimeMillis()) return

        // Reconstrói a entidade do medicamento com os dados do Intent para poder reagendar
        val medication = MedicationEntity(
            id              = medicationId,
            petId           = intent.getLongExtra(EXTRA_PET_ID, 0L),
            name            = medicationName,
            form            = intent.getStringExtra(EXTRA_FORM) ?: "ORAL",
            dose            = dose,
            doseUnit        = doseUnit,
            frequencyType   = intent.getStringExtra(EXTRA_FREQUENCY_TYPE) ?: "FIXED_TIMES",
            frequencyHours  = intent.getIntExtra(EXTRA_FREQUENCY_HOURS, -1).takeIf { it != -1 },
            fixedTimes      = intent.getStringExtra(EXTRA_FIXED_TIMES)?.ifBlank { null },
            startDate       = intent.getLongExtra(EXTRA_START_DATE, System.currentTimeMillis()),
            endDate         = endDate.takeIf { it != -1L },
            isContinuous    = isContinuous,
            reminderEnabled = true
        )

        AlarmScheduler(context).scheduleMedication(medication, petName)
    }
}
