package com.pratyakshkhurana.quizapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * База данных приложения QuizApp
 */
@Database(entities = [QuizResult::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class QuizDatabase : RoomDatabase() {
    
    /**
     * DAO для работы с результатами квиза
     */
    abstract fun quizResultDao(): QuizResultDao
    
    companion object {
        // Singleton для предотвращения создания нескольких экземпляров базы данных
        @Volatile
        private var INSTANCE: QuizDatabase? = null
        
        /**
         * Получить экземпляр базы данных
         */
        fun getDatabase(context: Context): QuizDatabase {
            // если экземпляр базы данных уже существует, вернуть его
            // в противном случае создать новый экземпляр
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuizDatabase::class.java,
                    "quiz_database"
                )
                .fallbackToDestructiveMigration() // при изменении версии базы данных, уничтожить старую и создать новую
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
} 