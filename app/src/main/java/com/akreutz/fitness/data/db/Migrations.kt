package com.akreutz.fitness.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the optional per-exercise bar weight; existing rows keep `NULL`, meaning the standard bar. */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `exercises` ADD COLUMN `barWeightKg` REAL")
    }
}

/** Adds the heart rate the watch measured, per performance and per session; existing rows keep `NULL`. */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `exercise_performance_records` ADD COLUMN `avgHeartRateBpm` INTEGER")
        db.execSQL("ALTER TABLE `exercise_performance_records` ADD COLUMN `maxHeartRateBpm` INTEGER")
        db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `avgHeartRateBpm` INTEGER")
        db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `maxHeartRateBpm` INTEGER")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_14_15, MIGRATION_15_16)
