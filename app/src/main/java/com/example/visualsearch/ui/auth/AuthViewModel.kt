package com.example.visualsearch.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.visualsearch.auth.AuthManager
import com.example.visualsearch.model.User

class AuthViewModel : ViewModel() {
    private val authManager = AuthManager.getInstance()

    // LiveData для отслеживания состояния аутентификации
    val currentUser: LiveData<User?> = authManager.currentUser

    // LiveData для состояния авторизации
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    /**
     * Состояния авторизации
     */
    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        object Success : AuthState()
        data class Error(val message: String) : AuthState()
    }

    init {
        _authState.value = AuthState.Idle
    }

    /**
     * Регистрация нового пользователя
     */
    fun register(email: String, password: String, displayName: String) {
        _authState.value = AuthState.Loading

        authManager.register(email, password, displayName) { success, error ->
            if (success) {
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(error ?: "Неизвестная ошибка при регистрации")
            }
        }
    }

    /**
     * Вход пользователя
     */
    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading

        authManager.login(email, password) { success, error ->
            if (success) {
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(error ?: "Неизвестная ошибка при входе")
            }
        }
    }

    /**
     * Выход пользователя
     */
    fun logout() {
        authManager.logout()
    }

    /**
     * Сброс состояния авторизации
     */
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    /**
     * Проверка, авторизован ли пользователь
     */
    fun isLoggedIn(): Boolean {
        return authManager.isLoggedIn()
    }
}