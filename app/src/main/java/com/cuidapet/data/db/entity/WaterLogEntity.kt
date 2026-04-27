package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Registra cada vez que o tutor anota que o pet bebeu água
// O tutor pode registrar várias vezes ao dia — o app soma tudo
// para comparar com a meta diária
@Entity(
    tableName = "water_logs",
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
data class WaterLogEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val petId: Long,

    // Momento exato em que o tutor registrou (timestamp completo)
    // Diferente do MealLogEntity que guarda só a data —
    // aqui guardamos hora também para exibir "bebeu às 10h, às 14h..."
    val registeredAt: Long = System.currentTimeMillis(),

    // Quantidade de água em mililitros neste registro
    // Ex: 150 (um copo pequeno), 250 (um copo médio)
    val amountMl: Double,

    // Observação opcional — ex: "bebeu sozinho", "ofereci na seringa"
    val notes: String? = null
)
