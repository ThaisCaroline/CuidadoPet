package com.cuidadopet.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.appwidget.updateAll
import com.cuidadopet.data.db.entity.MedicationEntity
import com.cuidadopet.widget.TodayWidget
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// AlarmScheduler é responsável por agendar e cancelar os alarmes de medicamento.
// Usa o AlarmManager do Android — o componente que dispara ações em horários específicos.
@Singleton
class AlarmScheduler @Inject constructor(
    private val context: Context
) {

    // Obtém o AlarmManager do sistema
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Agenda todos os alarmes de um medicamento para as próximas 24 horas.
    // Chamado quando: medicamento é cadastrado, editado, ou no boot do dispositivo.
    fun scheduleMedication(
        medication: MedicationEntity,
        petName: String
    ) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { TodayWidget().updateAll(context) } catch (_: Exception) {}
        }

        if (!medication.isActive || !medication.reminderEnabled) return

        val triggerTimes = calculateNextTriggerTimes(medication)

        triggerTimes.forEachIndexed { index, triggerTime ->
            scheduleAlarm(
                medication = medication,
                petName = petName,
                triggerAtMillis = triggerTime,
                alarmIndex = index
            )
        }
    }

    // Cancela todos os alarmes de um medicamento
    // Chamado quando: medicamento é desativado, tratamento encerrado, ou pet deletado
    fun cancelMedication(medicationId: Long) {
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
        // Cancela requestCode antigo (instalações anteriores à correção do índice)
        PendingIntent.getBroadcast(context, medicationId.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?.let { alarmManager.cancel(it) }
        // Cancela todos os slots indexados (até 10 horários fixos por medicamento)
        for (index in 0 until 10) {
            PendingIntent.getBroadcast(context, (medicationId * 100 + index).toInt(), intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                ?.let { alarmManager.cancel(it) }
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { TodayWidget().updateAll(context) } catch (_: Exception) {}
        }
    }

    // Calcula os próximos horários de disparo com base na frequência do medicamento
    private fun calculateNextTriggerTimes(medication: MedicationEntity): List<Long> {
        val now = System.currentTimeMillis()
        val times = mutableListOf<Long>()

        when (medication.frequencyType) {

            // INTERVAL: a cada X horas — calcula o próximo horário a partir de agora
            "INTERVAL" -> {
                val intervalMs = (medication.frequencyHours ?: 8) * 60 * 60 * 1000L
                // Trunca para o minuto exato, eliminando segundos/ms do momento do cadastro
                val startTruncated = (medication.startDate / 60_000L) * 60_000L
                var nextTime = startTruncated
                while (nextTime <= now) {
                    nextTime += intervalMs
                }
                times.add(nextTime)
            }

            // FIXED_TIMES: horários fixos do dia — agenda o próximo de cada horário
            // Ex: ["08:00","20:00"] → agenda os próximos 08:00 e 20:00 futuros
            "FIXED_TIMES" -> {
                val fixedTimes = parseFixedTimes(medication.fixedTimes ?: "[]")
                fixedTimes.forEach { timeString ->
                    val triggerTime = nextOccurrenceOf(timeString)
                    if (triggerTime > now) {
                        times.add(triggerTime)
                    }
                }
            }
        }

        return times
    }

    // Agenda um único alarme
    private fun scheduleAlarm(
        medication: MedicationEntity,
        petName: String,
        triggerAtMillis: Long,
        alarmIndex: Int = 0
    ) {
        // Monta o Intent com todos os dados que o receiver vai precisar
        // Os campos de frequência são necessários para que o receiver reagende o próximo alarme
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_ID,    medication.id)
            putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_NAME,  medication.name)
            putExtra(MedicationAlarmReceiver.EXTRA_PET_NAME,         petName)
            putExtra(MedicationAlarmReceiver.EXTRA_DOSE,             medication.dose)
            putExtra(MedicationAlarmReceiver.EXTRA_DOSE_UNIT,        medication.doseUnit)
            putExtra(MedicationAlarmReceiver.EXTRA_PET_ID,           medication.petId)
            putExtra(MedicationAlarmReceiver.EXTRA_FORM,             medication.form)
            putExtra(MedicationAlarmReceiver.EXTRA_FREQUENCY_TYPE,   medication.frequencyType)
            putExtra(MedicationAlarmReceiver.EXTRA_FREQUENCY_HOURS,  medication.frequencyHours ?: -1)
            putExtra(MedicationAlarmReceiver.EXTRA_FIXED_TIMES,      medication.fixedTimes ?: "")
            putExtra(MedicationAlarmReceiver.EXTRA_START_DATE,       medication.startDate)
            putExtra(MedicationAlarmReceiver.EXTRA_END_DATE,         medication.endDate ?: -1L)
            putExtra(MedicationAlarmReceiver.EXTRA_IS_CONTINUOUS,    medication.isContinuous)
            putExtra(MedicationAlarmReceiver.EXTRA_REMINDER_ENABLED, medication.reminderEnabled)
            putExtra(MedicationAlarmReceiver.EXTRA_IS_SUPER_REMINDER,medication.isSuperReminder)
            putExtra(MedicationAlarmReceiver.EXTRA_SCHEDULED_AT,     triggerAtMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            // requestCode único por medicamento + slot — evita sobrescrever alarmes do mesmo med
            (medication.id * 100 + alarmIndex).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                        )
                    } else {
                        // Permissão de alarme exato não concedida — usa alarme inexato
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                else ->
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    // Converte '["08:00","20:00"]' para listOf("08:00", "20:00")
    private fun parseFixedTimes(json: String): List<String> {
        return json.removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }


    // Calcula o próximo timestamp futuro para um horário fixo como "08:00"
    // Se "08:00" já passou hoje, retorna o "08:00" de amanhã
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

        // Se o horário de hoje já passou, agenda para amanhã
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }
}
