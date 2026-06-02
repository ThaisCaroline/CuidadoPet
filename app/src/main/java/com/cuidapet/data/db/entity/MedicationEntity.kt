package com.cuidadopet.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ForeignKey conecta esta tabela à tabela "pets"
// É o equivalente no banco de dados à relação "um pet tem vários medicamentos"
//
// onDelete = CASCADE significa: se o pet for deletado,
// todos os seus medicamentos são deletados automaticamente também.
// Evita "lixo" no banco — medicamentos sem dono.
@Entity(
    tableName = "medications",
    foreignKeys = [
        ForeignKey(
            entity = PetEntity::class,       // tabela pai
            parentColumns = ["id"],           // coluna na tabela pai
            childColumns = ["petId"],         // coluna nesta tabela que aponta para o pai
            onDelete = ForeignKey.CASCADE     // deleta junto quando o pet for removido
        )
    ],
    // Index melhora a performance das consultas por petId
    // Sem index, o banco leria TODOS os medicamentos para encontrar os do pet certo
    // Com index, ele vai direto — como um índice de livro
    indices = [Index("petId")]
)
data class MedicationEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // ID do pet dono deste medicamento — conectado via ForeignKey acima
    val petId: Long,

    // Nome do medicamento — o tutor digita exatamente como está na receita
    // Ex: "Amoxicilina", "Prednisolona"
    val name: String,

    // Forma de administração
    // Valores possíveis: "ORAL", "TOPICAL", "INJECTABLE", "EYE_DROP", "OTHER"
    val form: String,

    // Quantidade por dose — só o número
    // Ex: "1", "0.5", "2"
    val dose: String,

    // Unidade da dose — separada para facilitar a exibição
    // Ex: "comprimido", "ml", "gota"
    val doseUnit: String,

    // Tipo de frequência:
    // "INTERVAL" = a cada X horas (ex: a cada 8h)
    // "FIXED_TIMES" = horários fixos do dia (ex: 08:00 e 20:00)
    val frequencyType: String,

    // Usado quando frequencyType = "INTERVAL"
    // Quantas horas entre cada dose — ex: 8 (a cada 8 horas)
    val frequencyHours: Int? = null,

    // Usado quando frequencyType = "FIXED_TIMES"
    // Lista de horários em JSON — ex: ["08:00","14:00","20:00"]
    val fixedTimes: String? = null,

    // Data de início do tratamento (timestamp)
    val startDate: Long,

    // Data de fim do tratamento (timestamp) — null se for uso contínuo
    val endDate: Long? = null,

    // Se true, o medicamento não tem data de fim (uso para sempre, ex: remédio para doença crônica)
    val isContinuous: Boolean = false,

    // Orientação de administração:
    // "WITH_FOOD" = com alimentação
    // "FASTING"   = em jejum
    // "WITH_WATER" = diluído em água
    // "OTHER"     = outro (ver guidelineDetail)
    val administrationGuideline: String = "OTHER",

    // Detalhamento da orientação quando guideline = "OTHER"
    // Ex: "Esconder na comida", "Amarga — usar seringa"
    val guidelineDetail: String? = null,

    // Observações livres do tutor sobre este medicamento
    val observations: String? = null,

    // Se false, o medicamento foi encerrado (tratamento concluído ou cancelado)
    // Mantemos no banco para histórico — nunca deletamos registros clínicos
    val isActive: Boolean = true,

    // Se false, o app NÃO dispara notificações para este medicamento
    @androidx.room.ColumnInfo(name = "reminder_enabled", defaultValue = "1")
    val reminderEnabled: Boolean = true,

    @androidx.room.ColumnInfo(name = "is_super_reminder", defaultValue = "0")
    val isSuperReminder: Boolean = false,

    val createdAt: Long = System.currentTimeMillis()
)
