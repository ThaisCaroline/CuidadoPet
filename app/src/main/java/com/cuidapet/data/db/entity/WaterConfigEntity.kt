package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Configuração de hidratação do pet
// Define a meta diária de água e a frequência dos lembretes
// Cada pet tem uma configuração ativa
@Entity(
    tableName = "water_configs",
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
data class WaterConfigEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val petId: Long,

    // Meta diária de água em mililitros
    // Calculada pelo app (40-60ml/kg) mas pode ser sobrescrita pelo tutor
    // conforme orientação do veterinário
    val dailyTargetMl: Double,

    // A cada quantas horas o app lembra o tutor de oferecer água
    // Ex: 3 = lembrete a cada 3 horas
    val reminderIntervalHours: Int = 3,

    // Horário de início dos lembretes no formato "HH:mm"
    // Ex: "13:00" → primeiro lembrete às 13h, depois 15h, 17h... (com intervalo 2h)
    @androidx.room.ColumnInfo(defaultValue = "08:00")
    val reminderStartTime: String = "08:00",

    // Horário de fim dos lembretes — alertas fora dessa janela são silenciados
    @androidx.room.ColumnInfo(defaultValue = "23:55")
    val reminderEndTime: String = "23:55",

    // Se os lembretes estão ativos ou silenciados
    val remindersEnabled: Boolean = true,

    @androidx.room.ColumnInfo(name = "is_super_reminder", defaultValue = "0")
    val isSuperReminder: Boolean = false,

    val updatedAt: Long = System.currentTimeMillis()
)
