package com.cuidadopet.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cuidadopet.MainActivity
import com.cuidadopet.R
import com.cuidadopet.data.repository.MedicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// BroadcastReceiver é um componente Android que recebe "broadcasts" — mensagens do sistema.
// Quando o AlarmManager dispara um alarme, ele envia um broadcast.
// Este receiver escuta esse broadcast, exibe a notificação e reagenda o próximo alarme.
//
// Funciona mesmo com o app fechado — o Android acorda o processo só para executar onReceive().
@AndroidEntryPoint
class MedicationAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler

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
        const val EXTRA_REMINDER_ENABLED  = "reminder_enabled"
        const val EXTRA_IS_SUPER_REMINDER = "is_super_reminder"
        const val EXTRA_SCHEDULED_AT      = "scheduled_at"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId    = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val medicationName  = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medicamento"
        val petName         = intent.getStringExtra(EXTRA_PET_NAME) ?: "seu pet"
        val dose            = intent.getStringExtra(EXTRA_DOSE) ?: ""
        val doseUnit        = intent.getStringExtra(EXTRA_DOSE_UNIT) ?: ""
        val isSuperReminder = intent.getBooleanExtra(EXTRA_IS_SUPER_REMINDER, false)

        if (medicationId == -1L) return

        // ── Exibe a notificação ───────────────────────────────────────────────

        // scheduledAt vem do intent (timestamp exato agendado pelo AlarmScheduler).
        // Usar System.currentTimeMillis() aqui seria incorreto: o TodayViewModel busca
        // logs por scheduledAt == timestamp_exato_calculado (sem tolerância), então
        // qualquer diferença de milissegundos faria a dose não aparecer como administrada.
        val scheduledAt = intent.getLongExtra(EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        val notifId = NotificationChannels.NOTIFICATION_BASE_MEDICATION + medicationId.toInt()

        if (isSuperReminder) {
            showSuperReminderNotification(
                context  = context,
                type     = SuperReminderActivity.TYPE_MEDICATION,
                id       = medicationId,
                notifId  = NotificationChannels.NOTIFICATION_BASE_SUPER + medicationId.toInt(),
                petName  = petName,
                label    = medicationName,
                dose     = if (dose.isNotBlank()) "$dose $doseUnit" else "",
                scheduledAt = scheduledAt
            )
        } else {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, medicationId.toInt(), openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val doseText = if (dose.isNotBlank()) " • $dose $doseUnit" else ""
            val adminIntent = Intent(context, MedicationAdminReceiver::class.java).apply {
                putExtra(MedicationAdminReceiver.EXTRA_MEDICATION_ID, medicationId)
                putExtra(MedicationAdminReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
                putExtra(MedicationAdminReceiver.EXTRA_NOTIFICATION_ID, notifId)
            }
            val adminPendingIntent = PendingIntent.getBroadcast(
                context, (medicationId + 20000L).toInt(), adminIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val snoozeData = Intent().apply {
                putExtra(PushSnoozeReceiver.EXTRA_TYPE, PushSnoozeReceiver.TYPE_MEDICATION)
                putExtra(PushSnoozeReceiver.EXTRA_PET_NAME, petName)
                putExtra(PushSnoozeReceiver.EXTRA_MEDICATION_ID, medicationId)
                putExtra(PushSnoozeReceiver.EXTRA_MEDICATION_NAME, medicationName)
                putExtra(PushSnoozeReceiver.EXTRA_DOSE, dose)
                putExtra(PushSnoozeReceiver.EXTRA_DOSE_UNIT, doseUnit)
                putExtra(PushSnoozeReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
            }
            val snoozePending = PushSnoozeReceiver.snoozePendingIntent(context, notifId, snoozeData)
            val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_MEDICATIONS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notif_medication_title))
                .setContentText("$petName — $medicationName$doseText")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .addAction(0, context.getString(R.string.action_administered), adminPendingIntent)
                .addAction(0, context.getString(R.string.super_reminder_snooze), snoozePending)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }

        // ── Reagenda o próximo alarme ─────────────────────────────────────────
        // Busca dados atuais do banco para não reagendar com horário desatualizado
        // caso o medicamento tenha sido editado após o último disparo.
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val medication = medicationRepository.getMedicationById(medicationId).first()
                if (medication != null && medication.isActive && medication.reminderEnabled) {
                    val ended = !medication.isContinuous
                            && medication.endDate != null
                            && medication.endDate < System.currentTimeMillis()
                    if (!ended) alarmScheduler.scheduleMedication(medication, petName)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
