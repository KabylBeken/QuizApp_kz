package com.pratyakshkhurana.quizapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class Splash_Screen : AppCompatActivity() {
    private var sharedPreferences: SharedPreferences? = null
    private lateinit var firebaseQuizRepository: FirebaseQuizRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        
        firebaseQuizRepository = FirebaseQuizRepository()
        sharedPreferences = applicationContext.getSharedPreferences("pref", Context.MODE_PRIVATE)
        
        // Инициализация данных в Firebase при первой установке
        val isDbInitialized = sharedPreferences!!.getBoolean("isDbInitialized", false)
        
        if (!isDbInitialized) {
            initializeFirebaseData()
        } else {
            proceedToNextScreen()
        }
    }
    
    private fun initializeFirebaseData() {
        lifecycleScope.launch {
            try {
                // Инициализация базы данных вопросами
                firebaseQuizRepository.initializeQuizData()
                
                // Сохраняем флаг, что база данных инициализирована
                sharedPreferences!!.edit().putBoolean("isDbInitialized", true).apply()
                
                Log.d("Splash_Screen", "Firebase data initialized successfully")
                proceedToNextScreen()
            } catch (e: Exception) {
                Log.e("Splash_Screen", "Error initializing Firebase data: ${e.message}")
                // Даже в случае ошибки продолжаем работу приложения
                proceedToNextScreen()
            }
        }
    }
    
    private fun proceedToNextScreen() {
        Handler().postDelayed({
            if (sharedPreferences!!.getBoolean("isFirstTimeRun", false)) {
                startActivity(Intent(applicationContext, MainActivity::class.java))
            } else {
                startActivity(Intent(applicationContext, OnBoardingScreen::class.java))
            }
            finish()
        }, 1500)
    }
}