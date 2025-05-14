package com.example.visualsearch.data.converter

import androidx.room.TypeConverter
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Конвертер для работы с типом Date и Timestamp в Room
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
    
    @TypeConverter
    fun fromFirebaseTimestamp(timestamp: Timestamp?): Long? {
        return timestamp?.seconds?.times(1000)
    }
    
    @TypeConverter
    fun toFirebaseTimestamp(value: Long?): Timestamp? {
        return if (value != null) {
            Timestamp(value / 1000, 0)
        } else {
            null
        }
    }
}