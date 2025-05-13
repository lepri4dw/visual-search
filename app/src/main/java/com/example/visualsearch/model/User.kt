package com.example.visualsearch.model

/**
 * Модель данных пользователя
 */
data class User(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String = ""
)