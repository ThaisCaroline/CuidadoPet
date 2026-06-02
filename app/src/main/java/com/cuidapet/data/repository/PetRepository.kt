package com.cuidadopet.data.repository

import com.cuidadopet.data.db.dao.PetDao
import com.cuidadopet.data.db.entity.PetEntity
import com.cuidadopet.notification.BirthdayAlarmScheduler
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PetRepository @Inject constructor(
    private val petDao: PetDao,
    private val birthdayAlarmScheduler: BirthdayAlarmScheduler
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
    }
}
