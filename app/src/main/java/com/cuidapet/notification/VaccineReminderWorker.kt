package com.cuidadopet.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cuidadopet.R

class VaccineReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_VACCINE_ID   = "vaccine_id"
        const val KEY_VACCINE_NAME = "vaccine_name"
        const val KEY_VACCINE_TYPE = "vaccine_type"
        const val KEY_PET_NAME     = "pet_name"
        const val KEY_DAYS_BEFORE  = "days_before"
    }

    override suspend fun doWork(): Result {
        val vaccineId   = inputData.getLong(KEY_VACCINE_ID, -1L)
        val vaccineName = inputData.getString(KEY_VACCINE_NAME) ?: return Result.failure()
        val vaccineType = inputData.getString(KEY_VACCINE_TYPE) ?: "VACCINE"
        val petName     = inputData.getString(KEY_PET_NAME) ?: "seu pet"
        val daysBefore  = inputData.getInt(KEY_DAYS_BEFORE, 0)

        if (vaccineId == -1L) return Result.failure()

        val typeLabel = context.getString(
            if (vaccineType == "VACCINE") R.string.notif_vaccine_type_vaccine else R.string.notif_vaccine_type_dewormer
        )
        val body = when (daysBefore) {
            7    -> context.getString(R.string.notif_vaccine_7days, vaccineName)
            3    -> context.getString(R.string.notif_vaccine_3days, vaccineName)
            else -> context.getString(R.string.notif_vaccine_now, vaccineName)
        }

        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            (vaccineId * 10 + daysBefore).toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_VACCINES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_vaccine_title, typeLabel, petName))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NotificationChannels.NOTIFICATION_BASE_VACCINE + (vaccineId * 10 + daysBefore).toInt(), notification)
            } catch (_: SecurityException) {
                // Permissão de notificação negada
            }
        }

        return Result.success()
    }
}
