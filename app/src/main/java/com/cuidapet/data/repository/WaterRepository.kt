package com.cuidadopet.data.repository

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cuidadopet.data.db.dao.WaterDao
import com.cuidadopet.data.db.entity.WaterConfigEntity
import com.cuidadopet.data.db.entity.WaterLogEntity
import com.cuidadopet.notification.WaterReminderWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// Repositório de hidratação — centraliza o acesso ao banco para configuração de água
// e logs de ingestão, além de gerenciar os lembretes periódicos via WorkManager.
@Singleton
class WaterRepository @Inject constructor(
    private val waterDao: WaterDao,
    private val context: Context
) {

    // WorkManager é o componente do Android para tarefas periódicas em segundo plano.
    // Sobrevive a reinicializações do app — ideal para lembretes recorrentes.
    private val workManager = WorkManager.getInstance(context)

    // ─── Configuração ────────────────────────────────────────────────────────

    fun getWaterConfig(petId: Long): Flow<WaterConfigEntity?> =
        waterDao.getWaterConfig(petId)

    // Salva a configuração e atualiza o lembrete do WorkManager conforme as preferências
    suspend fun saveWaterConfig(config: WaterConfigEntity, petName: String) {
        // Reutiliza o id existente para que REPLACE atualize a linha correta
        val existing = waterDao.getWaterConfig(config.petId).first()
        val toSave = if (existing != null) config.copy(id = existing.id) else config

        waterDao.insertWaterConfig(toSave)

        if (config.remindersEnabled) {
            scheduleWaterReminder(config, petName)
        } else {
            cancelWaterReminder(config.petId)
        }
    }

    suspend fun deleteWaterConfig(petId: Long) {
        cancelWaterReminder(petId)
        waterDao.deleteWaterConfig(petId)
    }

    // ─── Logs de ingestão ────────────────────────────────────────────────────

    // Registros de água de um pet em um dia específico
    // dayStart = meia-noite do dia (00:00:00), dayEnd = 23:59:59 do mesmo dia
    fun getLogsForDay(petId: Long, dayStart: Long, dayEnd: Long): Flow<List<WaterLogEntity>> =
        waterDao.getLogsForDay(petId, dayStart, dayEnd)

    // Total em ml bebido no dia — SQL faz a soma direto no banco (mais eficiente)
    fun getTotalForDay(petId: Long, dayStart: Long, dayEnd: Long): Flow<Double> =
        waterDao.getTotalForDay(petId, dayStart, dayEnd)

    // Histórico de um período para o relatório
    fun getLogsForPeriod(
        petId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<WaterLogEntity>> =
        waterDao.getLogsForPeriod(petId, startDate, endDate)

    // Registra o consumo de água — cada toque no botão "+Xml" cria um novo log
    suspend fun addWaterLog(log: WaterLogEntity): Long =
        waterDao.insertLog(log)

    suspend fun updateWaterLog(log: WaterLogEntity) =
        waterDao.updateLog(log)

    // Remove um registro (caso o tutor tenha errado o valor)
    suspend fun deleteWaterLog(logId: Long) =
        waterDao.deleteLog(logId)

    // ─── WorkManager — lembretes periódicos ──────────────────────────────────

    // Agenda um lembrete periódico de água para o pet.
    // O WorkManager garante que o lembrete seja disparado mesmo após o app ser fechado.
    private fun scheduleWaterReminder(config: WaterConfigEntity, petName: String) {
        val inputData = Data.Builder()
            .putString(WaterReminderWorker.KEY_PET_NAME,  petName)
            .putLong(WaterReminderWorker.KEY_PET_ID,      config.petId)
            .putDouble(WaterReminderWorker.KEY_TARGET_ML, config.dailyTargetMl)
            .build()

        val intervalHours = config.reminderIntervalHours.toLong().coerceAtLeast(1L)
        val initialDelay  = calcInitialDelayMs(config.reminderStartTime, intervalHours)

        val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            repeatInterval = intervalHours,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(WaterReminderWorker.workTag(config.petId))
            .build()

        workManager.enqueueUniquePeriodicWork(
            WaterReminderWorker.workTag(config.petId),
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    // Calcula quantos ms faltam até a próxima ocorrência do lembrete.
    // Exemplo: início=13:00, intervalo=2h, agora=14:30 → próxima=15:00 → delay=30min
    private fun calcInitialDelayMs(startTime: String, intervalHours: Long): Long {
        val parts   = startTime.split(":")
        val hour    = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute  = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val now = System.currentTimeMillis()
        val intervalMs = intervalHours * 3_600_000L

        // Âncora = horário de início configurado hoje
        val anchor = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Se o horário já passou, avança pelo número mínimo de intervalos para ficar no futuro
        var next = anchor
        if (next <= now) {
            val elapsed        = now - next
            val intervalsToSkip = (elapsed + intervalMs - 1) / intervalMs  // divisão com teto
            next += intervalsToSkip * intervalMs
        }

        return (next - now).coerceAtLeast(0L)
    }

    // Cancela os lembretes de água de um pet — chamado quando remindersEnabled = false
    fun cancelWaterReminder(petId: Long) {
        workManager.cancelAllWorkByTag(WaterReminderWorker.workTag(petId))
    }
}
