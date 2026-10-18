package com.cuidadopet.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.cuidadopet.data.db.AppDatabase
import com.cuidadopet.data.db.dao.BackupRestoreDao
import com.cuidadopet.data.db.dao.FeedingDao
import com.cuidadopet.data.db.dao.HealthDao
import com.cuidadopet.data.db.dao.HealthPhotoDao
import com.cuidadopet.data.db.dao.MedicationDao
import com.cuidadopet.data.db.dao.PetDao
import com.cuidadopet.data.db.dao.PetDocumentDao
import com.cuidadopet.data.db.dao.VaccineDao
import com.cuidadopet.data.db.dao.WaterDao
import com.cuidadopet.data.db.entity.HealthPhotoEntity
import com.cuidadopet.data.db.entity.PetDocumentEntity
import com.cuidadopet.data.db.entity.PetEntity
import com.cuidadopet.data.repository.VaccineRepository
import com.cuidadopet.data.repository.WaterRepository
import com.cuidadopet.domain.PetDocumentRules
import com.cuidadopet.notification.AlarmScheduler
import com.cuidadopet.notification.BirthdayAlarmScheduler
import com.cuidadopet.notification.MealAlarmScheduler
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

// Erro de backup com motivo. A tela traduz o motivo (não usa texto fixo em português).
class BackupException(val reason: Reason, val detail: String = "") : IOException(reason.name) {
    enum class Reason {
        ENTRY_TOO_LARGE,   // um arquivo (detail = nome) passa de 30 MB
        BACKUP_TOO_LARGE,  // o conteúdo passa de 500 MB
        NO_SPACE,          // falta espaço no aparelho (detail = quanto liberar)
        INVALID            // o arquivo escolhido não é um backup válido
    }
}

