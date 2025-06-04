package com.pratyakshkhurana.quizapp.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import com.google.firebase.firestore.FirebaseFirestore

const val RC_SIGN_IN = 9001

/**
 * Репозиторий для работы с аутентификацией
 */
class AuthRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersRef = database.getReference("users")
    private val TAG = "AuthRepository"

    /**
     * Создание пользователя с email и паролем
     */
    fun registerUser(
        email: String,
        password: String,
        callback: (Boolean, String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Регистрация успешна, пользователь автоматически вошел в систему
                    val user = auth.currentUser
                    Log.d(TAG, "createUserWithEmail:success")
                    
                    // После регистрации нужно настроить профиль
                    navigateToProfileSetup(task.result?.user, callback)
                } else {
                    // Ошибка при регистрации
                    val errorMessage = task.exception?.message ?: "Registration failed"
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    callback(false, errorMessage)
                }
            }
    }

    /**
     * Вход пользователя с email и паролем
     */
    fun loginUser(
        email: String,
        password: String,
        callback: (Boolean, String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Вход успешен
                    val user = auth.currentUser
                    Log.d(TAG, "signInWithEmail:success")
                    
                    // Проверяем наличие профиля
                    navigateToProfileSetup(user, callback)
                } else {
                    // Ошибка при входе
                    val errorMessage = task.exception?.message ?: "Authentication failed"
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    callback(false, errorMessage)
                }
            }
    }

    /**
     * Получение текущего пользователя
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Выход пользователя
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Отправка письма для сброса пароля
     */
    fun resetPassword(email: String, callback: (Boolean, String) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Email sent for password reset")
                    callback(true, "Password reset email sent")
                } else {
                    val errorMessage = task.exception?.message ?: "Failed to send reset email"
                    Log.w(TAG, "resetPassword:failure", task.exception)
                    callback(false, errorMessage)
                }
            }
    }

    /**
     * Настройка клиента Google Sign-In
     */
    fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(com.pratyakshkhurana.quizapp.R.aslan.islam@narxoz.kzstring.default_web_client_id))
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(activity, gso)
    }

    /**
     * Запуск процесса входа через Google
     */
    fun startGoogleSignIn(activity: Activity) {
        val googleSignInClient = getGoogleSignInClient(activity)
        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    /**
     * Обработка результата входа через Google
     */
    fun handleGoogleSignInResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        callback: (Boolean, FirebaseUser?, String) -> Unit
    ) {
        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Google sign in succeeded")
                firebaseAuthWithGoogle(account, callback)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                callback(false, null, "Google sign in failed: ${e.message}")
            }
        }
    }

    /**
     * Аутентификация с Firebase используя учетные данные Google
     */
    private fun firebaseAuthWithGoogle(
        account: GoogleSignInAccount,
        callback: (Boolean, FirebaseUser?, String) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Вход успешен
                    val user = auth.currentUser
                    Log.d(TAG, "signInWithCredential:success")
                    
                    // Проверяем наличие профиля
                    navigateToProfileSetup(user) { success, message ->
                        callback(success, user, message)
                    }
                } else {
                    // Ошибка при входе
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    val errorMessage = task.exception?.message ?: "Authentication failed"
                    callback(false, null, errorMessage)
                }
            }
    }
    
    /**
     * Проверяет наличие профиля пользователя и направляет к его настройке при необходимости
     */
    private fun navigateToProfileSetup(
        user: FirebaseUser?, 
        callback: (Boolean, String) -> Unit
    ) {
        if (user != null) {
            callback(true, "Authentication successful")
        } else {
            callback(false, "Authentication failed: user is null")
        }
    }

    /**
     * Получить данные пользователя из Firebase Realtime Database
     * При отсутствии в Realtime Database попробует получить из Firestore
     */
    suspend fun getUserData(uid: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                // Сначала пытаемся получить данные из Realtime Database
                val dataSnapshot = usersRef.child(uid).get().await()
                
                if (dataSnapshot.exists()) {
                    // Если данные есть в Realtime Database, возвращаем их
                    val userData = dataSnapshot.getValue(User::class.java)
                        ?: return@withContext Result.failure(Exception("Пользователь не найден"))
                    return@withContext Result.success(userData)
                } else {
                    Log.d(TAG, "User not found in Realtime Database, trying Firestore")
                    
                    // Если данных нет в Realtime Database, пробуем получить из Firestore
                    try {
                        // Получаем FireStore database instance
                        val firestore = FirebaseFirestore.getInstance()
                        
                        // Получаем данные из Firestore
                        val firestoreDocument = firestore.collection("users")
                            .document(uid)
                            .get()
                            .await()
                            
                        if (firestoreDocument.exists()) {
                            // Преобразуем данные из Firestore в модель User
                            val userProfile = firestoreDocument.toObject(UserProfile::class.java)
                            
                            if (userProfile != null) {
                                Log.d(TAG, "User found in Firestore: ${userProfile.name} ${userProfile.surname}")
                                
                                // Создаем объект User из данных профиля
                                val user = User(
                                    uid = uid,
                                    name = "${userProfile.name} ${userProfile.surname}",
                                    email = userProfile.email,
                                    photoUrl = null,
                                    quizzesTaken = 0,
                                    averageScore = 0f,
                                    bestCategory = null
                                )
                                
                                // Сохраняем данные в Realtime Database для будущего использования
                                usersRef.child(uid).setValue(user).await()
                                
                                return@withContext Result.success(user)
                            }
                        }
                        
                        return@withContext Result.failure(Exception("Пользователь не найден ни в Realtime Database, ни в Firestore"))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting user from Firestore", e)
                        return@withContext Result.failure(e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user data", e)
                return@withContext Result.failure(e)
            }
        }
    }

    /**
     * Обновить данные пользователя
     */
    suspend fun updateUserProfile(user: User): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                usersRef.child(user.uid).setValue(user).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Обновить фото профиля
     */
    suspend fun updateProfilePhoto(uid: String, imageUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val filename = UUID.randomUUID().toString()
                val storageRef = storage.reference.child("profile_images/$uid/$filename")
                
                val uploadTask = storageRef.putFile(imageUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                
                // Обновляем ссылку на фото в базе данных
                usersRef.child(uid).child("photoUrl").setValue(downloadUrl).await()
                
                Result.success(downloadUrl)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Обновить пароль
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = auth.currentUser ?: return@withContext Result.failure(Exception("Пользователь не авторизован"))
                user.updatePassword(newPassword).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Обновить статистику пользователя
     */
    suspend fun updateUserStats(uid: String, quizzesTaken: Int, averageScore: Float, bestCategory: String?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userRef = usersRef.child(uid)
                userRef.child("quizzesTaken").setValue(quizzesTaken).await()
                userRef.child("averageScore").setValue(averageScore).await()
                userRef.child("bestCategory").setValue(bestCategory).await()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Инициализирует данные пользователя в Realtime Database, если они отсутствуют
     */
    suspend fun initUserInRealtimeDbIfNeeded(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser ?: return@withContext Result.failure(Exception("User not authenticated"))
                val uid = currentUser.uid
                val email = currentUser.email ?: ""
                val displayName = currentUser.displayName ?: "Пользователь"
                
                // Проверяем, существует ли запись в Realtime Database
                val dataSnapshot = usersRef.child(uid).get().await()
                
                if (!dataSnapshot.exists()) {
                    // Создаем базовую запись пользователя
                    val user = User(
                        uid = uid,
                        name = displayName,
                        email = email,
                        photoUrl = currentUser.photoUrl?.toString(),
                        quizzesTaken = 0,
                        averageScore = 0f,
                        bestCategory = null
                    )
                    
                    // Сохраняем в базу данных
                    usersRef.child(uid).setValue(user).await()
                    Log.d(TAG, "Created new user in Realtime Database: $uid")
                } else {
                    Log.d(TAG, "User already exists in Realtime Database: $uid")
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing user in Realtime Database", e)
                Result.failure(e)
            }
        }
    }
} 