package com.akreutz.fitness.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the optional per-exercise bar weight; existing rows keep `NULL`, meaning the standard bar. */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `barWeightKg` REAL")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_14_15)
