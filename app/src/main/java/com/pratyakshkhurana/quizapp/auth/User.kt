package com.pratyakshkhurana.quizapp.auth

/**
 * Модель данных пользователя
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val quizzesTaken: Int = 0,
    val averageScore: Float = 0f,
    val bestCategory: String? = null
) {
    // Пустой конструктор для Firebase
    constructor() : this("", "", "")
    
    // Копирование объекта с обновлением полей
    fun withUpdatedStats(quizzesTaken: Int, averageScore: Float, bestCategory: String?): User {
        return copy(
            quizzesTaken = quizzesTaken,
            averageScore = averageScore,
            bestCategory = bestCategory
        )
    }
} 