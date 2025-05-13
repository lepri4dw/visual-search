package com.example.visualsearch.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.visualsearch.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * Менеджер аутентификации для работы с Firebase Auth
 */
class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    private val TAG = "AuthManager"

    init {
        // Инициализация текущего пользователя при создании экземпляра
        updateCurrentUser(auth.currentUser)
    }

    /**
     * Регистрация нового пользователя
     */
    fun register(email: String, password: String, displayName: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Успешная регистрация, обновляем имя пользователя
                    val firebaseUser = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()

                    firebaseUser?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                // Профиль обновлен успешно
                                updateCurrentUser(firebaseUser)
                                callback(true, null)
                            } else {
                                // Ошибка обновления профиля
                                callback(false, profileTask.exception?.message)
                            }
                        }
                } else {
                    // Ошибка регистрации
                    callback(false, task.exception?.message)
                }
            }
    }

    /**
     * Вход пользователя
     */
    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Успешный вход
                    updateCurrentUser(auth.currentUser)
                    callback(true, null)
                } else {
                    // Ошибка входа
                    callback(false, task.exception?.message)
                }
            }
    }

    /**
     * Выход пользователя из аккаунта
     */
    fun logout() {
        auth.signOut()
        updateCurrentUser(null)
    }

    /**
     * Обновление данных текущего пользователя
     */
    private fun updateCurrentUser(firebaseUser: FirebaseUser?) {
        _currentUser.value = firebaseUser?.let {
            User(
                uid = it.uid,
                email = it.email ?: "",
                displayName = it.displayName ?: "",
                photoUrl = it.photoUrl?.toString() ?: ""
            )
        }
    }

    /**
     * Получение ID текущего пользователя
     */
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    /**
     * Проверяет, авторизован ли пользователь
     */
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager().also { instance = it }
            }
        }
    }
}
