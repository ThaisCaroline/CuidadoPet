package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Entrada do diário de saúde — o tutor registra como o pet está
// Todos os campos de observação são opcionais (nullable)
// porque o tutor não precisa preencher tudo em toda entrada
@Entity(
    tableName = "health_entries",
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
data class HealthEntryEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val petId: Long,

    // Data e hora do registro
    val registeredAt: Long = System.currentTimeMillis(),

    // Comportamento geral:
    // "NORMAL"    = normal, ativo
    // "LETHARGIC" = apático, sem energia
    // "AGITATED"  = agitado, inquieto
    // "SLEEPY"    = sonolento além do normal
    val behavior: String? = null,

    // Estado das fezes:
    // "NORMAL"   = consistência normal
    // "SOFT"     = amolecidas
    // "DIARRHEA" = diarreia
    // "ABSENT"   = não evacuou
    // "BLOOD"    = com sangue — sinal de alerta importante
    val fecesStatus: String? = null,

    // Estado da urina:
    // "NORMAL"    = cor e volume normais
    // "INCREASED" = urinando mais que o habitual
    // "REDUCED"   = urinando menos
    // "ABSENT"    = não urinou
    // "BLOOD"     = com sangue — sinal de alerta importante
    val urineStatus: String? = null,

    // Número de episódios de vômito — 0 = não vomitou, null = não observado
    val vomitCount: Int? = null,

    // Mobilidade:
    // "NORMAL"   = se move normalmente
    // "REDUCED"  = dificuldade para se mover
    // "IMMOBILE" = não está se movendo
    val mobility: String? = null,

    // Sinais de dor (comportamentais — o tutor não mede, apenas observa):
    // "NONE"     = não aparenta dor
    // "APPARENT" = parece estar com dor (geme, se lambe em excesso, postura encurvada)
    // "EVIDENT"  = demonstra dor claramente
    val painSigns: String? = null,

    // Campo livre para o tutor descrever qualquer observação não coberta acima
    // Ex: "dormiu o dia todo", "não quer brincar", "latiu quando toquei na barriga"
    val observations: String? = null
)
