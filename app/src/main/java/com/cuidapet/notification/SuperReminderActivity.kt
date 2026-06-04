package com.cuidadopet.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.glance.appwidget.updateAll
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MedicationLogEntity
import com.cuidadopet.data.db.entity.WaterLogEntity
import com.cuidadopet.data.repository.FeedingRepository
import com.cuidadopet.data.repository.MedicationRepository
import com.cuidadopet.data.repository.WaterRepository
import com.cuidadopet.widget.TodayWidget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.cuidadopet.R
import com.cuidadopet.ui.theme.CuidadoPetTheme
import java.util.Calendar

@AndroidEntryPoint
class SuperReminderActivity : ComponentActivity() {

    @Inject lateinit var medicationRepository: MedicationRepository
    @Inject lateinit var waterRepository: WaterRepository
    @Inject lateinit var feedingRepository: FeedingRepository

    companion object {
        const val EXTRA_TYPE            = "super_type"
        const val EXTRA_ID              = "super_id"
        const val EXTRA_NOTIFICATION_ID = "super_notification_id"
        const val EXTRA_PET_NAME        = "super_pet_name"
        const val EXTRA_LABEL           = "super_label"
        const val EXTRA_DOSE            = "super_dose"
        const val EXTRA_SCHEDULED_AT    = "super_scheduled_at"
        const val EXTRA_AMOUNT          = "super_amount"

        const val TYPE_MEDICATION = "MEDICATION"
        const val TYPE_WATER      = "WATER"
        const val TYPE_MEAL       = "MEAL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        super.onCreate(savedInstanceState)

        val type        = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_MEDICATION
        val id          = intent.getLongExtra(EXTRA_ID, -1L)
        val notifId     = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val petName     = intent.getStringExtra(EXTRA_PET_NAME) ?: ""
        val label       = intent.getStringExtra(EXTRA_LABEL) ?: ""
        val dose        = intent.getStringExtra(EXTRA_DOSE) ?: ""
        val scheduledAt = intent.getLongExtra(EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        val amount      = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)

        setContent {
            CuidadoPetTheme {
                SuperReminderScreen(
                    type     = type,
                    petName  = petName,
                    label    = label,
                    dose     = dose,
                    onAdministered = {
                        cancelNotif(notifId)
                        if (id != -1L) {
                            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                                try {
                                    when (type) {
                                        TYPE_MEDICATION -> {
                                            medicationRepository.saveLog(
                                                MedicationLogEntity(
                                                    medicationId = id,
                                                    scheduledAt  = scheduledAt,
                                                    registeredAt = System.currentTimeMillis(),
                                                    status       = "TAKEN"
                                                )
                                            )
                                            TodayWidget().updateAll(applicationContext)
                                        }
                                        TYPE_WATER -> {
                                            waterRepository.addWaterLog(
                                                WaterLogEntity(
                                                    petId        = id,
                                                    amountMl     = amount,
                                                    registeredAt = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                        TYPE_MEAL -> {
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
                                } catch (_: Exception) {}
                            }
                        }
                        finish()
                    },
                    onSnooze = {
                        cancelNotif(notifId)
                        scheduleSnooze(type, id, notifId, petName, label, dose, scheduledAt, amount)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun cancelNotif(notifId: Int) {
        if (notifId != -1) NotificationManagerCompat.from(this).cancel(notifId)
    }

    private fun scheduleSnooze(
        type: String, id: Long, notifId: Int,
        petName: String, label: String, dose: String, scheduledAt: Long, amount: Double
    ) {
        val snoozeIntent = Intent(this, SuperReminderSnoozeReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
            putExtra(EXTRA_PET_NAME, petName)
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_DOSE, dose)
            putExtra(EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(EXTRA_AMOUNT, amount)
        }
        val pending = PendingIntent.getBroadcast(
            this,
            50000 + notifId,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }
}

@Composable
private fun SuperReminderScreen(
    type: String,
    petName: String,
    label: String,
    dose: String,
    onAdministered: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape     = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier              = Modifier.padding(24.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
                verticalArrangement   = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "🔔", fontSize = 48.sp, textAlign = TextAlign.Center)

                if (petName.isNotBlank()) {
                    Text(
                        text  = petName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text        = label,
                    style       = MaterialTheme.typography.headlineSmall,
                    fontWeight  = FontWeight.Bold,
                    textAlign   = TextAlign.Center
                )

                if (dose.isNotBlank()) {
                    Text(
                        text      = dose,
                        style     = MaterialTheme.typography.bodyLarge,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(4.dp))

                val administeredLabel = when (type) {
                    SuperReminderActivity.TYPE_WATER -> stringResource(R.string.super_reminder_water_given)
                    SuperReminderActivity.TYPE_MEAL  -> stringResource(R.string.super_reminder_meal_given)
                    else                             -> stringResource(R.string.super_reminder_administered)
                }
                Button(
                    onClick  = onAdministered,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(administeredLabel) }

                OutlinedButton(
                    onClick  = onSnooze,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.super_reminder_snooze)) }

                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.action_dismiss)) }
            }
        }
    }
}
