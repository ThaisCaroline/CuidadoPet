package com.cuidadopet.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.cuidadopet.data.db.entity.MealEntity
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

// Responsável por agendar e cancelar os alarmes de refeição.
// Cada refeição do plano alimentar tem um lembrete diário recorrente.
// A lógica é parecida com AlarmScheduler (medicamentos), mas mais simples —
// refeições têm horário fixo diário, sem frequência variável.
@Singleton
class MealAlarmScheduler @Inject constructor(
    private val context: Context
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Agenda o próximo disparo de uma refeição.
    // O receiver reagenda para o dia seguinte cada vez que o alarme dispara.
    fun scheduleMeal(meal: MealEntity, petName: String, isSuperReminder: Boolean = false) {
        val triggerTime = nextOccurrenceOf(meal.timeOfDay)
        if (triggerTime <= 0L) return  // horário inválido — descarta

        val intent = Intent(context, MealAlarmReceiver::class.java).apply {
            putExtra(MealAlarmReceiver.EXTRA_MEAL_ID,          meal.id)
            putExtra(MealAlarmReceiver.EXTRA_PET_NAME,         petName)
            putExtra(MealAlarmReceiver.EXTRA_TIME,             meal.timeOfDay)
            putExtra(MealAlarmReceiver.EXTRA_QUANTITY,         meal.quantityGrams)
            putExtra(MealAlarmReceiver.EXTRA_IS_SUPER_REMINDER, isSuperReminder)
        }

        // requestCode único por refeição — offset de 10000 para não conflitar com medicamentos
        val requestCode = (10000 + meal.id).toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                        )
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
                    )
                else ->
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    // Cancela o alarme de uma refeição específica.
    // Chamado quando o plano alimentar é alterado ou o pet é deletado.
    fun cancelMeal(mealId: Long) {
        val intent = Intent(context, MealAlarmReceiver::class.java)
        val requestCode = (10000 + mealId).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    // Cancela todos os alarmes de um pet — usado quando o plano é substituído.
    // Como não temos o ID das refeições antigas aqui, cancelamos por petId (futuro: persistir IDs).
    // Por ora, o setMealPlan no repositório chama scheduleMeal nas novas refeições,
    // sobrescrevendo os PendingIntents pelo FLAG_UPDATE_CURRENT.
    fun cancelAllForPet(petId: Long) {
        // Estratégia simplificada: não temos os IDs das refeições antigas sem consultar o banco.
        // O FLAG_UPDATE_CURRENT no scheduleMeal() já atualiza os alarmes existentes.
        // Um cancelamento completo será implementado quando adicionarmos o BootReceiver (Sprint 8).
    }

    // Calcula o próximo timestamp para um horário fixo como "07:00".
    // Se o horário já passou hoje, retorna o mesmo horário de amanhã.
    private fun nextOccurrenceOf(timeString: String): Long {
        val parts = timeString.split(":")
        if (parts.size != 2) return 0L

        val hour   = parts[0].toIntOrNull() ?: return 0L
        val minute = parts[1].toIntOrNull() ?: return 0L

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }
}
