package com.cuidadopet.notification

import android.app.PendingIntent
import com.cuidadopet.R
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

// Worker do WorkManager responsável por exibir o lembrete periódico de água.
// O WorkManager chama o doWork() automaticamente no intervalo configurado.
//
// Diferença entre AlarmManager (medicamentos) e WorkManager (água):
// - AlarmManager: alarmes exatos, críticos, não podem atrasar.
// - WorkManager: tarefas periódicas, podem atrasar alguns minutos —
//   aceitável para um lembrete de "ofereça água", não para medicamento.
class WaterReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        // Chaves para os dados de entrada passados ao agendar o worker
        const val KEY_PET_NAME  = "pet_name"
        const val KEY_PET_ID    = "pet_id"
        const val KEY_TARGET_ML = "target_ml"

        // Tag única por pet — usada para cancelar os lembretes quando necessário
        fun workTag(petId: Long) = "water_reminder_$petId"
    }

    override suspend fun doWork(): Result {
        val petName  = inputData.getString(KEY_PET_NAME)  ?: "seu pet"
        val petId    = inputData.getLong(KEY_PET_ID, -1L)
        val targetMl = inputData.getDouble(KEY_TARGET_ML, 0.0)

        if (petId == -1L) return Result.failure()

        showWaterReminder(petName, targetMl)

        // Result.success() informa ao WorkManager que a tarefa terminou com êxito
        // O WorkManager vai reagendar automaticamente para a próxima execução periódica
        return Result.success()
    }

    // Exibe a notificação de lembrete de água
    private fun showWaterReminder(petName: String, targetMl: Double) {
        // Intent que abre o app ao tocar na notificação
        val openAppIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val targetText = if (targetMl > 0) " (meta: ${targetMl.toInt()} ml/dia)" else ""

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_WATER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hora de oferecer água para $petName")
            .setContentText("Lembre de hidratar seu pet$targetText")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                // Usa petId como parte do ID para não sobrescrever notificações de pets diferentes
                notify(NotificationChannels.CHANNEL_WATER.hashCode(), notification)
            } catch (e: SecurityException) {
                // Permissão de notificação negada
            }
        }
    }
}
