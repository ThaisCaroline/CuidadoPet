package com.cuidadopet.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE medications ADD COLUMN reminder_enabled INTEGER NOT NULL DEFAULT 1"
        )
    }
}
