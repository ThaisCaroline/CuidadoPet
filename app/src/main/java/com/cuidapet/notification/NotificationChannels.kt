package com.cuidadopet.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager

// IDs dos canais de notificação — cada tipo de alerta tem seu próprio canal.
// No Android 8+, o usuário pode silenciar canais individualmente nas configurações.
// Por isso separamos: o tutor pode desligar lembretes de água sem desligar os de remédio.
object NotificationChannels {

    // IDs com sufixo _v2 — força recriação dos canais com as configurações corretas.
    // O Android bloqueia alterações em canais já existentes; mudar o ID é a única
    // forma de garantir que som e importância sejam aplicados em instalações anteriores.
    const val CHANNEL_MEDICATIONS = "medication_reminders_v2"
    const val CHANNEL_MEALS       = "meal_reminders_v2"
    const val CHANNEL_WATER       = "water_reminders_v2"
    const val CHANNEL_DAILY       = "daily_alerts_v2"
    const val CHANNEL_VACCINES    = "vaccine_reminders_v1"
    const val CHANNEL_BIRTHDAYS   = "birthday_reminders_v1"
    const val CHANNEL_SUPER       = "super_reminders_v1"

    // IDs das notificações — usados para atualizar ou cancelar uma notificação específica
    const val NOTIFICATION_BASE_MEDICATION = 1000  // + medicationId para ser único
    const val NOTIFICATION_BASE_MEAL       = 2000
    const val NOTIFICATION_BASE_WATER      = 3000
    const val NOTIFICATION_DAILY_SUMMARY   = 4000
    const val NOTIFICATION_BASE_VACCINE    = 5000  // + vaccineId para ser único
    const val NOTIFICATION_BASE_BIRTHDAY   = 6000  // + petId para ser único
    const val NOTIFICATION_REENGAGEMENT   = 7000
    const val NOTIFICATION_BASE_CARE      = 8000  // + petId para ser único
    const val NOTIFICATION_BASE_SUPER     = 90000 // + id para ser único

    fun createAll(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // Remove canais antigos (v1) — configurações travadas pelo SO seriam ignoradas
        listOf("medication_reminders", "meal_reminders", "water_reminders", "daily_alerts")
            .forEach { manager.deleteNotificationChannel(it) }

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MEDICATIONS,
                context.getString(com.cuidadopet.R.string.notif_channel_medications),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(com.cuidadopet.R.string.notif_channel_medications_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MEALS,
                context.getString(com.cuidadopet.R.string.notif_channel_meals),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(com.cuidadopet.R.string.notif_channel_meals_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_WATER,
                context.getString(com.cuidadopet.R.string.notif_channel_water),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(com.cuidadopet.R.string.notif_channel_water_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY,
                context.getString(com.cuidadopet.R.string.notif_channel_daily),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(com.cuidadopet.R.string.notif_channel_daily_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_VACCINES,
                context.getString(com.cuidadopet.R.string.notif_channel_vaccines),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(com.cuidadopet.R.string.notif_channel_vaccines_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BIRTHDAYS,
                context.getString(com.cuidadopet.R.string.notif_channel_birthdays),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(com.cuidadopet.R.string.notif_channel_birthdays_desc)
            }
        )

        val alarmAudioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SUPER,
                context.getString(com.cuidadopet.R.string.notif_channel_super),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(com.cuidadopet.R.string.notif_channel_super_desc)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmAudioAttributes)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500, 200, 500)
                enableVibration(true)
            }
        )
    }
}
