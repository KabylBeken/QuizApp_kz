package com.pratyakshkhurana.quizapp

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize

/**
 * Класс инициализации Firebase для приложения.
 * Добавьте его в AndroidManifest.xml в секцию <application>:
 * android:name=".FirebaseInitializer"
 */
class FirebaseInitializer : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализация Firebase
        FirebaseApp.initializeApp(this)
    }
} 