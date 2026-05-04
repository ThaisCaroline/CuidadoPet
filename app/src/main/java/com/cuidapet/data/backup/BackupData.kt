package com.cuidadopet.data.backup

import com.cuidadopet.data.db.entity.HealthEntryEntity
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
import com.cuidadopet.data.db.entity.MedicationEntity
import com.cuidadopet.data.db.entity.MedicationLogEntity
import com.cuidadopet.data.db.entity.SporadicMealLogEntity
import com.cuidadopet.data.db.entity.WaterConfigEntity
import com.cuidadopet.data.db.entity.WaterLogEntity
import com.cuidadopet.data.db.entity.WeightRecordEntity

data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val pets: List<PetBackup> = emptyList(),
    val medications: List<MedicationEntity> = emptyList(),
    val medicationLogs: List<MedicationLogEntity> = emptyList(),
    val mealPlans: List<MealPlanEntity> = emptyList(),
    val meals: List<MealEntity> = emptyList(),
    val mealLogs: List<MealLogEntity> = emptyList(),
    val sporadicMealLogs: List<SporadicMealLogEntity> = emptyList(),
    val waterConfigs: List<WaterConfigEntity> = emptyList(),
    val waterLogs: List<WaterLogEntity> = emptyList(),
    val healthEntries: List<HealthEntryEntity> = emptyList(),
    val weightRecords: List<WeightRecordEntity> = emptyList(),
    val healthPhotos: List<HealthPhotoBackup> = emptyList()
)

// Versão de PetEntity para backup: photoPath é substituído por photoFile (nome do arquivo no ZIP)
data class PetBackup(
    val id: Long,
    val name: String,
    val species: String,
    val breed: String?,
    val birthDate: Long?,
    val weightKg: Double,
    val sex: String,
    val isNeutered: Boolean,
    val clinicalStates: String,
    val photoFile: String?,
    val createdAt: Long
)

// Versão de HealthPhotoEntity para backup: filePath é substituído por imageFile (nome no ZIP)
data class HealthPhotoBackup(
    val id: Long,
    val petId: Long,
    val entryDate: Long,
    val imageFile: String,
    val caption: String
)
