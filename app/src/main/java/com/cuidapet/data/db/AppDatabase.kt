package com.cuidadopet.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cuidadopet.data.db.dao.FeedingDao
import com.cuidadopet.data.db.dao.HealthDao
import com.cuidadopet.data.db.dao.MedicationDao
import com.cuidadopet.data.db.dao.PetDao
import com.cuidadopet.data.db.dao.WaterDao
import com.cuidadopet.data.db.entity.HealthEntryEntity
import com.cuidadopet.data.db.entity.MealEntity
import com.cuidadopet.data.db.entity.MealLogEntity
import com.cuidadopet.data.db.entity.MealPlanEntity
import com.cuidadopet.data.db.entity.MedicationEntity
import com.cuidadopet.data.db.entity.MedicationLogEntity
import com.cuidadopet.data.db.entity.PetEntity
import com.cuidadopet.data.db.entity.WaterConfigEntity
import com.cuidadopet.data.db.entity.WaterLogEntity
import com.cuidadopet.data.db.entity.WeightRecordEntity

// @Database é a anotação que define o banco de dados Room.
//
// entities: lista de todas as tabelas do banco — cada classe @Entity vira uma tabela
// version: número da versão do banco. MUITO IMPORTANTE:
//   - Toda vez que você mudar a estrutura de uma tabela (adicionar coluna,
//     renomear, etc.), precisa incrementar este número E criar uma Migration
//     (script que atualiza o banco sem apagar os dados do usuário).
//   - Começamos em 1. Se um dia adicionar uma coluna, vira 2, e assim por diante.
// exportSchema: salva um arquivo JSON com o esquema do banco para controle de versão.
//   Em produção deve ser true — deixamos false por simplicidade no desenvolvimento inicial.
@Database(
    entities = [
        PetEntity::class,
        MedicationEntity::class,
        MedicationLogEntity::class,
        MealPlanEntity::class,
        MealEntity::class,
        MealLogEntity::class,
        WaterConfigEntity::class,
        WaterLogEntity::class,
        HealthEntryEntity::class,
        WeightRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
// RoomDatabase é a classe base que o Room exige.
// abstract porque o Room gera a implementação real — você nunca instancia AppDatabase diretamente.
abstract class AppDatabase : RoomDatabase() {

    // Cada função abstrata aqui "entrega" o DAO correspondente.
    // O Room gera a implementação automaticamente.
    // Você pede o DAO, o Room devolve pronto para usar.
    abstract fun petDao(): PetDao
    abstract fun medicationDao(): MedicationDao
    abstract fun feedingDao(): FeedingDao
    abstract fun waterDao(): WaterDao
    abstract fun healthDao(): HealthDao
}
