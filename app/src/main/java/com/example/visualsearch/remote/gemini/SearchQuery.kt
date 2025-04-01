package com.example.visualsearch.remote.gemini

data class SearchQuery(
    val query: String,
    val productType: String,
    val modelName: String,
    val brand: String,
    val color: String = "" // Добавлено новое поле для цвета
)