package com.zixo.app.data.local.room

import androidx.room.TypeConverter
import timber.log.Timber

/**
 * Room TypeConverters for complex data types that cannot be stored
 * as primitive columns. Converts Lists and Maps to/from JSON strings.
 */
class ZixoTypeConverters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return try {
            value?.joinToString(separator = ",") { it }
        } catch (e: Exception) {
            Timber.e(e, "TypeConverter: Failed to convert String list")
            null
        }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return try {
            value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            Timber.e(e, "TypeConverter: Failed to parse String list")
            emptyList()
        }
    }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return try {
            value?.joinToString(separator = ",") { it.toString() }
        } catch (e: Exception) {
            Timber.e(e, "TypeConverter: Failed to convert Long list")
            null
        }
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        return try {
            value?.split(",")?.map { it.trim().toLong() } ?: emptyList()
        } catch (e: Exception) {
            Timber.e(e, "TypeConverter: Failed to parse Long list")
            emptyList()
        }
    }
}
