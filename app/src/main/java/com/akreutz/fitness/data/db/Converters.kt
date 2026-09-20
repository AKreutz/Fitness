package com.akreutz.fitness.data.db

import androidx.room.TypeConverter

/** Room type converters for column types it can't persist natively. */
class Converters {
    @TypeConverter
    fun fromIntList(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun toIntList(value: String): List<Int> =
        if (value.isEmpty()) emptyList() else value.split(",").map { it.toInt() }
}
