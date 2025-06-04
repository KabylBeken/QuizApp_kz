package com.pratyakshkhurana.quizapp.auth

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Репозиторий для работы с Firestore
 */
class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = "users"
    
    companion object {
        private const val TAG = "FirestoreRepository"
    }

    /**
     * Проверяет существование профиля текущего пользователя
     */
    suspend fun checkUserProfileExists(): Boolean {
        return try {
            val currentUid = auth.currentUser?.uid ?: return false
            val documentSnapshot = db.collection(usersCollection)
                .document(currentUid)
                .get()
                .await()
            
            documentSnapshot.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user profile exists", e)
            false
        }
    }

    /**
     * Создает профиль пользователя
     */
    suspend fun createUserProfile(profile: UserProfile): Boolean {
        return try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                Log.e(TAG, "Cannot create profile, user not authenticated")
                return false
            }
            
            // Добавляем профиль пользователя
            db.collection(usersCollection)
                .document(currentUser.uid)
                .set(profile)
                .await()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating user profile", e)
            false
        }
    }

    /**
     * Получает профиль текущего пользователя
     */
    suspend fun getUserProfile(): UserProfile? {
        return try {
            val currentUid = auth.currentUser?.uid ?: return null
            
            val documentSnapshot = db.collection(usersCollection)
                .document(currentUid)
                .get()
                .await()
            
            if (documentSnapshot.exists()) {
                documentSnapshot.toObject(UserProfile::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user profile", e)
            null
        }
    }
} 