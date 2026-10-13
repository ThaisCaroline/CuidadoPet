package com.cuidadopet.data.repository

import android.content.Context
import com.cuidadopet.data.db.dao.MedicationDao
import com.cuidadopet.data.db.entity.MedicationEntity
import com.cuidadopet.data.db.entity.MedicationLogEntity
import com.cuidadopet.notification.AlarmScheduler
import androidx.glance.appwidget.updateAll
import com.cuidadopet.widget.TodayWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import android.util.Log
import javax.inject.Singleton

private const val TAG = "MedicationRepository"

@Singleton
class MedicationRepository @Inject constructor(
    private val medicationDao: MedicationDao,
    private val alarmScheduler: AlarmScheduler,
    @ApplicationContext private val context: Context
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

    // Insere vários medicamentos de uma vez (ex.: leitura de receita).
    // Grava tudo numa transação e só agenda os alarmes depois que todas as linhas estiverem salvas.
    suspend fun insertMedications(medications: List<MedicationEntity>, petName: String): List<Long> {
        val ids = medicationDao.insertMedications(medications)
        // Os registros já estão salvos. Se um alarme falhar, não tratamos como erro de salvamento:
        // o tutor tentaria de novo e duplicaria os medicamentos.
        medications.zip(ids).forEach { (medication, id) ->
            try {
                alarmScheduler.scheduleMedication(medication.copy(id = id), petName, refreshWidget = false)
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao agendar alarme do medicamento $id", e)
            }
        }
        // Uma única atualização do widget para o lote inteiro
        try { TodayWidget().updateAll(context) } catch (_: Exception) {}
        return ids
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
        // Atualiza o widget para não mostrar mais a dose deste medicamento
        try { TodayWidget().updateAll(context) } catch (_: Exception) {}
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

    suspend fun deleteLog(logId: Long) =
        medicationDao.deleteLog(logId)
}
