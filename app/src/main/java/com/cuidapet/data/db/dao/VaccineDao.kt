package com.cuidadopet.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cuidadopet.data.db.entity.VaccineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccineDao {

    @Query("SELECT * FROM vaccines WHERE petId = :petId ORDER BY administeredAt DESC, createdAt DESC")
    fun getVaccinesForPet(petId: Long): Flow<List<VaccineEntity>>

    @Query("SELECT * FROM vaccines WHERE id = :id")
    fun getById(id: Long): Flow<VaccineEntity?>

    // Retorna a última dose administrada de cada vacina/vermífugo por nome (para o relatório)
    @Query("""
        SELECT v.* FROM vaccines v
        INNER JOIN (
            SELECT name, MAX(administeredAt) AS maxDate
            FROM vaccines
            WHERE petId = :petId AND administeredAt IS NOT NULL
            GROUP BY name
        ) sub ON v.name = sub.name AND v.administeredAt = sub.maxDate
        WHERE v.petId = :petId
        ORDER BY v.type, v.name
    """)
    suspend fun getLastDosesPerVaccine(petId: Long): List<VaccineEntity>

    @Query("SELECT * FROM vaccines")
    suspend fun getAllForBackup(): List<VaccineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vaccine: VaccineEntity): Long

    @Update
    suspend fun update(vaccine: VaccineEntity)

    @Query("DELETE FROM vaccines WHERE id = :id")
    suspend fun delete(id: Long)
}
