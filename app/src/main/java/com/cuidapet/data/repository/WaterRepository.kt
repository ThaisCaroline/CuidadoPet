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

    // Remove um registro (caso o tutor tenha errado o valor)
    suspend fun deleteWaterLog(logId: Long) =
        waterDao.deleteLog(logId)

    // ─── WorkManager — lembretes periódicos ──────────────────────────────────

    // Agenda um lembrete periódico de água para o pet.
    // O WorkManager garante que o lembrete seja disparado mesmo após o app ser fechado.
    private fun scheduleWaterReminder(config: WaterConfigEntity, petName: String) {
        // Dados passados para o worker — ele precisa saber o nome do pet e a meta
        val inputData = Data.Builder()
            .putString(WaterReminderWorker.KEY_PET_NAME,  petName)
            .putLong(WaterReminderWorker.KEY_PET_ID,      config.petId)
            .putDouble(WaterReminderWorker.KEY_TARGET_ML, config.dailyTargetMl)
            .build()

        // PeriodicWorkRequest: repete a cada X horas indefinidamente
        // O WorkManager garante que o mínimo é 15 minutos (limite do Android)
        val intervalHours = config.reminderIntervalHours.toLong().coerceAtLeast(1L)

        val workRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            repeatInterval = intervalHours,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInputData(inputData)
            .addTag(WaterReminderWorker.workTag(config.petId))
            .build()

        // REPLACE: cancela o lembrete anterior e cria um novo com os dados atualizados
        // Útil quando o tutor muda o intervalo nas configurações
        workManager.enqueueUniquePeriodicWork(
            WaterReminderWorker.workTag(config.petId),  // nome único por pet
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    // Cancela os lembretes de água de um pet — chamado quando remindersEnabled = false
    fun cancelWaterReminder(petId: Long) {
        workManager.cancelAllWorkByTag(WaterReminderWorker.workTag(petId))
    }
}
