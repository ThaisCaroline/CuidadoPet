package com.cuidadopet.data.db.dao

import androidx.room.Dao
import androidx.room.Query

// Apaga todos os dados do app antes de restaurar um backup.
// Filhos vêm antes dos pais, para não depender de a chave estrangeira estar ligada.
@Dao
interface BackupRestoreDao {

    @Query("DELETE FROM pet_documents")
    suspend fun deletePetDocuments()

    @Query("DELETE FROM vaccines")
    suspend fun deleteVaccines()

    @Query("DELETE FROM health_photos")
    suspend fun deleteHealthPhotos()

    @Query("DELETE FROM health_entries")
    suspend fun deleteHealthEntries()

    @Query("DELETE FROM weight_records")
    suspend fun deleteWeightRecords()

    @Query("DELETE FROM medication_logs")
    suspend fun deleteMedicationLogs()

    @Query("DELETE FROM medications")
    suspend fun deleteMedications()

    @Query("DELETE FROM meal_logs")
    suspend fun deleteMealLogs()

    @Query("DELETE FROM sporadic_meal_logs")
    suspend fun deleteSporadicMealLogs()

    @Query("DELETE FROM meals")
    suspend fun deleteMeals()

    @Query("DELETE FROM meal_plans")
    suspend fun deleteMealPlans()

    @Query("DELETE FROM water_logs")
    suspend fun deleteWaterLogs()

    @Query("DELETE FROM water_configs")
    suspend fun deleteWaterConfigs()

    @Query("DELETE FROM pets")
    suspend fun deletePets()
}
