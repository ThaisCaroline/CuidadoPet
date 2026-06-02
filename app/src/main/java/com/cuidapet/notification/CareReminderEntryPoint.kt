package com.cuidadopet.notification

import com.cuidadopet.data.repository.MedicationRepository
import com.cuidadopet.data.repository.PetRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CareReminderEntryPoint {
    fun petRepository(): PetRepository
    fun medicationRepository(): MedicationRepository
}