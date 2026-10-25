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

/**
 * Adds the watch's heart rate readings for a session, and for each exercise the stretch of the session it was done in so
 * its heart rate can be found in them; existing rows keep `NULL`.
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `heartRateSamples` BLOB")
        db.execSQL("ALTER TABLE `exercise_performance_records` ADD COLUMN `heartRateStartSeconds` INTEGER")
        db.execSQL("ALTER TABLE `exercise_performance_records` ADD COLUMN `heartRateEndSeconds` INTEGER")
    }
}

/** Adds when each working set of an exercise began and ended; existing rows keep `NULL`. */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `exercise_performance_records` ADD COLUMN `setWindows` TEXT")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18)
