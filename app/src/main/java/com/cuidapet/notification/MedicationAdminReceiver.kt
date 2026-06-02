package com.cuidadopet.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.cuidadopet.data.db.entity.MedicationLogEntity
import com.cuidadopet.data.repository.MedicationRepository
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
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_SCHEDULED_AT  = "scheduled_at"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val scheduledAt  = intent.getLongExtra(EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        if (medicationId == -1L) return

        NotificationManagerCompat.from(context).cancel(
            NotificationChannels.NOTIFICATION_BASE_MEDICATION + medicationId.toInt()
        )

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
            } finally {
                pendingResult.finish()
            }
        }
    }
}