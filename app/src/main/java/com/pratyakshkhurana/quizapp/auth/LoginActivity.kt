package com.pratyakshkhurana.quizapp.auth

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.pratyakshkhurana.quizapp.MainActivity
import com.pratyakshkhurana.quizapp.R
import com.pratyakshkhurana.quizapp.databinding.ActivityLoginBinding
import com.pratyakshkhurana.quizapp.databinding.DialogProfileSetupBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var firestoreRepo: FirestoreRepository
    private lateinit var googleSignInClient: GoogleSignInClient

    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository()
        firestoreRepo = FirestoreRepository()

        // Проверяем, авторизован ли уже пользователь
        authRepository.getCurrentUser()?.let {
            navigateToMainActivity()
            return
        }

        // Настройка входа через Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Настройка слушателей
        setupListeners()
    }

    private fun setupListeners() {
        binding.registerText.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.loginButton.setOnClickListener {
            loginUser()
        }

        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.forgotPasswordText.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Введите email для восстановления пароля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            resetPassword(email)
        }
    }

    private fun loginUser() {
        val email: String = binding.emailEditText.text.toString().trim()
        val password: String = binding.passwordEditText.text.toString().trim()

        when {
            TextUtils.isEmpty(email) -> {
                binding.emailEditText.error = "Email is required"
                return
            }
            TextUtils.isEmpty(password) -> {
                binding.passwordEditText.error = "Password is required"
                return
            }
            else -> {
                binding.progressBar.visibility = View.VISIBLE

                authRepository.loginUser(email, password) { isSuccess, message ->
                    binding.progressBar.visibility = View.GONE
                    
                    if (isSuccess) {
                        // Проверяем существование пользователя в Realtime Database и Firestore
                        checkUserProfileExists()
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun resetPassword(email: String) {
        showLoading(true)
        authRepository.resetPassword(email) { success, message ->
            if (success) {
                Toast.makeText(
                    this@LoginActivity,
                    "Инструкции по сбросу пароля отправлены на вашу почту",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@LoginActivity,
                    "Ошибка: $message",
                    Toast.LENGTH_SHORT
                ).show()
            }
            showLoading(false)
        }
    }

    /**
     * Проверяет наличие профиля пользователя и при необходимости перенаправляет на его создание
     */
    private fun checkUserProfileExists() {
        val currentUser = authRepository.getCurrentUser() ?: return

        // Проверяем наличие профиля в Realtime Database
        FirebaseDatabase.getInstance().getReference("users")
            .child(currentUser.uid)
            .get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    // Профиль уже существует, переходим на главный экран
                    navigateToMainActivity()
                } else {
                    // Проверяем наличие профиля в Firestore
                    lifecycleScope.launch {
                        val profileExists = firestoreRepo.checkUserProfileExists()
                        if (profileExists) {
                            // Профиль в Firestore существует, но нет в Realtime Database
                            // Инициализируем пользователя в Realtime Database
                            try {
                                val result = authRepository.initUserInRealtimeDbIfNeeded()
                                result.fold(
                                    onSuccess = {
                                        // Успешно инициализировали, переходим на главный экран
                                        navigateToMainActivity()
                                    },
                                    onFailure = { e ->
                                        // Если не удалось инициализировать, все равно переходим дальше
                                        Log.e("LoginActivity", "Failed to init user in Realtime DB", e)
                                        navigateToMainActivity()
                                    }
                                )
                            } catch (e: Exception) {
                                // При ошибке все равно переходим на главный экран
                                Log.e("LoginActivity", "Exception during user init", e)
                                navigateToMainActivity()
                            }
                        } else {
                            // Профиля нет ни в Firestore, ни в Realtime Database
                            // Переходим к созданию профиля
                            val intent = Intent(this@LoginActivity, ProfileSetupActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                // При ошибке проверки перенаправляем на создание профиля
                Toast.makeText(
                    this@LoginActivity,
                    "Ошибка проверки профиля: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                
                val intent = Intent(this@LoginActivity, ProfileSetupActivity::class.java)
                startActivity(intent)
                finish()
            }
    }

    /**
     * Показывает диалог создания профиля
     */
    private fun showProfileSetupDialog() {
        val dialog = Dialog(this)
        val dialogBinding = DialogProfileSetupBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        
        // Настраиваем диалог
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        
        // Обработчик кнопки сохранения
        dialogBinding.btnSaveProfile.setOnClickListener {
            val name = dialogBinding.etName.text.toString().trim()
            val surname = dialogBinding.etSurname.text.toString().trim()
            val ageStr = dialogBinding.etAge.text.toString().trim()
            
            // Проверяем введенные данные
            when {
                name.isEmpty() -> {
                    dialogBinding.etName.error = "Атыңызды енгізіңіз"
                    return@setOnClickListener
                }
                surname.isEmpty() -> {
                    dialogBinding.etSurname.error = "Тегіңізді енгізіңіз"
                    return@setOnClickListener
                }
                ageStr.isEmpty() -> {
                    dialogBinding.etAge.error = "Жасыңызды енгізіңіз"
                    return@setOnClickListener
                }
                else -> {
                    val age = ageStr.toInt()
                    // Получаем email из текущего пользователя Firebase Auth
                    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
                    
                    // Создаем профиль пользователя
                    val profile = UserProfile(name, surname, age, email)
                    
                    // Сохраняем профиль в Firestore
                    saveUserProfile(profile)
                    dialog.dismiss()
                }
            }
        }
        
        dialog.show()
    }
    
    /**
     * Сохраняет профиль пользователя в Firestore
     */
    private fun saveUserProfile(profile: UserProfile) {
        lifecycleScope.launch {
            try {
                val success = firestoreRepo.createUserProfile(profile)
                
                if (success) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Профиль сәтті сақталды!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Профильді сақтау кезінде қате орын алды",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Переходим на главный экран
                navigateToMainActivity()
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error saving user profile", e)
                Toast.makeText(
                    this@LoginActivity,
                    "Профильді сақтау кезінде қате орын алды",
                    Toast.LENGTH_SHORT
                ).show()
                
                // В случае ошибки все равно переходим на главный экран
                navigateToMainActivity()
            }
        }
    }
    
    /**
     * Переходит на главный экран
     */
    private fun navigateToMainActivity() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Обрабатываем результат входа через Google
        authRepository.handleGoogleSignInResult(requestCode, resultCode, data) { isSuccess, user, message ->
            if (isSuccess && user != null) {
                // После успешной аутентификации через Google проверяем наличие профиля
                checkUserProfileExists()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
} 