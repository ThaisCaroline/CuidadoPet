package com.cuidadopet.data.di

import android.content.Context
import androidx.room.Room
import com.cuidadopet.data.db.AppDatabase
import com.cuidadopet.data.db.MIGRATION_1_2
import com.cuidadopet.data.security.DatabaseKeyManager
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.cuidadopet.data.db.dao.FeedingDao
import com.cuidadopet.data.db.dao.HealthDao
import com.cuidadopet.data.db.dao.MedicationDao
import com.cuidadopet.data.db.dao.PetDao
import com.cuidadopet.data.db.dao.WaterDao
import com.cuidadopet.data.repository.FeedingRepository
import com.cuidadopet.data.repository.WaterRepository
import com.cuidadopet.notification.MealAlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// @Module indica ao Hilt que esta classe é um "manual de instruções":
// ela ensina o Hilt como criar objetos que não podem ser anotados diretamente
// (como o Room, que é uma biblioteca de terceiros).
@Module
// @InstallIn define o "escopo de vida" do módulo.
// SingletonComponent = os objetos criados aqui existem enquanto o app estiver aberto.
// Isso é correto para o banco de dados — queremos uma única instância durante toda a sessão.
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // @Provides ensina ao Hilt como criar o AppDatabase.
    // @Singleton garante que só exista UMA instância do banco em todo o app.
    // Múltiplas instâncias do banco causariam conflitos e inconsistência de dados.
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyManager: DatabaseKeyManager
    ): AppDatabase {
        val passphrase = SQLiteDatabase.getBytes(keyManager.getOrCreateKey())
        val factory    = SupportFactory(passphrase)
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "cuidadopet_database"
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    // As funções abaixo ensinam o Hilt como obter cada DAO a partir do banco.
    // São simples: apenas chamam a função correspondente do AppDatabase.
    // O Hilt injeta o AppDatabase automaticamente (já sabe criar graças ao @Provides acima).

    @Provides
    @Singleton
    fun providePetDao(database: AppDatabase): PetDao = database.petDao()

    @Provides
    @Singleton
    fun provideMedicationDao(database: AppDatabase): MedicationDao = database.medicationDao()

    @Provides
    @Singleton
    fun provideFeedingDao(database: AppDatabase): FeedingDao = database.feedingDao()

    @Provides
    @Singleton
    fun provideWaterDao(database: AppDatabase): WaterDao = database.waterDao()

    @Provides
    @Singleton
    fun provideHealthDao(database: AppDatabase): HealthDao = database.healthDao()

    // MealAlarmScheduler precisa do Context para acessar o AlarmManager do sistema
    @Provides
    @Singleton
    fun provideMealAlarmScheduler(
        @ApplicationContext context: Context
    ): MealAlarmScheduler = MealAlarmScheduler(context)

    // FeedingRepository une o DAO e o scheduler em um único ponto de acesso
    @Provides
    @Singleton
    fun provideFeedingRepository(
        feedingDao: FeedingDao,
        mealAlarmScheduler: MealAlarmScheduler
    ): FeedingRepository = FeedingRepository(feedingDao, mealAlarmScheduler)

    // WaterRepository usa o Context para acessar o WorkManager (lembretes periódicos)
    @Provides
    @Singleton
    fun provideWaterRepository(
        waterDao: WaterDao,
        @ApplicationContext context: Context
    ): WaterRepository = WaterRepository(waterDao, context)
}
