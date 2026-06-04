package com.cuidadopet.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cuidadopet.MainActivity
import com.cuidadopet.R

// Receiver responsável pelo snooze das notificações push padrão (não-Super Lembrete).
// Dois estágios:
//   ACTION_SNOOZE → cancela a notificação atual e agenda um alarme para +10 minutos
//   ACTION_SHOW   → recebe o alarme e exibe novamente a notificação com os mesmos dados
class PushSnoozeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE = "com.cuidadopet.ACTION_PUSH_SNOOZE"
        const val ACTION_SHOW   = "com.cuidadopet.ACTION_PUSH_SHOW"

        const val EXTRA_TYPE         = "push_type"
        const val EXTRA_NOTIF_ID     = "push_notif_id"
        const val EXTRA_PET_NAME     = "push_pet_name"
        const val EXTRA_SCHEDULED_AT = "push_scheduled_at"

        // Medicamento
        const val EXTRA_MEDICATION_ID   = "push_medication_id"
        const val EXTRA_MEDICATION_NAME = "push_medication_name"
        const val EXTRA_DOSE            = "push_dose"
        const val EXTRA_DOSE_UNIT       = "push_dose_unit"

        // Refeição
        const val EXTRA_TIME     = "push_time"
        const val EXTRA_QUANTITY = "push_quantity"

        // Água
        const val EXTRA_PET_ID    = "push_pet_id"
        const val EXTRA_TARGET_ML = "push_target_ml"

        const val TYPE_MEDICATION = "MEDICATION"
        const val TYPE_MEAL       = "MEAL"
        const val TYPE_WATER      = "WATER"

        // Cria o PendingIntent de snooze para ser adicionado como ação na notificação.
        // snoozeData deve conter todos os extras necessários para reconstruir a notificação.
        fun snoozePendingIntent(context: Context, notifId: Int, snoozeData: Intent): PendingIntent {
            val intent = Intent(context, PushSnoozeReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtras(snoozeData)
                putExtra(EXTRA_NOTIF_ID, notifId)
            }
            return PendingIntent.getBroadcast(
                context,
                70000 + notifId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SNOOZE -> handleSnooze(context, intent)
            ACTION_SHOW   -> handleShow(context, intent)
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
        if (notifId != -1) {
            try { NotificationManagerCompat.from(context).cancel(notifId) } catch (_: Exception) {}
        }

        val showIntent = Intent(context, PushSnoozeReceiver::class.java).apply {
            action = ACTION_SHOW
            putExtras(intent)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            75000 + notifId,
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

    private fun handleShow(context: Context, intent: Intent) {
        val type    = intent.getStringExtra(EXTRA_TYPE) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1).takeIf { it != -1 } ?: return
        val petName = intent.getStringExtra(EXTRA_PET_NAME) ?: "seu pet"
        when (type) {
            TYPE_MEDICATION -> showMedication(context, intent, notifId, petName)
            TYPE_MEAL       -> showMeal(context, intent, notifId, petName)
            TYPE_WATER      -> showWater(context, intent, notifId, petName)
        }
    }

    private fun showMedication(context: Context, intent: Intent, notifId: Int, petName: String) {
        val medicationId   = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: ""
        val dose           = intent.getStringExtra(EXTRA_DOSE) ?: ""
        val doseUnit       = intent.getStringExtra(EXTRA_DOSE_UNIT) ?: ""
        val scheduledAt    = intent.getLongExtra(EXTRA_SCHEDULED_AT, System.currentTimeMillis())

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPending = PendingIntent.getActivity(
            context, notifId, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val adminIntent = Intent(context, MedicationAdminReceiver::class.java).apply {
            putExtra(MedicationAdminReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(MedicationAdminReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(MedicationAdminReceiver.EXTRA_NOTIFICATION_ID, notifId)
        }
        val adminPending = PendingIntent.getBroadcast(
            context, (medicationId + 20000L).toInt(), adminIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeData = Intent().apply {
            putExtra(EXTRA_TYPE, TYPE_MEDICATION)
            putExtra(EXTRA_PET_NAME, petName)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_DOSE, dose)
            putExtra(EXTRA_DOSE_UNIT, doseUnit)
            putExtra(EXTRA_SCHEDULED_AT, scheduledAt)
        }
        val snoozePending = snoozePendingIntent(context, notifId, snoozeData)

        val doseText = if (dose.isNotBlank()) " • $dose $doseUnit" else ""
        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_MEDICATIONS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_medication_title))
            .setContentText("$petName — $medicationName$doseText")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(contentPending)
            .addAction(0, context.getString(R.string.action_administered), adminPending)
            .addAction(0, context.getString(R.string.super_reminder_snooze), snoozePending)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try { notify(notifId, notification) } catch (_: SecurityException) {}
        }
    }

    private fun showMeal(context: Context, intent: Intent, notifId: Int, petName: String) {
        val time     = intent.getStringExtra(EXTRA_TIME) ?: ""
        val quantity = intent.getDoubleExtra(EXTRA_QUANTITY, 0.0)

        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentPending = PendingIntent.getActivity(
            context, notifId, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeData = Intent().apply {
            putExtra(EXTRA_TYPE, TYPE_MEAL)
            putExtra(EXTRA_PET_NAME, petName)
            putExtra(EXTRA_TIME, time)
            putExtra(EXTRA_QUANTITY, quantity)
        }
        val snoozePending = snoozePendingIntent(context, notifId, snoozeData)

        val quantityText = if (quantity > 0) context.getString(R.string.notif_meal_quantity, quantity.toInt()) else ""
        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_MEALS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_meal_title, petName))
            .setContentText(context.getString(R.string.notif_meal_body, time, quantityText))
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

    private fun showWater(context: Context, intent: Intent, notifId: Int, petName: String) {
        val targetMl = intent.getDoubleExtra(EXTRA_TARGET_ML, 0.0)
        val petId    = intent.getLongExtra(EXTRA_PET_ID, -1L)

        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentPending = PendingIntent.getActivity(
            context, notifId, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeData = Intent().apply {
            putExtra(EXTRA_TYPE, TYPE_WATER)
            putExtra(EXTRA_PET_NAME, petName)
            putExtra(EXTRA_PET_ID, petId)
            putExtra(EXTRA_TARGET_ML, targetMl)
        }
        val snoozePending = snoozePendingIntent(context, notifId, snoozeData)

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