package com.cuidadopet.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.cuidadopet.data.db.entity.MedicationLogEntity
import com.cuidadopet.data.repository.MedicationRepository
import com.cuidadopet.widget.TodayWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MedicationAdminReceiver : BroadcastReceiver() {

    @Inject lateinit var medicationRepository: MedicationRepository

    companion object {
        const val EXTRA_MEDICATION_ID   = "medication_id"
        const val EXTRA_SCHEDULED_AT    = "scheduled_at"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val scheduledAt  = intent.getLongExtra(EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        val notifId      = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (medicationId == -1L) return

        val idToCancel = if (notifId != -1) notifId
                         else NotificationChannels.NOTIFICATION_BASE_MEDICATION + medicationId.toInt()
        NotificationManagerCompat.from(context).cancel(idToCancel)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                medicationRepository.saveLog(
                    MedicationLogEntity(
                        medicationId = medicationId,
                        scheduledAt  = scheduledAt,
                        registeredAt = System.currentTimeMillis(),
                        status       = "TAKEN"
                    )
                )
                TodayWidget().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}