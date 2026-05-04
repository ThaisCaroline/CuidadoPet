package com.cuidadopet.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.cuidadopet.data.db.entity.HealthPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthPhotoDao {

    @Query("SELECT * FROM health_photos WHERE petId = :petId AND entryDate = :date ORDER BY id ASC")
    fun observeForDay(petId: Long, date: Long): Flow<List<HealthPhotoEntity>>

    @Query("SELECT * FROM health_photos WHERE petId = :petId AND entryDate BETWEEN :start AND :end ORDER BY entryDate ASC, id ASC")
    suspend fun getForPeriod(petId: Long, start: Long, end: Long): List<HealthPhotoEntity>

    @Insert
    suspend fun insert(photo: HealthPhotoEntity): Long

    @Delete
    suspend fun delete(photo: HealthPhotoEntity)
}
