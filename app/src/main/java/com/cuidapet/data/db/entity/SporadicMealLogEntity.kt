package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Representa uma refeição extra (esporádica) que o pet comeu fora do plano alimentar.
// Ex: um petisco, uma sobra, algo dado fora do horário programado.
// Diferente de MealLogEntity, não tem vínculo com nenhuma refeição do plano.
@Entity(
    tableName = "sporadic_meal_logs",

    // ForeignKey garante que se o pet for excluído, todos os logs dele
    // também são excluídos automaticamente (CASCADE)
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,
            parentColumns = ["id"],   // coluna na tabela "pets"
            childColumns = ["petId"], // coluna nesta tabela
            onDelete = ForeignKey.CASCADE
        )
    ],

    // Index melhora a performance das consultas que filtram por petId
    indices = [Index("petId")]
)
data class SporadicMealLogEntity(

    // Identificador único — gerado automaticamente pelo banco
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // A qual pet este registro pertence
    val petId: Long,

    // Descrição livre — o tutor pode escrever "petisco", "frango cozido", etc.
    // É opcional (nullable) — o tutor pode registrar sem descrever
    val description: String? = null,

    // Valor numérico da quantidade — opcional (o tutor pode não saber o peso exato)
    val amountGrams: Double? = null,

    // Unidade da quantidade: "g" (gramas) ou "ml" (mililitros)
    // Separado do valor para permitir extras em unidades diferentes do plano principal
    @androidx.room.ColumnInfo(defaultValue = "g")
    val amountUnit: String = "g",

    // Momento em que o registro foi feito — preenchido automaticamente com "agora"
    val registeredAt: Long = System.currentTimeMillis()
)