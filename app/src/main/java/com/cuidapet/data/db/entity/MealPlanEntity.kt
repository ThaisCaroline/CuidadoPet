package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Plano alimentar do pet — define o que, quanto e quando ele come
// Um pet tem um plano ativo por vez (isActive = true)
// Planos anteriores são mantidos para histórico (isActive = false)
@Entity(
    tableName = "meal_plans",
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
data class MealPlanEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val petId: Long,

    // Tipo de alimento principal:
    // "DRY_KIBBLE"   = ração seca
    // "WET_FOOD"     = ração úmida / sachê
    // "NATURAL"      = alimentação natural / caseira
    // "THERAPEUTIC"  = dieta terapêutica prescrita
    // "OTHER"        = outro
    val foodType: String,

    // Detalhes do alimento em texto livre — marca da ração, composição da dieta natural, etc.
    // Ex: "Royal Canin Adult Medium" / "Frango com batata-doce e cenoura"
    val foodDetails: String? = null,

    // Restrições alimentares em texto livre
    // Ex: "Sem sódio, sem frango, sem glúten"
    val restrictions: String? = null,

    // Meta calórica diária em kcal — calculada pelo app ou informada pelo tutor
    // Nullable: pode não estar definida se o tutor preferir trabalhar só com gramas
    val dailyKcalTarget: Double? = null,

    // Quantidade total diária em gramas — calculada ou informada
    // Esta é a referência principal exibida no app
    val dailyQuantityGrams: Double? = null,

    // Se true, este é o plano alimentar atual do pet
    val isActive: Boolean = true,

    @androidx.room.ColumnInfo(name = "reminder_enabled", defaultValue = "1")
    val reminderEnabled: Boolean = true,

    @androidx.room.ColumnInfo(name = "is_super_reminder", defaultValue = "0")
    val isSuperReminder: Boolean = false,

    val createdAt: Long = System.currentTimeMillis()
)
