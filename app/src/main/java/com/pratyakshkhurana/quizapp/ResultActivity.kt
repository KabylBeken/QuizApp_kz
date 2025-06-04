package com.pratyakshkhurana.quizapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pratyakshkhurana.quizapp.database.QuizDatabase
import com.pratyakshkhurana.quizapp.database.QuizRepository
import com.pratyakshkhurana.quizapp.database.QuizResult
import com.pratyakshkhurana.quizapp.databinding.ActivityResultBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var repository: QuizRepository
    
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем данные из Intent
        val username = intent.getStringExtra("user") ?: ""
        val score = intent.getIntExtra("correct", 0)
        val totalQuestions = intent.getIntExtra("total", 0)
        val category = intent.getStringExtra("category") ?: ""

        // Инициализируем репозиторий
        val quizResultDao = QuizDatabase.getDatabase(this).quizResultDao()
        repository = QuizRepository(quizResultDao)

        // Сохраняем результат в базу данных
        saveResult(username, category, score, totalQuestions)

        // Отображаем результат
        binding.name.text = username
        binding.totalScore.text = "Сіз $score ұпай жинадыңыз ($totalQuestions-нан)"

        // Обработчики кнопок
        binding.btnFinish.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity() // Закрываем все активности в стеке
        }
    }

    private fun saveResult(username: String, category: String, score: Int, totalQuestions: Int) {
        val result = QuizResult(
            userName = username,
            category = category,
            score = score,
            totalQuestions = totalQuestions
        )
        
        lifecycleScope.launch {
            try {
                repository.insertResult(result)
                Toast.makeText(this@ResultActivity, "Результат сохранен", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ResultActivity, "Ошибка при сохранении результата", Toast.LENGTH_SHORT).show()
            }
        }
    }
}