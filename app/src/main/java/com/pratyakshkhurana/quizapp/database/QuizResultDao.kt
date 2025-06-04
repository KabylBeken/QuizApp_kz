package com.pratyakshkhurana.quizapp.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO интерфейс для работы с результатами квиза
 */
@Dao
interface QuizResultDao {
    
    /**
     * Получить все результаты квизов
     */
    @Query("SELECT * FROM quiz_results ORDER BY date DESC")
    fun getAllResults(): Flow<List<QuizResult>>
    
    /**
     * Получить результаты квизов для определенного пользователя
     */
    @Query("SELECT * FROM quiz_results WHERE user_name = :userName ORDER BY date DESC")
    fun getResultsByUser(userName: String): Flow<List<QuizResult>>
    
    /**
     * Получить результаты квизов для определенной категории
     */
    @Query("SELECT * FROM quiz_results WHERE category = :category ORDER BY date DESC")
    fun getResultsByCategory(category: String): Flow<List<QuizResult>>
    
    /**
     * Получить лучший результат пользователя
     * Используем обычный List вместо suspend функции, чтобы избежать проблем с Room
     */
    @Query("SELECT * FROM quiz_results WHERE user_name = :userName ORDER BY score DESC LIMIT 1")
    fun getBestResultNonSuspend(userName: String): QuizResult?
    
    /**
     * Вставить новый результат
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertResultNonSuspend(quizResult: QuizResult): Long
    
    /**
     * Удалить результат
     */
    @Delete
    fun deleteResultNonSuspend(quizResult: QuizResult)
    
    /**
     * Удалить все результаты
     */
    @Query("DELETE FROM quiz_results")
    fun deleteAllResultsNonSuspend()
} 