package com.cuidadopet.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cuidadopet.R
import com.cuidadopet.data.repository.PetRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class ReEngagementReceiver : BroadcastReceiver() {

    @Inject lateinit var petRepository: PetRepository

    companion object {
        private const val REQUEST_CODE = 9001
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000L

        fun schedule(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 19)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            val pi = makePendingIntent(context)
            try {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        if (am.canScheduleExactAlarms())
                            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
                        else
                            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
                    else ->
                        am.set(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
                }
            } catch (_: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
            }
        }

        private fun makePendingIntent(context: Context) = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReEngagementReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        schedule(context)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                doCheck(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun doCheck(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastOpen = prefs.getLong("last_app_open", 0L)
        val stage = prefs.getInt("reengagement_stage", 0)

        if (stage >= 3 || lastOpen == 0L) return

        val daysSinceOpen = (System.currentTimeMillis() - lastOpen) / DAY_MILLIS

        val nextStage = when {
            stage == 0 && daysSinceOpen >= 7  -> 1
            stage == 1 && daysSinceOpen >= 11 -> 2
            stage == 2 && daysSinceOpen >= 15 -> 3
            else -> return
        }

        val pets = petRepository.getAllPets().first()
        val petsName = buildPetsName(context, pets.map { it.name })

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

        showNotification(context, context.getString(titleRes), context.getString(bodyRes, petsName))
        prefs.edit().putInt("reengagement_stage", nextStage).apply()
    }

    private fun buildPetsName(context: Context, names: List<String>): String = when (names.size) {
        0    -> context.getString(R.string.reengagement_pets_fallback)
        1    -> names[0]
        2    -> context.getString(R.string.reengagement_pets_two, names[0], names[1])
        else -> context.getString(R.string.reengagement_pets_multiple, names[0])
    }

    private fun showNotification(context: Context, title: String, body: String) {
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
