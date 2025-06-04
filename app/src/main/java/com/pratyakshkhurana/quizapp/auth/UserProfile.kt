package com.pratyakshkhurana.quizapp.auth

/**
 * Модель данных профиля пользователя
 */
data class UserProfile(
    val name: String = "",
    val surname: String = "",
    val age: Int = 0,
    val email: String = ""
) {
    // Пустой конструктор для Firestore
    constructor() : this("", "", 0, "")
} 