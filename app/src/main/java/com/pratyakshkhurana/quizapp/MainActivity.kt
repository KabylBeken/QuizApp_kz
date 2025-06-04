package com.pratyakshkhurana.quizapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pratyakshkhurana.quizapp.auth.AuthRepository
import com.pratyakshkhurana.quizapp.auth.LoginActivity
import com.pratyakshkhurana.quizapp.auth.ProfileActivity
import com.pratyakshkhurana.quizapp.auth.User
import com.pratyakshkhurana.quizapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var authRepository: AuthRepository
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository()

        // Проверяем и инициализируем данные пользователя при запуске
        checkAndInitUserData()

        // Настраиваем обработку нажатия кнопки "назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Перемещаем задачу в фоновый режим при нажатии "назад"
                moveTaskToBack(true)
            }
        })

        binding.buttonStart.setOnClickListener {
            // Если пользователь авторизован, используем его имя
            // Иначе берем имя из поля ввода
            val userNameToPass = userName ?: binding.enterNameEditText.text.toString()
            
            if (userNameToPass.isEmpty()) {
                Toast.makeText(this, "Өтінемін, атыңызды енгізіңіз!", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, QuizCategories::class.java)
                intent.putExtra("user", userNameToPass)
                startActivity(intent)
            }
        }
        
        // Добавляем кнопку перехода на экран истории
        binding.buttonHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        // Добавляем кнопку профиля
        binding.profileButton.setOnClickListener {
            val currentUser = authRepository.getCurrentUser()
            
            if (currentUser != null) {
                // Пользователь авторизован, переходим на экран профиля
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            } else {
                // Пользователь не авторизован, переходим на экран входа
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    /**
     * Проверяет и инициализирует данные пользователя при необходимости
     */
    private fun checkAndInitUserData() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            Log.d("MainActivity", "Checking user data for: ${currentUser.uid}")
            
            lifecycleScope.launch {
                try {
                    val initResult = authRepository.initUserInRealtimeDbIfNeeded()
                    initResult.fold(
                        onSuccess = {
                            Log.d("MainActivity", "User data initialized successfully")
                            // Получаем данные пользователя и настраиваем UI
                            getUserDataAndUpdateUI(currentUser.uid)
                        },
                        onFailure = { e ->
                            Log.e("MainActivity", "Failed to initialize user data", e)
                        }
                    )
                } catch (e: Exception) {
                    Log.e("MainActivity", "Exception during user data initialization", e)
                }
            }
        }
    }

    /**
     * Получает данные пользователя и обновляет интерфейс
     */
    private fun getUserDataAndUpdateUI(uid: String) {
        lifecycleScope.launch {
            try {
                val userDataResult = authRepository.getUserData(uid)
                userDataResult.fold(
                    onSuccess = { user ->
                        // Сохраняем имя пользователя
                        userName = user.name
                        
                        // Обновляем UI для зарегистрированного пользователя
                        updateUIForLoggedInUser(user)
                    },
                    onFailure = { e ->
                        Log.e("MainActivity", "Error getting user data", e)
                    }
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Exception while getting user data", e)
            }
        }
    }

    /**
     * Обновляет интерфейс для авторизованного пользователя
     */
    private fun updateUIForLoggedInUser(user: User) {
        // Скрываем поле ввода имени, так как мы уже знаем пользователя
        binding.enterNameEditText.visibility = View.GONE
        binding.enterNameTextView.visibility = View.GONE
        binding.nameInputLayout.visibility = View.GONE
        
        // Выбираем случайное приветствие
        val welcomeMessages = listOf(
            "🌟 Қош келдің, данышпан ${user.name}!",
            "🧠 Білім жолындағы тағы бір шайқас басталмақ!",
            "🎯 Мақсат – 100%! Дайынсың ба, ${user.name}?"
        )
        
        // Устанавливаем случайное приветствие
        val randomMessage = welcomeMessages[Random.nextInt(welcomeMessages.size)]
        binding.welcomeTextView.text = randomMessage
        
        // Обновляем текст на кнопке
        binding.buttonStart.text = "БАСТАУ"
    }
}

