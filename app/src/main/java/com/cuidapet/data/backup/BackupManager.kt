package com.cuidadopet.data.backup

import android.content.Context
import android.net.Uri
import com.cuidadopet.data.db.dao.FeedingDao
import com.cuidadopet.data.db.dao.HealthDao
import com.cuidadopet.data.db.dao.HealthPhotoDao
import com.cuidadopet.data.db.dao.MedicationDao
import com.cuidadopet.data.db.dao.PetDao
import com.cuidadopet.data.db.dao.WaterDao
import com.cuidadopet.data.db.entity.HealthPhotoEntity
import com.cuidadopet.data.db.entity.PetEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val petDao: PetDao,
    private val medicationDao: MedicationDao,
    private val feedingDao: FeedingDao,
    private val waterDao: WaterDao,
    private val healthDao: HealthDao,
    private val healthPhotoDao: HealthPhotoDao,
    @ApplicationContext private val context: Context
) {
    private val gson: Gson = GsonBuilder().serializeNulls().create()

    companion object {
        private const val MAX_ZIP_ENTRIES = 1_000          // máximo de arquivos no ZIP
        private const val MAX_ENTRY_BYTES = 30L * 1024 * 1024   // 30 MB por arquivo
        private const val MAX_TOTAL_BYTES = 150L * 1024 * 1024  // 150 MB total
    }

    suspend fun exportBackup(): File = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val tempDir = File(context.cacheDir, "backups/tmp_$timestamp").also { it.mkdirs() }
        val imagesDir = File(tempDir, "images").also { it.mkdirs() }

        try {
            val pets = petDao.getAllOnce()
            val healthPhotos = healthPhotoDao.getAllForBackup()

            val petBackups = pets.map { pet ->
                val photoFile = pet.photoPath?.let { path ->
                    val src = File(path)
                    if (src.exists()) {
                        val destName = "pet_${pet.id}_${src.name}"
                        src.copyTo(File(imagesDir, destName), overwrite = true)
                        destName
                    } else null
                }
                PetBackup(
                    id = pet.id, name = pet.name, species = pet.species,
                    breed = pet.breed, birthDate = pet.birthDate, weightKg = pet.weightKg,
                    sex = pet.sex, isNeutered = pet.isNeutered,
                    clinicalStates = pet.clinicalStates,
                    photoFile = photoFile, createdAt = pet.createdAt
                )
            }

            val healthPhotoBackups = healthPhotos.mapNotNull { hp ->
                val src = File(hp.filePath)
                if (!src.exists()) return@mapNotNull null
                val destName = "health_${hp.id}_${src.name}"
                src.copyTo(File(imagesDir, destName), overwrite = true)
                HealthPhotoBackup(
                    id = hp.id, petId = hp.petId, entryDate = hp.entryDate,
                    imageFile = destName, caption = hp.caption
                )
            }

            val backupData = BackupData(
                exportedAt = timestamp,
                pets = petBackups,
                medications = medicationDao.getAllForBackup(),
                medicationLogs = medicationDao.getAllLogsForBackup(),
                mealPlans = feedingDao.getAllMealPlansForBackup(),
                meals = feedingDao.getAllMealsForBackup(),
                mealLogs = feedingDao.getAllMealLogsForBackup(),
                sporadicMealLogs = feedingDao.getAllSporadicLogsForBackup(),
                waterConfigs = waterDao.getAllConfigsForBackup(),
                waterLogs = waterDao.getAllWaterLogsForBackup(),
                healthEntries = healthDao.getAllEntriesForBackup(),
                weightRecords = healthDao.getAllWeightRecordsForBackup(),
                healthPhotos = healthPhotoBackups
            )

            val jsonFile = File(tempDir, "data.json")
            jsonFile.writeText(gson.toJson(backupData))

            val zipFile = File(context.cacheDir, "backups/cuidadopet_backup_$timestamp.zip")
            zipFile.parentFile?.mkdirs()

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zip ->
                zip.putNextEntry(ZipEntry("data.json"))
                jsonFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                imagesDir.listFiles()?.forEach { imgFile ->
                    zip.putNextEntry(ZipEntry("images/${imgFile.name}"))
                    imgFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }

            zipFile
        } finally {
            tempDir.deleteRecursively()
        }
    }

    suspend fun importBackup(uri: Uri) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val tempDir = File(context.cacheDir, "backups/restore_$timestamp").also { it.mkdirs() }

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(BufferedInputStream(input)).use { zip ->
                    val tempCanonical = tempDir.canonicalPath
                    var entryCount   = 0
                    var totalBytes   = 0L
                    var entry        = zip.nextEntry

                    while (entry != null) {
                        entryCount++
                        if (entryCount > MAX_ZIP_ENTRIES)
                            throw IOException("Backup inválido: número de arquivos excede o limite.")

                        if (!entry.isDirectory) {
                            // Zip Slip: garante que o arquivo extraído fica dentro de tempDir
                            val outFile = File(tempDir, entry.name).canonicalFile
                            if (!outFile.canonicalPath.startsWith(tempCanonical + File.separator))
                                throw IOException("Backup inválido: caminho suspeito detectado.")

                            outFile.parentFile?.mkdirs()

                            // Limite por arquivo e total
                            var fileBytes = 0L
                            outFile.outputStream().use { out ->
                                val buf = ByteArray(8192)
                                var read: Int
                                while (zip.read(buf).also { read = it } != -1) {
                                    fileBytes  += read
                                    totalBytes += read
                                    if (fileBytes > MAX_ENTRY_BYTES)
                                        throw IOException("Backup inválido: arquivo muito grande.")
                                    if (totalBytes > MAX_TOTAL_BYTES)
                                        throw IOException("Backup inválido: tamanho total excede o limite.")
                                    out.write(buf, 0, read)
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: throw IOException("Não foi possível abrir o arquivo de backup")

            val jsonFile = File(tempDir, "data.json")
            if (!jsonFile.exists()) throw IOException("Arquivo inválido: data.json não encontrado")

            val backup = gson.fromJson(jsonFile.readText(), BackupData::class.java)
                ?: throw IOException("Arquivo inválido: dados corrompidos ou formato desconhecido.")

            // Ordem respeitando as foreign keys: pets primeiro, depois dependentes
            val petPhotosDir = File(context.filesDir, "photos").also { it.mkdirs() }
            for (pet in backup.pets) {
                val newPhotoPath = pet.photoFile?.let { fileName ->
                    val src = File(tempDir, "images/$fileName")
                    if (src.exists()) {
                        val dest = File(petPhotosDir, fileName)
                        src.copyTo(dest, overwrite = true)
                        dest.absolutePath
                    } else null
                }
                petDao.insert(
                    PetEntity(
                        id = pet.id, name = pet.name, species = pet.species,
                        breed = pet.breed, birthDate = pet.birthDate, weightKg = pet.weightKg,
                        sex = pet.sex, isNeutered = pet.isNeutered,
                        clinicalStates = pet.clinicalStates,
                        photoPath = newPhotoPath, createdAt = pet.createdAt
                    )
                )
            }

            for (med in backup.medications) medicationDao.insertMedication(med)
            for (plan in backup.mealPlans) feedingDao.insertMealPlan(plan)
            for (config in backup.waterConfigs) waterDao.insertWaterConfig(config)
            for (entry in backup.healthEntries) healthDao.insertEntry(entry)
            for (record in backup.weightRecords) healthDao.insertWeightRecord(record)
            for (log in backup.medicationLogs) medicationDao.insertLog(log)
            for (meal in backup.meals) feedingDao.insertMeal(meal)
            for (log in backup.mealLogs) feedingDao.insertMealLog(log)
            for (log in backup.sporadicMealLogs) feedingDao.insertSporadicLog(log)
            for (log in backup.waterLogs) waterDao.insertLog(log)

            for (hp in backup.healthPhotos) {
                val photoDir = File(context.filesDir, "health_photos/${hp.petId}").also { it.mkdirs() }
                val src = File(tempDir, "images/${hp.imageFile}")
                if (src.exists()) {
                    val dest = File(photoDir, hp.imageFile)
                    src.copyTo(dest, overwrite = true)
                    healthPhotoDao.insert(
                        HealthPhotoEntity(
                            id = hp.id, petId = hp.petId, entryDate = hp.entryDate,
                            filePath = dest.absolutePath, caption = hp.caption
                        )
                    )
                }
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
