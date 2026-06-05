package com.cuidadopet.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cuidadopet.R
import com.cuidadopet.data.repository.FeedingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// BroadcastReceiver que recebe os alarmes de refeição e exibe a notificação.
// Funciona igual ao MedicationAlarmReceiver, mas para lembretes de horário de refeição.
// O Android chama o método onReceive() quando o alarme dispara.
@AndroidEntryPoint
class MealAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var feedingRepository: FeedingRepository
    @Inject lateinit var mealAlarmScheduler: MealAlarmScheduler

    companion object {
        // Chaves dos extras passados pelo AlarmScheduler para este receiver
        const val EXTRA_MEAL_ID           = "meal_id"
        const val EXTRA_PET_NAME          = "pet_name"
        const val EXTRA_TIME              = "time_of_day"  // ex: "07:00"
        const val EXTRA_QUANTITY          = "quantity_grams"
        const val EXTRA_IS_SUPER_REMINDER = "is_super_reminder"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val mealId          = intent.getLongExtra(EXTRA_MEAL_ID, -1L)
        val petName         = intent.getStringExtra(EXTRA_PET_NAME) ?: "seu pet"
        val time            = intent.getStringExtra(EXTRA_TIME) ?: ""
        val quantity        = intent.getDoubleExtra(EXTRA_QUANTITY, 0.0)
        val isSuperReminder = intent.getBooleanExtra(EXTRA_IS_SUPER_REMINDER, false)

        if (mealId == -1L) return  // alarme inválido — descarta

        val notifId = NotificationChannels.NOTIFICATION_BASE_MEAL + mealId.toInt()
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val todayMidnight = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val existingLog = feedingRepository.getLogForMealOnDateOnce(mealId, todayMidnight)

                if (existingLog == null) {
                    if (isSuperReminder) {
                        val quantityText = if (quantity > 0) "${quantity.toInt()}g" else ""
                        showSuperReminderNotification(
                            context     = context,
                            type        = SuperReminderActivity.TYPE_MEAL,
                            id          = mealId,
                            notifId     = NotificationChannels.NOTIFICATION_BASE_SUPER + mealId.toInt() + 10000,
                            petName     = petName,
                            label       = context.getString(R.string.notif_meal_title, petName),
                            dose        = if (quantityText.isNotBlank()) "$time • $quantityText" else time,
                            scheduledAt = System.currentTimeMillis(),
                            amount      = quantity
                        )
                    } else {
                        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        val pendingIntent = PendingIntent.getActivity(
                            context, mealId.toInt(), openAppIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        val quantityText = if (quantity > 0) context.getString(R.string.notif_meal_quantity, quantity.toInt()) else ""
                        val snoozeData = Intent().apply {
                            putExtra(PushSnoozeReceiver.EXTRA_TYPE, PushSnoozeReceiver.TYPE_MEAL)
                            putExtra(PushSnoozeReceiver.EXTRA_PET_NAME, petName)
                            putExtra(PushSnoozeReceiver.EXTRA_TIME, time)
                            putExtra(PushSnoozeReceiver.EXTRA_QUANTITY, quantity)
                        }
                        val snoozePending = PushSnoozeReceiver.snoozePendingIntent(context, notifId, snoozeData)
                        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_MEALS)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(context.getString(R.string.notif_meal_title, petName))
                            .setContentText(context.getString(R.string.notif_meal_body, time, quantityText))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setDefaults(NotificationCompat.DEFAULT_ALL)
                            .setContentIntent(pendingIntent)
                            .addAction(0, context.getString(R.string.super_reminder_snooze), snoozePending)
                            .setAutoCancel(true)
                            .build()
                        with(NotificationManagerCompat.from(context)) {
                            try { notify(notifId, notification) } catch (_: SecurityException) {}
                        }
                    }
                }

                // Reagenda o próximo alarme independente de ter disparado o lembrete.
                // Busca dados atuais do banco — se o plano foi substituído, getMealByIdIfActive
                // retorna null e o alarme não é reagendado (evita fantasmas de planos antigos).
                val meal = feedingRepository.getMealByIdIfActive(mealId)
                if (meal != null) mealAlarmScheduler.scheduleMeal(meal, petName)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
