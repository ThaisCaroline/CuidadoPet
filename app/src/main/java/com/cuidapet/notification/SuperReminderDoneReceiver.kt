package com.cuidadopet.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MedicationLogEntity
import com.cuidadopet.data.repository.FeedingRepository
import com.cuidadopet.data.repository.MedicationRepository
import com.cuidadopet.widget.TodayWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class SuperReminderDoneReceiver : BroadcastReceiver() {

    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var feedingRepository: FeedingRepository

    override fun onReceive(context: Context, intent: Intent) {
        val type        = intent.getStringExtra(SuperReminderActivity.EXTRA_TYPE) ?: return
        val id          = intent.getLongExtra(SuperReminderActivity.EXTRA_ID, -1L)
        val notifId     = intent.getIntExtra(SuperReminderActivity.EXTRA_NOTIFICATION_ID, -1)
        val scheduledAt = intent.getLongExtra(SuperReminderActivity.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        val amount      = intent.getDoubleExtra(SuperReminderActivity.EXTRA_AMOUNT, 0.0)

        if (notifId != -1) {
            try { NotificationManagerCompat.from(context).cancel(notifId) } catch (_: Exception) {}
        }
        if (id == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (type) {
                    SuperReminderActivity.TYPE_MEDICATION -> {
                        medicationRepository.saveLog(
                            MedicationLogEntity(
                                medicationId = id,
                                scheduledAt  = scheduledAt,
                                registeredAt = System.currentTimeMillis(),
                                status       = "TAKEN"
                            )
                        )
                        TodayWidget().updateAll(context)
                    }
                    SuperReminderActivity.TYPE_WATER -> {
                        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            .edit().putLong("open_today_pet_id", id).apply()
                        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                            it.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                            )
                            context.startActivity(it)
                        }
                    }
                    SuperReminderActivity.TYPE_MEAL -> {
                        val midnight = Calendar.getInstance().apply {
                            timeInMillis = scheduledAt
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        feedingRepository.saveMealLog(
                            MealLogEntity(
                                mealId          = id,
                                date            = midnight,
                                eatenPercentage = 100,
                                appetiteStatus  = "ALL"
                            )
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
