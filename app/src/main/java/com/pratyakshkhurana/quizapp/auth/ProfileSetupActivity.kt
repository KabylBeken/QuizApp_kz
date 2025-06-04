package com.pratyakshkhurana.quizapp.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.pratyakshkhurana.quizapp.MainActivity
import com.pratyakshkhurana.quizapp.R
import com.pratyakshkhurana.quizapp.databinding.DialogProfileSetupBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileSetupActivity : AppCompatActivity() {
    private lateinit var binding: DialogProfileSetupBinding
    private lateinit var firestoreRepo: FirestoreRepository
    private lateinit var authRepository: AuthRepository
    private val TAG = "ProfileSetupActivity"
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogProfileSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreRepo = FirestoreRepository()
        authRepository = AuthRepository()

        // Применяем анимации с задержкой
        animateUI()

        // Проверяем существует ли профиль пользователя
        checkUserProfile()

        // Обработчик кнопки сохранения
        binding.btnSaveProfile.setOnClickListener {
            saveProfileWithAnimation()
        }
    }
    
    private fun animateUI() {
        // Загружаем анимации
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in_with_delay)
        val buttonAnim = AnimationUtils.loadAnimation(this, R.anim.button_pulse)
        
        // Применяем анимации с задержкой для плавного появления
        binding.profileIcon.alpha = 1f
        binding.profileIcon.startAnimation(fadeIn)
        
        handler.postDelayed({
            binding.titleText.alpha = 1f
            binding.titleText.startAnimation(fadeIn)
        }, 150)
        
        handler.postDelayed({
            binding.nameInputLayout.alpha = 1f
            binding.nameInputLayout.startAnimation(fadeIn)
        }, 300)
        
        handler.postDelayed({
            binding.surnameInputLayout.alpha = 1f
            binding.surnameInputLayout.startAnimation(fadeIn)
        }, 450)
        
        handler.postDelayed({
            binding.ageInputLayout.alpha = 1f
            binding.ageInputLayout.startAnimation(fadeIn)
        }, 600)
        
        handler.postDelayed({
            binding.btnSaveProfile.alpha = 1f
            binding.btnSaveProfile.startAnimation(buttonAnim)
        }, 750)
    }

    private fun saveProfileWithAnimation() {
        // Анимация нажатия кнопки
        val scaleAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.btnSaveProfile.startAnimation(scaleAnim)
        
        // Вызываем основной метод сохранения с небольшой задержкой
        handler.postDelayed({ saveProfile() }, 100)
    }

    private fun checkUserProfile() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Checking if user profile exists")
                val profileExists = firestoreRepo.checkUserProfileExists()
                
                if (profileExists) {
                    // Профиль уже существует, загружаем его
                    Log.d(TAG, "Profile exists in Firestore, loading it")
                    val profile = firestoreRepo.getUserProfile()
                    
                    if (profile != null) {
                        Log.d(TAG, "Profile loaded successfully from Firestore: ${profile.name} ${profile.surname}")
                        Toast.makeText(
                            this@ProfileSetupActivity,
                            "Қош келдіңіз, ${profile.name} ${profile.surname}!",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Также проверим Realtime Database и синхронизируем данные если нужно
                        syncProfileToRealtimeDb(profile)
                        
                        // Переходим на главный экран
                        navigateToMainActivity()
                    } else {
                        Log.e(TAG, "Profile exists in Firestore but failed to load")
                        Toast.makeText(
                            this@ProfileSetupActivity,
                            "Профильді жүктеу кезінде қате орын алды",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.d(TAG, "Profile doesn't exist in Firestore, showing input form")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking user profile", e)
                Toast.makeText(
                    this@ProfileSetupActivity,
                    "Профильді тексеру кезінде қате орын алды: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().trim()
        val surname = binding.etSurname.text.toString().trim()
        val ageStr = binding.etAge.text.toString().trim()
        
        // Проверяем введенные данные
        when {
            name.isEmpty() -> {
                binding.etName.error = "Атыңызды енгізіңіз"
                return
            }
            surname.isEmpty() -> {
                binding.etSurname.error = "Тегіңізді енгізіңіз"
                return
            }
            ageStr.isEmpty() -> {
                binding.etAge.error = "Жасыңызды енгізіңіз"
                return
            }
            else -> {
                // Показываем индикатор загрузки
                binding.btnSaveProfile.isEnabled = false
                binding.btnSaveProfile.text = "Жүктеу..."
                
                val age = ageStr.toInt()
                // Получаем email из текущего пользователя Firebase Auth
                val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                
                // Создаем профиль пользователя для Firestore
                val firestoreProfile = UserProfile(name, surname, age, email)
                
                // Создаем профиль пользователя для Realtime Database
                val realtimeDbProfile = User(
                    uid = uid,
                    name = "$name $surname",
                    email = email,
                    photoUrl = null,
                    quizzesTaken = 0,
                    averageScore = 0f,
                    bestCategory = null
                )
                
                // Сохраняем профиль в Firestore и Realtime Database
                lifecycleScope.launch {
                    try {
                        // Сохраняем в Firestore
                        val firestoreSuccess = firestoreRepo.createUserProfile(firestoreProfile)
                        
                        // Сохраняем в Realtime Database
                        var realtimeDbSuccess = false
                        try {
                            withContext(Dispatchers.IO) {
                                usersRef.child(uid).setValue(realtimeDbProfile).await()
                                realtimeDbSuccess = true
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving profile to Realtime Database", e)
                        }
                        
                        if (firestoreSuccess || realtimeDbSuccess) {
                            Toast.makeText(
                                this@ProfileSetupActivity,
                                "Профиль сәтті сақталды!",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Переходим на главный экран с анимацией
                            val fadeOut = AnimationUtils.loadAnimation(this@ProfileSetupActivity, R.anim.fade_out)
                            binding.root.startAnimation(fadeOut)
                            
                            handler.postDelayed({
                                navigateToMainActivity()
                            }, 400)
                        } else {
                            Toast.makeText(
                                this@ProfileSetupActivity,
                                "Профильді сақтау кезінде қате орын алды",
                                Toast.LENGTH_SHORT
                            ).show()
                            binding.btnSaveProfile.isEnabled = true
                            binding.btnSaveProfile.text = "Сақтау"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving user profile", e)
                        Toast.makeText(
                            this@ProfileSetupActivity,
                            "Профильді сақтау кезінде қате орын алды: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.btnSaveProfile.isEnabled = true
                        binding.btnSaveProfile.text = "Сақтау"
                    }
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Синхронизирует данные профиля из Firestore в Realtime Database
     */
    private suspend fun syncProfileToRealtimeDb(profile: UserProfile) {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            
            Log.d(TAG, "Syncing profile from Firestore to Realtime Database")
            
            // Проверяем существование записи в Realtime Database
            val dataSnapshot = withContext(Dispatchers.IO) {
                database.getReference("users").child(uid).get().await()
            }
            
            if (!dataSnapshot.exists()) {
                Log.d(TAG, "User not found in Realtime Database, creating entry")
                
                // Конвертируем данные из Firestore в формат для Realtime Database
                val user = User(
                    uid = uid,
                    name = "${profile.name} ${profile.surname}",
                    email = profile.email,
                    photoUrl = null,
                    quizzesTaken = 0,
                    averageScore = 0f,
                    bestCategory = null
                )
                
                // Сохраняем в Realtime Database
                withContext(Dispatchers.IO) {
                    usersRef.child(uid).setValue(user).await()
                }
                
                Log.d(TAG, "Successfully synced profile to Realtime Database")
            } else {
                Log.d(TAG, "User already exists in Realtime Database, skipping sync")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing profile to Realtime Database", e)
        }
    }
} 