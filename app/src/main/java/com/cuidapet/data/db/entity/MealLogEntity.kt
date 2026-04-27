package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Registra o que realmente aconteceu em cada refeição
// Para cada refeição programada (MealEntity) + cada dia,
// o tutor registra aqui quanto o pet comeu
@Entity(
    tableName = "meal_logs",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealId")]
)
data class MealLogEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // ID da refeição programada a que este log se refere
    val mealId: Long,

    // Data do registro — timestamp do início do dia (meia-noite)
    // Guardamos assim para facilitar consultas como "tudo que aconteceu hoje"
    val date: Long,

    // Porcentagem do que foi oferecido que o pet comeu
    // Valores sugeridos na interface: 0, 25, 50, 75, 100
    val eatenPercentage: Int,

    // Classificação do apetite:
    // "ALL"     = comeu tudo
    // "PARTIAL" = comeu parcialmente
    // "REFUSED" = recusou / não quis comer
    val appetiteStatus: String,

    // Observações livres — ex: "comeu devagar", "só comeu quando misturei com patê"
    val notes: String? = null,

    val registeredAt: Long = System.currentTimeMillis()
)
