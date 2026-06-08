package com.cuidadopet.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cuidadopet.data.db.entity.HealthEntryEntity
import com.cuidadopet.data.db.entity.WeightRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {

    // ─── Diário de saúde ────────────────────────────────────────────

    // Busca todas as entradas do diário, da mais recente para a mais antiga
    // DESC = decrescente (mais novo primeiro)
    @Query("SELECT * FROM health_entries WHERE petId = :petId ORDER BY registeredAt DESC")
    fun getAllEntries(petId: Long): Flow<List<HealthEntryEntity>>

    // Busca entradas de um período específico — para o relatório
    @Query("""
        SELECT * FROM health_entries
        WHERE petId = :petId
        AND registeredAt BETWEEN :startDate AND :endDate
        ORDER BY registeredAt ASC
    """)
    fun getEntriesForPeriod(
        petId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<HealthEntryEntity>>

    @Query("SELECT * FROM health_entries")
    suspend fun getAllEntriesForBackup(): List<HealthEntryEntity>

    @Query("SELECT * FROM weight_records")
    suspend fun getAllWeightRecordsForBackup(): List<WeightRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HealthEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: HealthEntryEntity)

    @Query("DELETE FROM health_entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("SELECT COUNT(*) > 0 FROM health_entries WHERE petId = :petId AND registeredAt >= :since")
    suspend fun hasEntrySince(petId: Long, since: Long): Boolean

    // ─── Histórico de peso ──────────────────────────────────────────

    // Busca todos os registros de peso em ordem cronológica
    // ASC = crescente (mais antigo primeiro) — necessário para o gráfico
    // mostrar a evolução da esquerda para a direita
    @Query("SELECT * FROM weight_records WHERE petId = :petId ORDER BY date ASC")
    fun getAllWeightRecords(petId: Long): Flow<List<WeightRecordEntity>>

    // Busca o peso mais recente — exibido no perfil do pet
    @Query("SELECT * FROM weight_records WHERE petId = :petId ORDER BY date DESC, id DESC LIMIT 1")
    fun getLatestWeight(petId: Long): Flow<WeightRecordEntity?>

    // Busca o registro de peso de um dia específico — usado na sincronização do savePet
    @Query("SELECT * FROM weight_records WHERE petId = :petId AND date = :date LIMIT 1")
    suspend fun getWeightRecordForDate(petId: Long, date: Long): WeightRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightRecord(record: WeightRecordEntity): Long

    @Update
    suspend fun updateWeightRecord(record: WeightRecordEntity)

    @Query("DELETE FROM weight_records WHERE id = :recordId")
    suspend fun deleteWeightRecord(recordId: Long)
}
