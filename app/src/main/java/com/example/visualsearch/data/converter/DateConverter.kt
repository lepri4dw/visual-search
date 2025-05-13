package com.example.visualsearch.data.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Конвертер для работы с типом Date в Room
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}