@Singleton
class BackupManager @Inject constructor(
    private val database: AppDatabase,
    private val backupRestoreDao: BackupRestoreDao,
    private val petDao: PetDao,
    private val medicationDao: MedicationDao,
    private val feedingDao: FeedingDao,
    private val waterDao: WaterDao,
    private val healthDao: HealthDao,
    private val healthPhotoDao: HealthPhotoDao,
    private val vaccineDao: VaccineDao,
    private val petDocumentDao: PetDocumentDao,
    private val vaccineRepository: VaccineRepository,
    private val waterRepository: WaterRepository,
    private val alarmScheduler: AlarmScheduler,
    private val mealAlarmScheduler: MealAlarmScheduler,
    private val birthdayAlarmScheduler: BirthdayAlarmScheduler,
    @ApplicationContext private val context: Context
) {
    private val gson: Gson = GsonBuilder().serializeNulls().create()

    // Resultado da restauração: quantos documentos vieram no backup sem o arquivo PDF
    data class RestoreReport(val skippedDocuments: Int)

    companion object {
        private const val MAX_ZIP_ENTRIES = 5_000               // máximo de arquivos no ZIP
        private const val MAX_ENTRY_BYTES = 30L * 1024 * 1024   // 30 MB por arquivo (PDFs são limitados a 20 MB)
        private const val MAX_TOTAL_BYTES = PetDocumentRules.BACKUP_LIMIT_BYTES  // 500 MB no total
        private const val SPACE_MARGIN_BYTES = 50L * 1024 * 1024
    }

    // Tamanho do conteúdo que entraria no backup (fotos e PDFs). A tela de backup usa para avisar o limite.
    suspend fun estimateBackupBytes(): Long = withContext(Dispatchers.IO) {
        val photos = petDao.getAllOnce().mapNotNull { it.photoPath }.sumOf { sizeOf(it) }
        val healthPhotos = healthPhotoDao.getAllForBackup().sumOf { sizeOf(it.filePath) }
        val documents = petDocumentDao.getAllForBackup().sumOf { sizeOf(it.filePath) }
        photos + healthPhotos + documents
    }

    private fun sizeOf(path: String): Long = File(path).let { if (it.exists()) it.length() else 0L }

    // Um arquivo acima de 30 MB geraria um backup que a restauração recusa
    private fun requireEntrySize(path: String) {
        val file = File(path)
        if (file.exists() && file.length() > MAX_ENTRY_BYTES) {
            throw BackupException(BackupException.Reason.ENTRY_TOO_LARGE, file.name)
        }
    }

    // Grava um arquivo no ZIP. Retorna false se o arquivo sumiu antes de ser lido (não gera entrada vazia).
    private fun addFile(zip: ZipOutputStream, entryName: String, file: File): Boolean {
        val input = try {
            file.inputStream()
        } catch (_: FileNotFoundException) {
            return false
        }
        input.use { src ->
            zip.putNextEntry(ZipEntry(entryName))
            src.copyTo(zip)
            zip.closeEntry()
        }
        return true
    }

    suspend fun exportBackup(): File = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        // Fotos vão direto para o ZIP (sem cópia intermediária): o espaço necessário é só o tamanho do backup
        val images = mutableListOf<Pair<String, File>>()

        // Antes de copiar qualquer coisa: um backup acima do limite não poderia ser restaurado
        val needed = estimateBackupBytes()
        if (needed > MAX_TOTAL_BYTES) throw BackupException(BackupException.Reason.BACKUP_TOO_LARGE)
        // Confere o espaço livre antes de começar, para falhar com mensagem clara e não no meio
        if (context.cacheDir.usableSpace < needed + SPACE_MARGIN_BYTES) {
            throw BackupException(
                BackupException.Reason.NO_SPACE,
                PetDocumentRules.formatMegabytes(needed + SPACE_MARGIN_BYTES)
            )
        }

        val pets = petDao.getAllOnce()
        val healthPhotos = healthPhotoDao.getAllForBackup().filter { File(it.filePath).exists() }
        val documents = petDocumentDao.getAllForBackup().filter { File(it.filePath).exists() }

        pets.mapNotNull { it.photoPath }.forEach { requireEntrySize(it) }
        healthPhotos.forEach { requireEntrySize(it.filePath) }
        documents.forEach { requireEntrySize(it.filePath) }

        val petBackups = pets.map { pet ->
            val photoFile = pet.photoPath?.let { path ->
                val src = File(path)
                if (src.exists()) {
                    val destName = "pet_${pet.id}_${src.name}"
                    images += destName to src
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

        val healthPhotoBackups = healthPhotos.map { hp ->
            val src = File(hp.filePath)
            val destName = "health_${hp.id}_${src.name}"
            images += destName to src
            HealthPhotoBackup(
                id = hp.id, petId = hp.petId, entryDate = hp.entryDate,
                imageFile = destName, caption = hp.caption
            )
        }

        val zipFile = File(context.cacheDir, "backups/cuidadopet_backup_$timestamp.zip")
        zipFile.parentFile?.mkdirs()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zip ->
            images.forEach { (name, file) -> addFile(zip, "images/$name", file) }

            // PDFs são gravados direto no ZIP. Um PDF que sumiu durante a exportação é deixado de fora
            // (e de data.json), em vez de derrubar a exportação inteira.
            val writtenDocuments = documents.filter { doc ->
                addFile(zip, "documents/${File(doc.filePath).name}", File(doc.filePath))
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
                healthPhotos = healthPhotoBackups,
                vaccines = vaccineDao.getAllForBackup(),
                petDocuments = writtenDocuments.map { doc ->
                    PetDocumentBackup(
                        id = doc.id, petId = doc.petId, title = doc.title,
                        documentDate = doc.documentDate,
                        fileName = File(doc.filePath).name,
                        createdAt = doc.createdAt
                    )
                }
            )

            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(gson.toJson(backupData).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        zipFile
    }

    // Restaurar SUBSTITUI os dados atuais do app pelos do backup.
    // Tudo acontece em uma transação: se qualquer passo falhar, os dados atuais continuam como estavam.
    suspend fun importBackup(uri: Uri): RestoreReport = withContext(Dispatchers.IO) {
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
                        if (entryCount > MAX_ZIP_ENTRIES) throw BackupException(BackupException.Reason.INVALID)

                        if (!entry.isDirectory) {
                            // Zip Slip: garante que o arquivo extraído fica dentro de tempDir
                            val outFile = File(tempDir, entry.name).canonicalFile
                            if (!outFile.canonicalPath.startsWith(tempCanonical + File.separator))
                                throw BackupException(BackupException.Reason.INVALID)

                            outFile.parentFile?.mkdirs()

                            // Limite por arquivo e total
                            var fileBytes = 0L
                            outFile.outputStream().use { out ->
                                val buf = ByteArray(8192)
                                var read: Int
                                while (zip.read(buf).also { read = it } != -1) {
                                    fileBytes  += read
                                    totalBytes += read
                                    if (fileBytes > MAX_ENTRY_BYTES) throw BackupException(BackupException.Reason.INVALID)
                                    if (totalBytes > MAX_TOTAL_BYTES) throw BackupException(BackupException.Reason.INVALID)
                                    out.write(buf, 0, read)
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: throw BackupException(BackupException.Reason.INVALID)

            val jsonFile = File(tempDir, "data.json")
            if (!jsonFile.exists()) throw BackupException(BackupException.Reason.INVALID)

            val backup = gson.fromJson(jsonFile.readText(), BackupData::class.java)
                ?: throw BackupException(BackupException.Reason.INVALID)

            // Dados atuais: guardamos IDs e arquivos para cancelar alarmes e apagar arquivos só depois da troca
            val current = snapshotCurrentData()

            // 1) Copia os arquivos para pastas do app. Os nomes são novos, então não sobrescrevem arquivos atuais.
            val newFiles = mutableListOf<File>()
            val petPhotosDir = File(context.filesDir, "photos").also { it.mkdirs() }
            val photoPathByPetId = mutableMapOf<Long, String>()
            for (pet in backup.pets) {
                val fileName = pet.photoFile ?: continue
                val src = File(tempDir, "images/$fileName")
                if (!src.exists()) continue
                val dest = File(petPhotosDir, fileName)
                src.copyTo(dest, overwrite = true)
                newFiles += dest
                photoPathByPetId[pet.id] = dest.absolutePath
            }

            val healthPhotoRows = mutableListOf<HealthPhotoEntity>()
            for (hp in backup.healthPhotos) {
                val src = File(tempDir, "images/${hp.imageFile}")
                if (!src.exists()) continue
                val dir = File(context.filesDir, "health_photos/${hp.petId}").also { it.mkdirs() }
                val dest = File(dir, hp.imageFile)
                src.copyTo(dest, overwrite = true)
                newFiles += dest
                healthPhotoRows += HealthPhotoEntity(
                    id = hp.id, petId = hp.petId, entryDate = hp.entryDate,
                    filePath = dest.absolutePath, caption = hp.caption
                )
            }

            val documentRows = mutableListOf<PetDocumentEntity>()
            var skippedDocuments = 0
            for (doc in backup.petDocuments) {
                val src = File(tempDir, "documents/${doc.fileName}")
                if (!src.exists()) {
                    skippedDocuments++
                    continue
                }
                val dir = File(context.filesDir, "documents/pet_${doc.petId}").also { it.mkdirs() }
                val dest = File(dir, doc.fileName)
                src.copyTo(dest, overwrite = true)
                newFiles += dest
                documentRows += PetDocumentEntity(
                    id = doc.id, petId = doc.petId, title = doc.title,
                    documentDate = doc.documentDate, filePath = dest.absolutePath,
                    fileSizeBytes = dest.length(), createdAt = doc.createdAt
                )
            }

            // 2) Troca os registros numa transação. Se falhar, apaga os arquivos novos e mantém os atuais.
            try {
                database.withTransaction {
                    clearAllTables()

                    // Ordem respeitando as chaves estrangeiras: pets primeiro, depois dependentes
                    for (pet in backup.pets) {
                        petDao.insert(
                            PetEntity(
                                id = pet.id, name = pet.name, species = pet.species,
                                breed = pet.breed, birthDate = pet.birthDate, weightKg = pet.weightKg,
                                sex = pet.sex, isNeutered = pet.isNeutered,
                                clinicalStates = pet.clinicalStates,
                                photoPath = photoPathByPetId[pet.id], createdAt = pet.createdAt
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
                    for (row in healthPhotoRows) healthPhotoDao.insert(row)
                    for (vaccine in backup.vaccines) vaccineDao.insert(vaccine)
                    for (row in documentRows) petDocumentDao.insert(row)
                }
            } catch (e: Exception) {
                newFiles.forEach { it.delete() }
                throw e
            }

            // 3) Troca concluída: cancela alarmes antigos, apaga arquivos que não são mais usados e reagenda
            cancelCurrentAlarms(current)
            val newPaths = newFiles.map { it.absolutePath }.toSet()
            current.files.filter { it.absolutePath !in newPaths }.forEach { it.delete() }

            reagendarAlarmes(backup)
            val petNameById = backup.pets.associate { it.id to it.name }
            for (vaccine in backup.vaccines) {
                vaccineRepository.scheduleReminderFor(vaccine, petNameById[vaccine.petId] ?: "")
            }
            for (config in backup.waterConfigs) {
                waterRepository.restoreReminder(config, petNameById[config.petId] ?: "")
            }

            RestoreReport(skippedDocuments = skippedDocuments)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // Apaga todas as linhas das tabelas (dentro da transação de restauração)
    private suspend fun clearAllTables() {
        backupRestoreDao.deletePetDocuments()
        backupRestoreDao.deleteVaccines()
        backupRestoreDao.deleteHealthPhotos()
        backupRestoreDao.deleteHealthEntries()
        backupRestoreDao.deleteWeightRecords()
        backupRestoreDao.deleteMedicationLogs()
        backupRestoreDao.deleteMedications()
        backupRestoreDao.deleteMealLogs()
        backupRestoreDao.deleteSporadicMealLogs()
        backupRestoreDao.deleteMeals()
        backupRestoreDao.deleteMealPlans()
        backupRestoreDao.deleteWaterLogs()
        backupRestoreDao.deleteWaterConfigs()
        backupRestoreDao.deletePets()
    }

    private class CurrentData(
        val petIds: List<Long>,
        val medicationIds: List<Long>,
        val mealIds: List<Long>,
        val vaccineIds: List<Long>,
        val files: List<File>
    )

    private suspend fun snapshotCurrentData(): CurrentData {
        val pets = petDao.getAllOnce()
        val healthPhotos = healthPhotoDao.getAllForBackup()
        val documents = petDocumentDao.getAllForBackup()
        return CurrentData(
            petIds = pets.map { it.id },
            medicationIds = medicationDao.getAllForBackup().map { it.id },
            mealIds = feedingDao.getAllMealsForBackup().map { it.id },
            vaccineIds = vaccineDao.getAllForBackup().map { it.id },
            files = pets.mapNotNull { it.photoPath }.map { File(it) } +
                healthPhotos.map { File(it.filePath) } +
                documents.map { File(it.filePath) }
        )
    }

    private fun cancelCurrentAlarms(current: CurrentData) {
        current.medicationIds.forEach { alarmScheduler.cancelMedication(it) }
        current.mealIds.forEach { mealAlarmScheduler.cancelMeal(it) }
        current.petIds.forEach { birthdayAlarmScheduler.cancelBirthday(it) }
        current.petIds.forEach { waterRepository.cancelWaterReminder(it) }
        current.vaccineIds.forEach { vaccineRepository.cancelReminder(it) }
    }

    private fun reagendarAlarmes(backup: BackupData) {
        val petNameById = backup.pets.associate { it.id to it.name }
        val now = System.currentTimeMillis()

        for (med in backup.medications) {
            if (!med.isActive || !med.reminderEnabled) continue
            if (med.endDate != null && med.endDate < now) continue
            val petName = petNameById[med.petId] ?: continue
            alarmScheduler.scheduleMedication(med, petName)
        }

        val activePlanById = backup.mealPlans
            .filter { it.isActive && it.reminderEnabled }
            .associateBy { it.id }
        for (meal in backup.meals) {
            val plan = activePlanById[meal.mealPlanId] ?: continue
            val petName = petNameById[plan.petId] ?: continue
            mealAlarmScheduler.scheduleMeal(meal, petName, plan.isSuperReminder)
        }

        for (petBackup in backup.pets) {
            if (petBackup.birthDate == null) continue
            birthdayAlarmScheduler.scheduleBirthday(
                PetEntity(
                    id = petBackup.id, name = petBackup.name, species = petBackup.species,
                    breed = petBackup.breed, birthDate = petBackup.birthDate,
                    weightKg = petBackup.weightKg, sex = petBackup.sex,
                    isNeutered = petBackup.isNeutered, clinicalStates = petBackup.clinicalStates,
                    photoPath = null, createdAt = petBackup.createdAt
                )
            )
        }
    }
}
