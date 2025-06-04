package com.pratyakshkhurana.quizapp.auth

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pratyakshkhurana.quizapp.MainActivity
import com.pratyakshkhurana.quizapp.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository()

        setupListeners()
    }

    private fun setupListeners() {
        binding.registerButton.setOnClickListener {
            registerUser()
        }

        binding.loginText.setOnClickListener {
            onBackPressed()
        }
    }

    private fun registerUser() {
        val email: String = binding.emailEditText.text.toString().trim { it <= ' ' }
        val password: String = binding.passwordEditText.text.toString().trim { it <= ' ' }

        when {
            TextUtils.isEmpty(email) -> {
                binding.emailEditText.error = "Электрондық пошта қажет"
                return
            }
            TextUtils.isEmpty(password) -> {
                binding.passwordEditText.error = "Пароль қажет"
                return
            }
            else -> {
                binding.progressBar.visibility = View.VISIBLE

                authRepository.registerUser(email, password) { isSuccess, message ->
                    binding.progressBar.visibility = View.GONE
                    
                    if (isSuccess) {
                        // После успешной регистрации переходим к настройке профиля
                        startActivity(Intent(this, ProfileSetupActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
} 