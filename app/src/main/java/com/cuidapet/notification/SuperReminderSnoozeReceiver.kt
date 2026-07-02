package com.cuidadopet.notification

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cuidadopet.MainActivity
import com.cuidadopet.R

class SuperReminderSnoozeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE  = "com.cuidadopet.ACTION_SUPER_SNOOZE"
        const val ACTION_DISMISS = "com.cuidadopet.ACTION_SUPER_DISMISS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(SuperReminderActivity.EXTRA_NOTIFICATION_ID, -1)

        if (intent.action == ACTION_DISMISS) {
            if (notifId != -1) {
                try { NotificationManagerCompat.from(context).cancel(notifId) } catch (_: Exception) {}
            }
            return
        }

        val type        = intent.getStringExtra(SuperReminderActivity.EXTRA_TYPE) ?: return
        val id          = intent.getLongExtra(SuperReminderActivity.EXTRA_ID, -1L)
        val petName     = intent.getStringExtra(SuperReminderActivity.EXTRA_PET_NAME) ?: "seu pet"
        val label       = intent.getStringExtra(SuperReminderActivity.EXTRA_LABEL) ?: ""
        val dose        = intent.getStringExtra(SuperReminderActivity.EXTRA_DOSE) ?: ""
        val scheduledAt = intent.getLongExtra(SuperReminderActivity.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        val amount      = intent.getDoubleExtra(SuperReminderActivity.EXTRA_AMOUNT, 0.0)

        if (intent.action == ACTION_SNOOZE) {
            if (notifId != -1) {
                try { NotificationManagerCompat.from(context).cancel(notifId) } catch (_: Exception) {}
            }
            scheduleShowAlarm(context, type, id, notifId, petName, label, dose, scheduledAt, amount)
        } else {
            showSuperReminderNotification(context, type, id, notifId, petName, label, dose, scheduledAt, amount)
        }
    }

    private fun scheduleShowAlarm(
        context: Context, type: String, id: Long, notifId: Int,
        petName: String, label: String, dose: String, scheduledAt: Long, amount: Double
    ) {
        val showIntent = Intent(context, SuperReminderSnoozeReceiver::class.java).apply {
            putExtra(SuperReminderActivity.EXTRA_TYPE, type)
            putExtra(SuperReminderActivity.EXTRA_ID, id)
            putExtra(SuperReminderActivity.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(SuperReminderActivity.EXTRA_PET_NAME, petName)
            putExtra(SuperReminderActivity.EXTRA_LABEL, label)
            putExtra(SuperReminderActivity.EXTRA_DOSE, dose)
            putExtra(SuperReminderActivity.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(SuperReminderActivity.EXTRA_AMOUNT, amount)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            50000 + notifId,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + 10 * 60 * 1000L
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    if (alarmManager.canScheduleExactAlarms())
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                    else alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                else -> alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }
}

fun showSuperReminderNotification(
    context: Context,
    type: String,
    id: Long,
    notifId: Int,
    petName: String,
    label: String,
    dose: String,
    scheduledAt: Long,
    amount: Double = 0.0
) {
    val activityIntent = Intent(context, SuperReminderActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(SuperReminderActivity.EXTRA_TYPE, type)
        putExtra(SuperReminderActivity.EXTRA_ID, id)
        putExtra(SuperReminderActivity.EXTRA_NOTIFICATION_ID, notifId)
        putExtra(SuperReminderActivity.EXTRA_PET_NAME, petName)
        putExtra(SuperReminderActivity.EXTRA_LABEL, label)
        putExtra(SuperReminderActivity.EXTRA_DOSE, dose)
        putExtra(SuperReminderActivity.EXTRA_SCHEDULED_AT, scheduledAt)
        putExtra(SuperReminderActivity.EXTRA_AMOUNT, amount)
    }
    val fullScreenPending = PendingIntent.getActivity(
        context,
        60000 + notifId,
        activityIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_SUPER)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.super_reminder_notif_title, petName))
        .setContentText(if (dose.isNotBlank()) "$label • $dose" else label)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setFullScreenIntent(fullScreenPending, true)
        .setContentIntent(fullScreenPending)
        .setAutoCancel(false)
        .setOngoing(true)

    val doneLabel = when (type) {
        SuperReminderActivity.TYPE_WATER -> context.getString(R.string.super_reminder_water_ok)
        SuperReminderActivity.TYPE_MEAL  -> context.getString(R.string.super_reminder_meal_given)
        else                             -> context.getString(R.string.super_reminder_administered)
    }
    val donePending = if (type == SuperReminderActivity.TYPE_WATER) {
        val waterIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("open_today_pet_id", id)
        }
        PendingIntent.getActivity(
            context,
            (id + 20000L).toInt(),
            waterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    } else {
        val doneIntent = Intent(context, SuperReminderDoneReceiver::class.java).apply {
            putExtra(SuperReminderActivity.EXTRA_TYPE, type)
            putExtra(SuperReminderActivity.EXTRA_ID, id)
            putExtra(SuperReminderActivity.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(SuperReminderActivity.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(SuperReminderActivity.EXTRA_AMOUNT, amount)
        }
        PendingIntent.getBroadcast(
            context,
            (id + 20000L).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    builder.addAction(0, doneLabel, donePending)

    val snoozeIntent = Intent(context, SuperReminderSnoozeReceiver::class.java).apply {
        action = SuperReminderSnoozeReceiver.ACTION_SNOOZE
        putExtra(SuperReminderActivity.EXTRA_TYPE, type)
        putExtra(SuperReminderActivity.EXTRA_ID, id)
        putExtra(SuperReminderActivity.EXTRA_NOTIFICATION_ID, notifId)
        putExtra(SuperReminderActivity.EXTRA_PET_NAME, petName)
        putExtra(SuperReminderActivity.EXTRA_LABEL, label)
        putExtra(SuperReminderActivity.EXTRA_DOSE, dose)
        putExtra(SuperReminderActivity.EXTRA_SCHEDULED_AT, scheduledAt)
        putExtra(SuperReminderActivity.EXTRA_AMOUNT, amount)
    }
    val snoozePending = PendingIntent.getBroadcast(
        context,
        55000 + notifId,
        snoozeIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    builder.addAction(0, context.getString(R.string.super_reminder_snooze), snoozePending)

    NotificationManagerCompat.from(context).apply {
        try { notify(notifId, builder.build()) } catch (_: SecurityException) {}
    }

    // No Android 14+, USE_FULL_SCREEN_INTENT exige concessão manual pelo usuário.
    // Se a permissão não foi concedida, o fullScreenIntent é ignorado silenciosamente
    // e a notificação cai no drawer. Chamamos startActivity() diretamente, pois
    // apps com SCHEDULE_EXACT_ALARM têm exceção à restrição de background activity start.
    val canUseFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    } else {
        true
    }
    if (!canUseFullScreen) {
        try { context.startActivity(activityIntent) } catch (_: Exception) {}
    }
}
