package com.pratyakshkhurana.quizapp.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.pratyakshkhurana.quizapp.HistoryActivity
import com.pratyakshkhurana.quizapp.MainActivity
import com.pratyakshkhurana.quizapp.R
import com.pratyakshkhurana.quizapp.databinding.ActivityProfileBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var authRepository: AuthRepository
    private var currentUser: User? = null

    private val PICK_IMAGE_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository()

        setupListeners()
        loadUserData()
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener {
            finish()
        }

        binding.editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }

        binding.changePhotoButton.setOnClickListener {
            openGallery()
        }

        binding.changePasswordLayout.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.historyLayout.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.logoutLayout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun loadUserData() {
        val currentFirebaseUser = authRepository.getCurrentUser()
        if (currentFirebaseUser == null) {
            // Пользователь не авторизован, перенаправляем на экран входа
            Log.e("ProfileActivity", "Current Firebase user is null")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d("ProfileActivity", "Loading user data for UID: ${currentFirebaseUser.uid}")
        showLoading(true)
        
        // Выводим для отладки данные текущего пользователя Firebase
        Log.d("ProfileActivity", "Firebase user details: email=${currentFirebaseUser.email}, " +
                "displayName=${currentFirebaseUser.displayName}, " +
                "isEmailVerified=${currentFirebaseUser.isEmailVerified}, " +
                "photoUrl=${currentFirebaseUser.photoUrl}")
                
        lifecycleScope.launch {
            try {
                val result = authRepository.getUserData(currentFirebaseUser.uid)
                result.fold(
                    onSuccess = { user ->
                        Log.d("ProfileActivity", "User data loaded successfully: ${user.name}, ${user.email}")
                        currentUser = user
                        updateUI(user)
                        showLoading(false)
                    },
                    onFailure = { exception ->
                        Log.e("ProfileActivity", "Failed to load user data", exception)
                        Toast.makeText(
                            this@ProfileActivity,
                            "Ошибка загрузки данных: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        showLoading(false)
                        
                        // Попробуем инициализировать пользователя, если не удалось загрузить
                        tryInitUser(currentFirebaseUser.uid)
                    }
                )
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Exception during loadUserData", e)
                Toast.makeText(
                    this@ProfileActivity,
                    "Произошла ошибка при загрузке данных профиля",
                    Toast.LENGTH_SHORT
                ).show()
                showLoading(false)
            }
        }
    }

    /**
     * Пытается инициализировать пользователя, если не удалось загрузить данные
     */
    private fun tryInitUser(uid: String) {
        Log.d("ProfileActivity", "Trying to initialize user data in Realtime DB")
        lifecycleScope.launch {
            try {
                val result = authRepository.initUserInRealtimeDbIfNeeded()
                result.fold(
                    onSuccess = {
                        Log.d("ProfileActivity", "Successfully initialized user, reloading data")
                        loadUserData() // Перезагружаем данные после инициализации
                    },
                    onFailure = { e ->
                        Log.e("ProfileActivity", "Failed to initialize user", e)
                        // Если не удалось инициализировать через стандартный метод, 
                        // пробуем загрузить напрямую из Firestore
                        tryLoadFromFirestoreDirectly(uid)
                    }
                )
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Exception during initialization", e)
                // Пробуем загрузить из Firestore напрямую
                tryLoadFromFirestoreDirectly(uid)
            }
        }
    }
    
    /**
     * Пробует загрузить данные напрямую из Firestore в случае проблем
     */
    private fun tryLoadFromFirestoreDirectly(uid: String) {
        Log.d("ProfileActivity", "Trying to load user data directly from Firestore")
        lifecycleScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val documentSnapshot = withContext(Dispatchers.IO) {
                    firestore.collection("users")
                        .document(uid)
                        .get()
                        .await()
                }
                
                if (documentSnapshot.exists()) {
                    val userProfile = documentSnapshot.toObject(UserProfile::class.java)
                    if (userProfile != null) {
                        Log.d("ProfileActivity", "Successfully loaded user from Firestore: ${userProfile.name} ${userProfile.surname}")
                        
                        // Создаем объект User из данных из Firestore
                        val user = User(
                            uid = uid,
                            name = "${userProfile.name} ${userProfile.surname}",
                            email = userProfile.email,
                            photoUrl = null,
                            quizzesTaken = 0,
                            averageScore = 0f,
                            bestCategory = null
                        )
                        
                        // Обновляем UI
                        currentUser = user
                        updateUI(user)
                        
                        // Сохраняем данные в Realtime Database для будущего использования
                        try {
                            Log.d("ProfileActivity", "Saving user data to Realtime Database")
                            withContext(Dispatchers.IO) {
                                FirebaseDatabase.getInstance()
                                    .getReference("users")
                                    .child(uid)
                                    .setValue(user)
                                    .await()
                            }
                            Log.d("ProfileActivity", "Successfully saved user data to Realtime Database")
                        } catch (e: Exception) {
                            Log.e("ProfileActivity", "Failed to save user data to Realtime DB", e)
                        }
                    } else {
                        Log.e("ProfileActivity", "Failed to convert Firestore document to UserProfile")
                        Toast.makeText(
                            this@ProfileActivity, 
                            "Не удалось загрузить данные профиля из Firestore", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("ProfileActivity", "User document doesn't exist in Firestore")
                    Toast.makeText(
                        this@ProfileActivity, 
                        "Профиль пользователя не найден в Firestore", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileActivity", "Exception during direct Firestore loading", e)
                Toast.makeText(
                    this@ProfileActivity, 
                    "Ошибка при загрузке данных из Firestore: ${e.message}", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateUI(user: User) {
        binding.userName.text = user.name
        binding.userEmail.text = user.email
        binding.quizzesTaken.text = user.quizzesTaken.toString()
        binding.averageScore.text = "${user.averageScore.toInt()}%"

        // Загрузка изображения профиля
        if (!user.photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(user.photoUrl)
                .placeholder(R.drawable.profile_placeholder)
                .circleCrop()
                .into(binding.profileImage)
        }
    }

    private fun showEditProfileDialog() {
        val user = currentUser ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val nameEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.nameEditText)
        nameEditText.setText(user.name)

        AlertDialog.Builder(this)
            .setTitle("Редактировать профиль")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val newName = nameEditText.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(this, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateUserProfile(user.copy(name = newName))
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val newPasswordEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.newPasswordEditText)
        val confirmPasswordEditText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.confirmPasswordEditText)

        AlertDialog.Builder(this)
            .setTitle("Изменить пароль")
            .setView(dialogView)
            .setPositiveButton("Изменить") { dialog, _ ->
                val newPassword = newPasswordEditText.text.toString().trim()
                val confirmPassword = confirmPasswordEditText.text.toString().trim()

                if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 6) {
                    Toast.makeText(this, "Пароль должен содержать минимум 6 символов", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updatePassword(newPassword)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход")
            .setMessage("Вы действительно хотите выйти из профиля?")
            .setPositiveButton("Да") { dialog, _ ->
                logoutUser()
                dialog.dismiss()
            }
            .setNegativeButton("Нет") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            imageUri?.let { uploadProfileImage(it) }
        }
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val currentFirebaseUser = authRepository.getCurrentUser() ?: return
        
        showLoading(true)
        lifecycleScope.launch {
            val result = authRepository.updateProfilePhoto(currentFirebaseUser.uid, imageUri)
            result.fold(
                onSuccess = { url ->
                    // Обновляем UI с новым изображением
                    Glide.with(this@ProfileActivity)
                        .load(url)
                        .circleCrop()
                        .into(binding.profileImage)
                    
                    // Обновляем модель пользователя
                    currentUser = currentUser?.copy(photoUrl = url)
                    
                    Toast.makeText(
                        this@ProfileActivity,
                        "Фотография профиля обновлена",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    showLoading(false)
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка обновления фото: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showLoading(false)
                }
            )
        }
    }

    private fun updateUserProfile(updatedUser: User) {
        showLoading(true)
        lifecycleScope.launch {
            val result = authRepository.updateUserProfile(updatedUser)
            result.fold(
                onSuccess = {
                    currentUser = updatedUser
                    updateUI(updatedUser)
                    Toast.makeText(
                        this@ProfileActivity,
                        "Профиль успешно обновлен",
                        Toast.LENGTH_SHORT
                    ).show()
                    showLoading(false)
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка обновления профиля: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showLoading(false)
                }
            )
        }
    }

    private fun updatePassword(newPassword: String) {
        showLoading(true)
        lifecycleScope.launch {
            val result = authRepository.updatePassword(newPassword)
            result.fold(
                onSuccess = {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Пароль успешно обновлен",
                        Toast.LENGTH_SHORT
                    ).show()
                    showLoading(false)
                },
                onFailure = { exception ->
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка обновления пароля: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showLoading(false)
                }
            )
        }
    }

    private fun logoutUser() {
        authRepository.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
} 