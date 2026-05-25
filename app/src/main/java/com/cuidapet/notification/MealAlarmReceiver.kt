package com.cuidadopet.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cuidadopet.R
import com.cuidadopet.data.repository.FeedingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

// BroadcastReceiver que recebe os alarmes de refeição e exibe a notificação.
// Funciona igual ao MedicationAlarmReceiver, mas para lembretes de horário de refeição.
// O Android chama o método onReceive() quando o alarme dispara.
@AndroidEntryPoint
class MealAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var feedingRepository: FeedingRepository
    @Inject lateinit var mealAlarmScheduler: MealAlarmScheduler

    companion object {
        // Chaves dos extras passados pelo AlarmScheduler para este receiver
        const val EXTRA_MEAL_ID   = "meal_id"
        const val EXTRA_PET_NAME  = "pet_name"
        const val EXTRA_TIME      = "time_of_day"  // ex: "07:00"
        const val EXTRA_QUANTITY  = "quantity_grams"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val mealId   = intent.getLongExtra(EXTRA_MEAL_ID, -1L)
        val petName  = intent.getStringExtra(EXTRA_PET_NAME) ?: "seu pet"
        val time     = intent.getStringExtra(EXTRA_TIME) ?: ""
        val quantity = intent.getDoubleExtra(EXTRA_QUANTITY, 0.0)

        if (mealId == -1L) return  // alarme inválido — descarta

        // Intent que abre o app ao tocar na notificação
        val openAppIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context,
            mealId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Monta a notificação com ícone, título e texto
        val quantityText = if (quantity > 0) context.getString(R.string.notif_meal_quantity, quantity.toInt()) else ""
        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_MEALS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_meal_title, petName))
            .setContentText(context.getString(R.string.notif_meal_body, time, quantityText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Exibe a notificação — usa o mealId como ID único para não sobrescrever outras
        with(NotificationManagerCompat.from(context)) {
            // A permissão POST_NOTIFICATIONS é verificada no Manifest
            try {
                notify(mealId.toInt(), notification)
            } catch (e: SecurityException) {
                // Permissão de notificação negada pelo usuário — silencia o erro
            }
        }

        // ── Reagenda o próximo alarme para amanhã ─────────────────────────────
        // Busca dados atuais do banco — se o plano foi substituído, getMealByIdIfActive
        // retorna null e o alarme não é reagendado (evita fantasmas de planos antigos).
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val meal = feedingRepository.getMealByIdIfActive(mealId)
                if (meal != null) mealAlarmScheduler.scheduleMeal(meal, petName)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
