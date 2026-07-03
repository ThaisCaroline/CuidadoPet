package com.cuidadopet.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cuidadopet.data.db.entity.WaterConfigEntity
import com.cuidadopet.data.db.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {

    // ─── Configuração de hidratação ─────────────────────────────────

    // Busca a configuração de água do pet
    @Query("SELECT * FROM water_configs WHERE petId = :petId LIMIT 1")
    fun getWaterConfig(petId: Long): Flow<WaterConfigEntity?>

    @Query("SELECT * FROM water_configs")
    suspend fun getAllConfigsForBackup(): List<WaterConfigEntity>

    @Query("SELECT * FROM water_logs")
    suspend fun getAllWaterLogsForBackup(): List<WaterLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterConfig(config: WaterConfigEntity): Long

    @Update
    suspend fun updateWaterConfig(config: WaterConfigEntity)

    @Query("DELETE FROM water_configs WHERE petId = :petId")
    suspend fun deleteWaterConfig(petId: Long)

    // ─── Registros de ingestão ──────────────────────────────────────

    // Busca todos os registros de água de um pet em um dia específico
    // :dayStart e :dayEnd são os timestamps de meia-noite e 23:59:59 do dia
    // Soma desses registros = total bebido no dia
    @Query("""
        SELECT * FROM water_logs
        WHERE petId = :petId
        AND registeredAt BETWEEN :dayStart AND :dayEnd
        ORDER BY registeredAt ASC
    """)
    fun getLogsForDay(petId: Long, dayStart: Long, dayEnd: Long): Flow<List<WaterLogEntity>>

    // Total de água em ml bebida em um dia — direto do banco, sem precisar somar no código
    // SUM() é uma função SQL que soma todos os valores de uma coluna
    // COALESCE transforma null em 0.0 caso não haja registros no dia
    @Query("""
        SELECT COALESCE(SUM(amountMl), 0.0) FROM water_logs
        WHERE petId = :petId
        AND registeredAt BETWEEN :dayStart AND :dayEnd
    """)
    fun getTotalForDay(petId: Long, dayStart: Long, dayEnd: Long): Flow<Double>

    // Busca registros de um período para o relatório
    @Query("""
        SELECT * FROM water_logs
        WHERE petId = :petId
        AND registeredAt BETWEEN :startDate AND :endDate
        ORDER BY registeredAt ASC
    """)
    fun getLogsForPeriod(petId: Long, startDate: Long, endDate: Long): Flow<List<WaterLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLogEntity): Long

    @Update
    suspend fun updateLog(log: WaterLogEntity)

    // Remove um registro específico — caso o tutor tenha digitado errado
    @Query("DELETE FROM water_logs WHERE id = :logId")
    suspend fun deleteLog(logId: Long)

    // Verifica se há algum registro de água desde :since — usado pelo Worker para
    // não disparar o lembrete se o tutor já ofereceu água no intervalo atual
    @Query("SELECT COUNT(*) > 0 FROM water_logs WHERE petId = :petId AND registeredAt >= :since")
    suspend fun hasLogSince(petId: Long, since: Long): Boolean
}
