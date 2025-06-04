package com.pratyakshkhurana.quizapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

/**
 * Сущность для хранения результатов квиза
 */
@Entity(tableName = "quiz_results")
data class QuizResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "user_name")
    val userName: String,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "score")
    val score: Int,
    
    @ColumnInfo(name = "total_questions")
    val totalQuestions: Int,
    
    @ColumnInfo(name = "date")
    val date: Long = System.currentTimeMillis()
) 