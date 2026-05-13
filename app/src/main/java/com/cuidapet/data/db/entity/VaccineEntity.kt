package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vaccines",
    foreignKeys = [ForeignKey(
        entity = PetEntity::class,
        parentColumns = ["id"],
        childColumns = ["petId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("petId")]
)
data class VaccineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val type: String,           // "VACCINE" or "DEWORMER"
    val name: String,
    val administeredAt: Long?,  // null = ainda não administrada (só agendada)
    val nextDueDate: Long?,     // data da próxima dose para lembrete
    val notes: String?,
    val reminderEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
