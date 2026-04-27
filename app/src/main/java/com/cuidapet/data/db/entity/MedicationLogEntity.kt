package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Registra cada evento de administração de medicamento
// Para cada dose programada, existe (ou existirá) um log aqui
@Entity(
    tableName = "medication_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId")]
)
data class MedicationLogEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // ID do medicamento ao qual este log pertence
    val medicationId: Long,

    // Horário em que a dose estava programada para ser dada (timestamp)
    // Ex: hoje às 20:00 = 1745614800000
    val scheduledAt: Long,

    // Horário em que o tutor registrou a ação — pode ser diferente do programado
    // Nullable: preenchido só quando o tutor registrar algo
    val registeredAt: Long? = null,

    // O que aconteceu com a dose:
    // "TAKEN"     = tomou normalmente ✓
    // "NOT_TAKEN" = não tomou ✗
    // "VOMITED"   = tomou mas vomitou ⚠️
    // Nullable: null = ainda não registrado (dose futura ou pendente)
    val status: String? = null,

    // Se status = "VOMITED": quantos minutos após a administração vomitou
    // Informação importante para o veterinário (se vomitou antes de 30min,
    // a dose pode não ter sido absorvida)
    val vomitMinutesAfter: Int? = null,

    // Observações livres — ex: "recusou, tive que misturar no patê"
    val notes: String? = null
)
