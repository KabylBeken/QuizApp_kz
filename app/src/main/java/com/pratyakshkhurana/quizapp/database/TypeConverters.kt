package com.pratyakshkhurana.quizapp.database

import androidx.room.TypeConverter
import java.util.Date

/**
 * Конвертеры типов для Room Database
 */
class Converters {
    /**
     * Конвертирует дату в формат Long для хранения в базе данных
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Конвертирует Long в Date при получении из базы данных
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
} 