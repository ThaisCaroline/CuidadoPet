package com.cuidadopet.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cuidadopet.data.db.entity.PetDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDocumentDao {

    @Query("SELECT * FROM pet_documents WHERE petId = :petId ORDER BY documentDate DESC, createdAt DESC")
    fun observeForPet(petId: Long): Flow<List<PetDocumentEntity>>

    @Query("SELECT * FROM pet_documents WHERE petId = :petId")
    suspend fun getForPetOnce(petId: Long): List<PetDocumentEntity>

    @Query("SELECT * FROM pet_documents")
    suspend fun getAllForBackup(): List<PetDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: PetDocumentEntity): Long

    @Update
    suspend fun update(document: PetDocumentEntity)

    @Delete
    suspend fun delete(document: PetDocumentEntity)
}
