package com.cuidadopet.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cuidadopet.R

class SuperReminderSnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type        = intent.getStringExtra(SuperReminderActivity.EXTRA_TYPE) ?: return
        val id          = intent.getLongExtra(SuperReminderActivity.EXTRA_ID, -1L)
        val notifId     = intent.getIntExtra(SuperReminderActivity.EXTRA_NOTIFICATION_ID, -1)
        val petName     = intent.getStringExtra(SuperReminderActivity.EXTRA_PET_NAME) ?: "seu pet"
        val label       = intent.getStringExtra(SuperReminderActivity.EXTRA_LABEL) ?: ""
        val dose        = intent.getStringExtra(SuperReminderActivity.EXTRA_DOSE) ?: ""
        val scheduledAt = intent.getLongExtra(SuperReminderActivity.EXTRA_SCHEDULED_AT, System.currentTimeMillis())

        showSuperReminderNotification(context, type, id, notifId, petName, label, dose, scheduledAt)
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
    scheduledAt: Long
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

    if (type == SuperReminderActivity.TYPE_MEDICATION && id != -1L) {
        val adminIntent = Intent(context, MedicationAdminReceiver::class.java).apply {
            putExtra(MedicationAdminReceiver.EXTRA_MEDICATION_ID, id)
            putExtra(MedicationAdminReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        val adminPending = PendingIntent.getBroadcast(
            context,
            (id + 20000L).toInt(),
            adminIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, context.getString(R.string.action_administered), adminPending)
    }

    NotificationManagerCompat.from(context).apply {
        try { notify(notifId, builder.build()) } catch (_: SecurityException) {}
    }
}
