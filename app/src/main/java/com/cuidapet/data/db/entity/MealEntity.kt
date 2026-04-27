package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Representa uma refeição individual dentro do plano alimentar
// Ex: se o pet come 3x por dia, haverá 3 registros de MealEntity
// ligados ao mesmo MealPlanEntity
@Entity(
    tableName = "meals",
    foreignKeys = [
        ForeignKey(
            entity = MealPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealPlanId")]
)
data class MealEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // ID do plano alimentar ao qual esta refeição pertence
    val mealPlanId: Long,

    // Horário da refeição no formato "HH:mm" — ex: "07:00", "12:30", "19:00"
    // Usamos String para simplicidade — não precisamos de cálculos de tempo aqui
    val timeOfDay: String,

    // Quantidade desta refeição em gramas
    // Nem sempre é dailyQuantityGrams / número de refeições —
    // o tutor pode querer distribuir de forma irregular (mais à noite, menos de manhã)
    val quantityGrams: Double
)
