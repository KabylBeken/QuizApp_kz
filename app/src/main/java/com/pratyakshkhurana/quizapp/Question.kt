package com.pratyakshkhurana.quizapp

data class Question(
    val id: Int = 0,
    val question: String = "",
    val option1: String = "",
    val option2: String = "",
    val option3: String = "",
    val option4: String = "",
    val correctOptionIndex: Int = 0
) {
    // Пустой конструктор для Firebase
    constructor() : this(0, "", "", "", "", "", 0)
} 