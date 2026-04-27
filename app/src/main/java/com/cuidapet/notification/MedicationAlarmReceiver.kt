package com.cuidadopet.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cuidadopet.MainActivity
import com.cuidadopet.R

// BroadcastReceiver é um componente Android que recebe "broadcasts" — mensagens do sistema.
// Quando o AlarmManager dispara um alarme, ele envia um broadcast.
// Este receiver escuta esse broadcast e exibe a notificação de medicamento.
//
// Funciona mesmo com o app fechado — o Android acorda o processo só para executar onReceive().
class MedicationAlarmReceiver : BroadcastReceiver() {

    companion object {
        // Chaves para os extras passados no Intent do alarme
        const val EXTRA_MEDICATION_ID   = "medication_id"
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_PET_NAME        = "pet_name"
        const val EXTRA_DOSE            = "dose"
        const val EXTRA_DOSE_UNIT       = "dose_unit"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Recupera os dados que foram passados quando o alarme foi agendado
        val medicationId   = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medicamento"
        val petName        = intent.getStringExtra(EXTRA_PET_NAME) ?: "seu pet"
        val dose           = intent.getStringExtra(EXTRA_DOSE) ?: ""
        val doseUnit       = intent.getStringExtra(EXTRA_DOSE_UNIT) ?: ""

        if (medicationId == -1L) return  // alarme inválido, ignora

        // Intent que abre o app quando o tutor toca na notificação
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // PendingIntent "empacota" o Intent para ser executado mais tarde
        // FLAG_IMMUTABLE = obrigatório no Android 12+ por segurança
        val pendingIntent = PendingIntent.getActivity(
            context,
            medicationId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Monta o texto da notificação
        // Ex: "Rex — Amoxicilina • 1 comprimido"
        val doseText = if (dose.isNotBlank()) " • $dose $doseUnit" else ""
        val contentText = "$petName — $medicationName$doseText"

        // Constrói a notificação usando o canal de medicamentos (alta importância)
        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_MEDICATIONS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Hora do medicamento!")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)  // som + vibração em Android < 8
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Exibe a notificação
        // O id único garante que cada medicamento tem sua própria notificação
        // (não sobrescreve a de outro medicamento)
        NotificationManagerCompat.from(context).notify(
            NotificationChannels.NOTIFICATION_BASE_MEDICATION + medicationId.toInt(),
            notification
        )
    }
}
