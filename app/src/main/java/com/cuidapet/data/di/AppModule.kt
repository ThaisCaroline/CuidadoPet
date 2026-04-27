package com.cuidadopet.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Módulo Hilt para dependências gerais do app que não são banco de dados
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Fornece o Context do app para classes que precisam dele (ex: AlarmScheduler)
    // O Hilt já sabe injetar Context via @ApplicationContext,
    // mas algumas classes recebem Context diretamente no construtor
    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context = context
}
