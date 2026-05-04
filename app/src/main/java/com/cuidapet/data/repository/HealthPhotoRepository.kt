package com.cuidadopet.data.repository

import android.content.Context
import android.net.Uri
import com.cuidadopet.data.db.dao.HealthPhotoDao
import com.cuidadopet.data.db.entity.HealthPhotoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthPhotoRepository @Inject constructor(
    private val dao: HealthPhotoDao,
    @ApplicationContext private val context: Context
) {

    fun observeForDay(petId: Long, date: Long): Flow<List<HealthPhotoEntity>> =
        dao.observeForDay(petId, date)

    suspend fun getForPeriod(petId: Long, start: Long, end: Long): List<HealthPhotoEntity> =
        dao.getForPeriod(petId, start, end)

    suspend fun savePhoto(petId: Long, entryDate: Long, uri: Uri, caption: String = ""): HealthPhotoEntity {
        val file = copyToInternalStorage(petId, uri)
        val entity = HealthPhotoEntity(
            petId = petId,
            entryDate = entryDate,
            filePath = file.absolutePath,
            caption = caption
        )
        val id = dao.insert(entity)
        return entity.copy(id = id)
    }

    suspend fun deletePhoto(photo: HealthPhotoEntity) {
        File(photo.filePath).delete()
        dao.delete(photo)
    }

    private fun copyToInternalStorage(petId: Long, uri: Uri): File {
        val dir = File(context.filesDir, "health_photos/$petId").also { it.mkdirs() }
        val file = File(dir, "${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }
}
