package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Exame ou documento em PDF do pet.
// O arquivo é copiado para o armazenamento interno do app (filesDir/documents/pet_<id>/),
// então continua disponível mesmo que o arquivo original seja apagado da galeria ou dos downloads.
@Entity(
    tableName = "pet_documents",
    foreignKeys = [ForeignKey(
        entity = PetEntity::class,
        parentColumns = ["id"],
        childColumns = ["petId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("petId")]
)
data class PetDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Long,
    val title: String,
    // Data do exame: início do dia no horário local
    val documentDate: Long,
    // Caminho absoluto do PDF copiado para o armazenamento interno
    val filePath: String,
    val fileSizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)
