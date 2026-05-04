package com.cuidadopet.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS health_photos (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                petId INTEGER NOT NULL,
                entryDate INTEGER NOT NULL,
                filePath TEXT NOT NULL,
                caption TEXT NOT NULL DEFAULT '',
                FOREIGN KEY (petId) REFERENCES pets(id) ON DELETE CASCADE
            )
        """)
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_health_photos_petId ON health_photos(petId)"
        )
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE medications ADD COLUMN reminder_enabled INTEGER NOT NULL DEFAULT 1"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE meals ADD COLUMN quantityUnit TEXT NOT NULL DEFAULT 'g'"
        )
    }
}
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE water_configs ADD COLUMN reminderStartTime TEXT NOT NULL DEFAULT '08:00'"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE sporadic_meal_logs ADD COLUMN amountUnit TEXT NOT NULL DEFAULT 'g'"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {                                                                                                                                                                                 database.execSQL("""
              CREATE TABLE IF NOT EXISTS sporadic_meal_logs (                                                                                                                                                                       
                  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,                                                                                                                                                                    
                  petId INTEGER NOT NULL,
                  description TEXT,                                                                                                                                                                                                 
                  amountGrams REAL,                                                                                                                                                                                                 
                  registeredAt INTEGER NOT NULL,                                                                                                                                                                                    
                  FOREIGN KEY (petId) REFERENCES pets(id) ON DELETE CASCADE                                                                                                                                                         
              )                                                                                                                                                                                                                     
          """)
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_sporadic_meal_logs_petId ON sporadic_meal_logs(petId)"
        )
    }
}
