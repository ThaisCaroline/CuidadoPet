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

class ReEngagementWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "reengagement_check"
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000L
    }

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastOpen = prefs.getLong("last_app_open", 0L)
        val stage = prefs.getInt("reengagement_stage", 0)

        if (stage >= 3 || lastOpen == 0L) return Result.success()

        val daysSinceOpen = (System.currentTimeMillis() - lastOpen) / DAY_MILLIS

        val nextStage = when {
            stage == 0 && daysSinceOpen >= 7  -> 1
            stage == 1 && daysSinceOpen >= 11 -> 2
            stage == 2 && daysSinceOpen >= 15 -> 3
            else -> return Result.success()
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            CareReminderEntryPoint::class.java
        )
        val pets = entryPoint.petRepository().getAllPets().first()
        val petsName = buildPetsName(pets.map { it.name })

        val (titleRes, bodyRes) = when (nextStage) {
            1 -> R.string.notif_reengagement_d7_title to listOf(
                R.string.notif_reengagement_d7_1,
                R.string.notif_reengagement_d7_2,
                R.string.notif_reengagement_d7_3
            ).random()
            2 -> R.string.notif_reengagement_d11_title to listOf(
                R.string.notif_reengagement_d11_1,
                R.string.notif_reengagement_d11_2,
                R.string.notif_reengagement_d11_3
            ).random()
            else -> R.string.notif_reengagement_d15_title to listOf(
                R.string.notif_reengagement_d15_1,
                R.string.notif_reengagement_d15_2,
                R.string.notif_reengagement_d15_3
            ).random()
        }

        showNotification(context.getString(titleRes), context.getString(bodyRes, petsName))
        prefs.edit().putInt("reengagement_stage", nextStage).apply()

        return Result.success()
    }

    private fun buildPetsName(names: List<String>): String = when (names.size) {
        0    -> context.getString(R.string.reengagement_pets_fallback)
        1    -> names[0]
        2    -> context.getString(R.string.reengagement_pets_two, names[0], names[1])
        else -> context.getString(R.string.reengagement_pets_multiple, names[0])
    }

    private fun showNotification(title: String, body: String) {
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            NotificationChannels.NOTIFICATION_REENGAGEMENT,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_DAILY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NotificationChannels.NOTIFICATION_REENGAGEMENT, notification
            )
        } catch (_: SecurityException) { }
    }
}