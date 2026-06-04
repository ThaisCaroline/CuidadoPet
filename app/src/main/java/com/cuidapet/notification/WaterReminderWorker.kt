package com.cuidadopet.notification

import android.app.PendingIntent
import com.cuidadopet.R
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar

// Worker do WorkManager responsável por exibir o lembrete periódico de água.
// O WorkManager chama o doWork() automaticamente no intervalo configurado.
//
// Diferença entre AlarmManager (medicamentos) e WorkManager (água):
// - AlarmManager: alarmes exatos, críticos, não podem atrasar.
// - WorkManager: tarefas periódicas, podem atrasar alguns minutos —
//   aceitável para um lembrete de "ofereça água", não para medicamento.
class WaterReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        // Chaves para os dados de entrada passados ao agendar o worker
        const val KEY_PET_NAME        = "pet_name"
        const val KEY_PET_ID          = "pet_id"
        const val KEY_TARGET_ML       = "target_ml"
        const val KEY_START_TIME      = "start_time"
        const val KEY_END_TIME        = "end_time"
        const val KEY_IS_SUPER_REMINDER = "is_super_reminder"

        // Tag única por pet — usada para cancelar os lembretes quando necessário
        fun workTag(petId: Long) = "water_reminder_$petId"

        private fun parseMinutes(time: String): Int {
            val parts = time.split(":")
            return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 +
                   (parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }
    }

    override suspend fun doWork(): Result {
        val petName         = inputData.getString(KEY_PET_NAME)  ?: "seu pet"
        val petId           = inputData.getLong(KEY_PET_ID, -1L)
        val targetMl        = inputData.getDouble(KEY_TARGET_ML, 0.0)
        val startTime       = inputData.getString(KEY_START_TIME) ?: "08:00"
        val endTime         = inputData.getString(KEY_END_TIME)   ?: "23:55"
        val isSuperReminder = inputData.getBoolean(KEY_IS_SUPER_REMINDER, false)

        if (petId == -1L) return Result.failure()

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        if (currentMinutes < parseMinutes(startTime) || currentMinutes >= parseMinutes(endTime)) {
            return Result.success()
        }

        if (isSuperReminder) {
            val notifId = NotificationChannels.NOTIFICATION_BASE_SUPER + petId.toInt() + 20000
            showSuperReminderNotification(
                context     = context,
                type        = SuperReminderActivity.TYPE_WATER,
                id          = petId,
                notifId     = notifId,
                petName     = petName,
                label       = context.getString(R.string.notif_water_title, petName),
                dose        = if (targetMl > 0) context.getString(R.string.notif_water_target, targetMl.toInt()) else "",
                scheduledAt = System.currentTimeMillis(),
                amount      = targetMl
            )
        } else {
            showWaterReminder(petName, petId, targetMl)
        }

        return Result.success()
    }

    private fun showWaterReminder(petName: String, petId: Long, targetMl: Double) {
        val notifId = NotificationChannels.NOTIFICATION_BASE_WATER + petId.toInt()

        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentPending = PendingIntent.getActivity(
            context, notifId, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeData = Intent().apply {
            putExtra(PushSnoozeReceiver.EXTRA_TYPE, PushSnoozeReceiver.TYPE_WATER)
            putExtra(PushSnoozeReceiver.EXTRA_PET_NAME, petName)
            putExtra(PushSnoozeReceiver.EXTRA_PET_ID, petId)
            putExtra(PushSnoozeReceiver.EXTRA_TARGET_ML, targetMl)
        }
        val snoozePending = PushSnoozeReceiver.snoozePendingIntent(context, notifId, snoozeData)

        val targetText = if (targetMl > 0) context.getString(R.string.notif_water_target, targetMl.toInt()) else ""
        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_WATER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_water_title, petName))
            .setContentText(context.getString(R.string.notif_water_body, targetText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentPending)
            .addAction(0, context.getString(R.string.super_reminder_snooze), snoozePending)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try { notify(notifId, notification) } catch (_: SecurityException) {}
        }
    }
}
