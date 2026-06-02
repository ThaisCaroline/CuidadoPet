package com.cuidadopet.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cuidadopet.MainActivity
import com.cuidadopet.R
import com.cuidadopet.data.repository.PetRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BirthdayAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var petRepository: PetRepository
    @Inject lateinit var birthdayAlarmScheduler: BirthdayAlarmScheduler

    companion object {
        const val EXTRA_PET_ID   = "pet_id"
        const val EXTRA_PET_NAME = "pet_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val petId   = intent.getLongExtra(EXTRA_PET_ID, -1L)
        val petName = intent.getStringExtra(EXTRA_PET_NAME) ?: return
        if (petId == -1L) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapIntent = PendingIntent.getActivity(
            context,
            NotificationChannels.NOTIFICATION_BASE_BIRTHDAY + petId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_BIRTHDAYS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_birthday_title))
            .setContentText(context.getString(R.string.notif_birthday_body, petName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            NotificationChannels.NOTIFICATION_BASE_BIRTHDAY + petId.toInt(),
            notification
        )

        // Reagenda para o próximo ano
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val pet = petRepository.getPetById(petId).first()
                if (pet != null) {
                    birthdayAlarmScheduler.scheduleBirthday(pet)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}