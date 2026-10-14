package com.cuidadopet.data.repository

import android.content.Context
import com.cuidadopet.data.db.dao.PetDao
import com.cuidadopet.data.db.entity.PetEntity
import com.cuidadopet.notification.BirthdayAlarmScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetRepository @Inject constructor(
    private val petDao: PetDao,
    private val birthdayAlarmScheduler: BirthdayAlarmScheduler,
    @ApplicationContext private val context: Context
) {

    fun getAllPets(): Flow<List<PetEntity>> = petDao.getAll()

    fun getPetById(petId: Long): Flow<PetEntity?> = petDao.getById(petId)

    suspend fun insertPet(pet: PetEntity): Long {
        val newId = petDao.insert(pet)
        if (pet.birthDate != null) {
            birthdayAlarmScheduler.scheduleBirthday(pet.copy(id = newId))
        }
        return newId
    }

    suspend fun updatePet(pet: PetEntity) {
        petDao.update(pet)
        if (pet.birthDate != null) {
            birthdayAlarmScheduler.scheduleBirthday(pet)
        } else {
            birthdayAlarmScheduler.cancelBirthday(pet.id)
        }
    }

    suspend fun deletePet(pet: PetEntity) {
        petDao.delete(pet)
        birthdayAlarmScheduler.cancelBirthday(pet.id)
        // O banco apaga os registros em cascata, mas não os arquivos: removemos fotos e PDFs do pet.
        // Feito fora da thread principal: a pasta de documentos pode ter muitos arquivos grandes.
        withContext(Dispatchers.IO) {
            pet.photoPath?.let { File(it).delete() }
            File(context.filesDir, "health_photos/${pet.id}").deleteRecursively()
            File(context.filesDir, "documents/pet_${pet.id}").deleteRecursively()
        }
    }
}
