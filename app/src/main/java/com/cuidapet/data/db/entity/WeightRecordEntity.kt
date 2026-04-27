package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Registro histórico de peso do pet
// Cada vez que o tutor pesa o pet, cria um novo registro aqui
// O gráfico de evolução de peso é gerado a partir desta tabela
@Entity(
    tableName = "weight_records",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],
            childColumns = ["petId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("petId")]
)
data class WeightRecordEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val petId: Long,

    // Data da pesagem — guardamos o timestamp do início do dia
    // para facilitar a exibição ("pesado em 25/04/2026")
    val date: Long,

    // Peso registrado em quilogramas
    // Ex: 4.3 (quatro quilos e trezentos gramas)
    val weightKg: Double,

    // Observação opcional — ex: "pesado na clínica", "balança doméstica"
    val notes: String? = null
)
