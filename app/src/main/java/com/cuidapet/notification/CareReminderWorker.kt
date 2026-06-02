package com.cuidadopet.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cuidadopet.R
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.Calendar

class CareReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "care_reminder"
    }

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastOpen = prefs.getLong("last_app_open", 0L)
        if (System.currentTimeMillis() - lastOpen > 48L * 60 * 60 * 1000L) return Result.success()

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CareReminderEntryPoint::class.java
        )

        val midnightToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val midnightTomorrow = midnightToday + 24 * 60 * 60 * 1000L

        val pets = entryPoint.petRepository().getAllPets().first()

        pets.forEach { pet ->
            val activeMeds = entryPoint.medicationRepository()
                .getActiveMedications(pet.id).first()
            if (activeMeds.isEmpty()) return@forEach

            val logsToday = entryPoint.medicationRepository()
                .getLogsForPetInPeriod(pet.id, midnightToday, midnightTomorrow)
                .first()

            if (logsToday.isEmpty()) {
                showCareReminder(pet.name, pet.id)
            }
        }

        return Result.success()
    }

    private fun showCareReminder(petName: String, petId: Long) {
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            NotificationChannels.NOTIFICATION_BASE_CARE + petId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_care_title, petName))
            .setContentText(context.getString(R.string.notif_care_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NotificationChannels.NOTIFICATION_BASE_CARE + petId.toInt(),
                notification
            )
        } catch (_: SecurityException) { }
    }
}