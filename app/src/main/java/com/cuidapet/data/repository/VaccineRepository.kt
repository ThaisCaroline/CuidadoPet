package com.cuidadopet.data.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.cuidadopet.data.db.dao.VaccineDao
import com.cuidadopet.data.db.entity.VaccineEntity
import com.cuidadopet.notification.VaccineReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaccineRepository @Inject constructor(
    private val vaccineDao: VaccineDao,
    @ApplicationContext private val context: Context
) {

    fun getVaccinesForPet(petId: Long): Flow<List<VaccineEntity>> =
        vaccineDao.getVaccinesForPet(petId)

    fun getById(id: Long): Flow<VaccineEntity?> = vaccineDao.getById(id)

    suspend fun getLastDosesPerVaccine(petId: Long): List<VaccineEntity> =
        vaccineDao.getLastDosesPerVaccine(petId)

    suspend fun insert(vaccine: VaccineEntity, petName: String): Long {
        val id = vaccineDao.insert(vaccine)
        scheduleReminderIfNeeded(vaccine.copy(id = id), petName)
        return id
    }

    suspend fun update(vaccine: VaccineEntity, petName: String) {
        vaccineDao.update(vaccine)
        cancelReminder(vaccine.id)
        scheduleReminderIfNeeded(vaccine, petName)
    }

    suspend fun delete(id: Long) {
        cancelReminder(id)
        vaccineDao.delete(id)
    }

    private fun scheduleReminderIfNeeded(vaccine: VaccineEntity, petName: String) {
        val nextDue = vaccine.nextDueDate ?: return
        if (!vaccine.reminderEnabled) return
        val delayMs = nextDue - System.currentTimeMillis()
        if (delayMs <= 0) return

        val data = workDataOf(
            VaccineReminderWorker.KEY_VACCINE_ID   to vaccine.id,
            VaccineReminderWorker.KEY_VACCINE_NAME to vaccine.name,
            VaccineReminderWorker.KEY_VACCINE_TYPE to vaccine.type,
            VaccineReminderWorker.KEY_PET_NAME     to petName
        )

        val request = OneTimeWorkRequestBuilder<VaccineReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("vaccine_${vaccine.id}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "vaccine_reminder_${vaccine.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun cancelReminder(vaccineId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("vaccine_reminder_$vaccineId")
    }
}
