package com.cuidadopet.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cuidadopet.data.db.entity.MedicationEntity
import com.cuidadopet.data.db.entity.MedicationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    // Busca todos os medicamentos ativos de um pet, ordenados por nome
    @Query("SELECT * FROM medications WHERE petId = :petId AND isActive = 1 ORDER BY name ASC")
    fun getActiveMedications(petId: Long): Flow<List<MedicationEntity>>

    // Busca todos os medicamentos (ativos e encerrados) — para histórico
    @Query("SELECT * FROM medications WHERE petId = :petId ORDER BY isActive DESC, name ASC")
    fun getAllMedications(petId: Long): Flow<List<MedicationEntity>>

    // Busca um medicamento específico pelo id
    @Query("SELECT * FROM medications WHERE id = :medicationId")
    fun getMedicationById(medicationId: Long): Flow<MedicationEntity?>

    @Query("SELECT * FROM medications")
    suspend fun getAllForBackup(): List<MedicationEntity>

    @Query("SELECT * FROM medication_logs")
    suspend fun getAllLogsForBackup(): List<MedicationLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    // Desativa um medicamento (não deletamos — mantemos para histórico e relatório)
    @Query("UPDATE medications SET isActive = 0 WHERE id = :medicationId")
    suspend fun deactivateMedication(medicationId: Long)

    // ─── Logs de medicamento ────────────────────────────────────────

    // Busca todos os logs de um medicamento, do mais recente para o mais antigo
    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId ORDER BY scheduledAt DESC")
    fun getLogsForMedication(medicationId: Long): Flow<List<MedicationLogEntity>>

    // Busca logs de um período específico — usado para gerar o relatório
    // :startDate e :endDate são timestamps de início e fim do período
    @Query("""
        SELECT ml.* FROM medication_logs ml
        INNER JOIN medications m ON ml.medicationId = m.id
        WHERE m.petId = :petId
        AND ml.scheduledAt BETWEEN :startDate AND :endDate
        ORDER BY ml.scheduledAt ASC
    """)
    fun getLogsForPetInPeriod(
        petId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<MedicationLogEntity>>

    // Busca o log de uma dose específica no dia de hoje
    // Usado para verificar se a dose já foi registrada antes de exibir o alerta
    @Query("""
        SELECT * FROM medication_logs
        WHERE medicationId = :medicationId
        AND scheduledAt = :scheduledAt
        LIMIT 1
    """)
    suspend fun getLogForScheduledDose(
        medicationId: Long,
        scheduledAt: Long
    ): MedicationLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MedicationLogEntity): Long

    @Update
    suspend fun updateLog(log: MedicationLogEntity)

    @Query("DELETE FROM medication_logs WHERE id = :logId")
    suspend fun deleteLog(logId: Long)
}
