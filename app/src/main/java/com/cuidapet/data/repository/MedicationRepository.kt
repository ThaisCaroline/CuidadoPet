package com.cuidadopet.data.repository

import com.cuidadopet.data.db.dao.MedicationDao
import com.cuidadopet.data.db.entity.MedicationEntity
import com.cuidadopet.data.db.entity.MedicationLogEntity
import com.cuidadopet.notification.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationRepository @Inject constructor(
    private val medicationDao: MedicationDao,
    private val alarmScheduler: AlarmScheduler
) {

    // Busca medicamentos ativos de um pet
    fun getActiveMedications(petId: Long): Flow<List<MedicationEntity>> =
        medicationDao.getActiveMedications(petId)

    // Busca todos (ativos + encerrados) — para histórico
    fun getAllMedications(petId: Long): Flow<List<MedicationEntity>> =
        medicationDao.getAllMedications(petId)

    fun getMedicationById(id: Long): Flow<MedicationEntity?> =
        medicationDao.getMedicationById(id)

    // Insere medicamento E agenda o alarme
    // petName é necessário para personalizar o texto da notificação
    suspend fun insertMedication(medication: MedicationEntity, petName: String): Long {
        val id = medicationDao.insertMedication(medication)
        // Agenda com o id real gerado pelo banco
        alarmScheduler.scheduleMedication(medication.copy(id = id), petName)
        return id
    }

    // Atualiza medicamento E reagenda o alarme
    suspend fun updateMedication(medication: MedicationEntity, petName: String) {
        medicationDao.updateMedication(medication)
        // Cancela o alarme antigo e agenda o novo com os horários atualizados
        alarmScheduler.cancelMedication(medication.id)
        alarmScheduler.scheduleMedication(medication, petName)
    }

    // Desativa medicamento E cancela o alarme
    suspend fun deactivateMedication(medicationId: Long) {
        medicationDao.deactivateMedication(medicationId)
        alarmScheduler.cancelMedication(medicationId)
    }

    // ── Logs ──────────────────────────────────────────────────────────

    fun getLogsForMedication(medicationId: Long): Flow<List<MedicationLogEntity>> =
        medicationDao.getLogsForMedication(medicationId)

    fun getLogsForPetInPeriod(
        petId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<MedicationLogEntity>> =
        medicationDao.getLogsForPetInPeriod(petId, startDate, endDate)

    suspend fun saveLog(log: MedicationLogEntity): Long =
        medicationDao.insertLog(log)

    suspend fun updateLog(log: MedicationLogEntity) =
        medicationDao.updateLog(log)
}
