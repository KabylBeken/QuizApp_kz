package com.pratyakshkhurana.quizapp.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Репозиторий для работы с базой данных квизов
 */
class QuizRepository(private val quizResultDao: QuizResultDao) {
    
    /**
     * Получить все результаты квизов
     */
    fun getAllResults(): Flow<List<QuizResult>> {
        return quizResultDao.getAllResults()
    }
    
    /**
     * Получить результаты квизов для определенного пользователя
     */
    fun getResultsByUser(userName: String): Flow<List<QuizResult>> {
        return quizResultDao.getResultsByUser(userName)
    }
    
    /**
     * Получить результаты квизов для определенной категории
     */
    fun getResultsByCategory(category: String): Flow<List<QuizResult>> {
        return quizResultDao.getResultsByCategory(category)
    }
    
    /**
     * Получить лучший результат пользователя
     */
    suspend fun getBestResult(userName: String): QuizResult? {
        return withContext(Dispatchers.IO) {
            quizResultDao.getBestResultNonSuspend(userName)
        }
    }
    
    /**
     * Вставить новый результат
     */
    suspend fun insertResult(quizResult: QuizResult): Long {
        return withContext(Dispatchers.IO) {
            quizResultDao.insertResultNonSuspend(quizResult)
        }
    }
    
    /**
     * Удалить результат
     */
    suspend fun deleteResult(quizResult: QuizResult) {
        withContext(Dispatchers.IO) {
            quizResultDao.deleteResultNonSuspend(quizResult)
        }
    }
    
    /**
     * Удалить все результаты
     */
    suspend fun deleteAllResults() {
        withContext(Dispatchers.IO) {
            quizResultDao.deleteAllResultsNonSuspend()
        }
    }
} 