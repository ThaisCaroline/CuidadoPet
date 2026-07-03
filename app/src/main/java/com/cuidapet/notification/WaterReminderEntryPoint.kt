package com.cuidadopet.notification

import com.cuidadopet.data.db.dao.WaterDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WaterReminderEntryPoint {
    fun waterDao(): WaterDao
}
