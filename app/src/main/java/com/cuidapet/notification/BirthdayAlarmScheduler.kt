package com.cuidadopet.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.cuidadopet.data.db.entity.PetEntity
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BirthdayAlarmScheduler @Inject constructor(
    private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleBirthday(pet: PetEntity) {
        val birthDate = pet.birthDate ?: return
        val triggerTime = nextBirthdayMillis(birthDate)

        val intent = Intent(context, BirthdayAlarmReceiver::class.java).apply {
            putExtra(BirthdayAlarmReceiver.EXTRA_PET_ID, pet.id)
            putExtra(BirthdayAlarmReceiver.EXTRA_PET_NAME, pet.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationChannels.NOTIFICATION_BASE_BIRTHDAY + pet.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            else ->
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelBirthday(petId: Long) {
        val intent = Intent(context, BirthdayAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationChannels.NOTIFICATION_BASE_BIRTHDAY + petId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    // Returns the next birthday at 09:00; if today's birthday has already passed 09:00, returns next year.
    fun nextBirthdayMillis(birthDateMillis: Long): Long {
        val now = Calendar.getInstance()
        val birth = Calendar.getInstance().apply { timeInMillis = birthDateMillis }

        val next = Calendar.getInstance().apply {
            set(Calendar.MONTH, birth.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, birth.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!next.after(now)) {
            next.add(Calendar.YEAR, 1)
        }

        return next.timeInMillis
    }
}