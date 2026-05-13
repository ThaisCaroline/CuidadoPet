package com.cuidadopet.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

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

    // IDs das notificações — usados para atualizar ou cancelar uma notificação específica
    const val NOTIFICATION_BASE_MEDICATION = 1000  // + medicationId para ser único
    const val NOTIFICATION_BASE_MEAL       = 2000
    const val NOTIFICATION_BASE_WATER      = 3000
    const val NOTIFICATION_DAILY_SUMMARY   = 4000
    const val NOTIFICATION_BASE_VACCINE    = 5000  // + vaccineId para ser único

    fun createAll(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // Remove canais antigos (v1) — configurações travadas pelo SO seriam ignoradas
        listOf("medication_reminders", "meal_reminders", "water_reminders", "daily_alerts")
            .forEach { manager.deleteNotificationChannel(it) }

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MEDICATIONS,
                "Lembretes de medicamentos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de horário dos medicamentos do seu pet"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MEALS,
                "Lembretes de alimentação",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lembretes dos horários de refeição do seu pet"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_WATER,
                "Lembretes de hidratação",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lembretes para oferecer água ao seu pet"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY,
                "Alertas diários",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Resumo diário de alimentação e hidratação"
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_VACCINES,
                "Lembretes de vacinas e vermífugos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos de vacinas e vermífugos vencendo"
            }
        )
    }
}
