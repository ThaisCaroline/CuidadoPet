package com.cuidadopet.data.repository

import com.cuidadopet.data.db.dao.HealthDao
import com.cuidadopet.data.db.entity.HealthEntryEntity
import com.cuidadopet.data.db.entity.WeightRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// Repositório de saúde — centraliza o acesso ao diário de sintomas e ao histórico de peso.
// O Hilt injeta o HealthDao automaticamente (já provido pelo DatabaseModule).
@Singleton
class HealthRepository @Inject constructor(
    private val healthDao: HealthDao
) {

    // ─── Diário de saúde ─────────────────────────────────────────────────────

    // Lista todas as entradas do diário, da mais recente para a mais antiga
    fun getAllEntries(petId: Long): Flow<List<HealthEntryEntity>> =
        healthDao.getAllEntries(petId)

    // Entradas de um período específico — usado no relatório
    fun getEntriesForPeriod(
        petId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<HealthEntryEntity>> =
        healthDao.getEntriesForPeriod(petId, startDate, endDate)

    suspend fun saveEntry(entry: HealthEntryEntity): Long =
        healthDao.insertEntry(entry)

    suspend fun updateEntry(entry: HealthEntryEntity) =
        healthDao.updateEntry(entry)

    suspend fun deleteEntry(entryId: Long) =
        healthDao.deleteEntry(entryId)

    // ─── Histórico de peso ───────────────────────────────────────────────────

    // Todos os registros de peso em ordem cronológica — necessário para o gráfico
    fun getAllWeightRecords(petId: Long): Flow<List<WeightRecordEntity>> =
        healthDao.getAllWeightRecords(petId)

    fun getLatestWeight(petId: Long): Flow<WeightRecordEntity?> =
        healthDao.getLatestWeight(petId)

    suspend fun getWeightRecordForDate(petId: Long, date: Long): WeightRecordEntity? =
        healthDao.getWeightRecordForDate(petId, date)

    suspend fun saveWeightRecord(record: WeightRecordEntity): Long =
        healthDao.insertWeightRecord(record)

    suspend fun updateWeightRecord(record: WeightRecordEntity) =
        healthDao.updateWeightRecord(record)

    suspend fun deleteWeightRecord(recordId: Long) =
        healthDao.deleteWeightRecord(recordId)
}
