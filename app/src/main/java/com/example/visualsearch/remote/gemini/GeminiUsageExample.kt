package com.example.visualsearch.remote.gemini

import android.util.Log

/**
 * Класс с примерами использования GeminiApiClient
 * для получения поисковых запросов на основе данных Google Vision API
 */
class GeminiUsageExample(private val apiKey: String) {

    companion object {
        private const val TAG = "GeminiExample"
    }

    /**
     * Демонстрирует использование GeminiApiClient для смартфона Redmi
     */
    fun runSmartphoneExample() {
        // Создаем клиент Gemini API
        val geminiClient = GeminiApiClient(apiKey)

        // Данные от Google Vision API для смартфона Redmi
        val logoDescriptions = listOf(Pair("Redmi", 0.96503633f))
        val labels = listOf(
            Pair("Electronic device", 0.9716161f),
            Pair("Gadget", 0.8963614f),
            Pair("Technology", 0.88950837f),
            Pair("Silver", 0.7215447f),
            Pair("Communication Device", 0.58219695f),
            Pair("Electronics", 0.5203028f),
            Pair("Everyday carry", 0.51519173f)
        )
        val textDescriptions = listOf("Redmi", "Redmi")
        val dominantColors = listOf(
            Triple(48, 50, 43),
            Triple(195, 179, 156),
            Triple(87, 128, 140)
        )

        // Получаем поисковый запрос
        geminiClient.getSearchQuery(
            logoDescriptions,
            labels,
            textDescriptions,
            dominantColors,
            object : GeminiApiClient.GeminiApiListener {
                override fun onSuccess(searchQuery: SearchQuery) {
                    Log.d(TAG, "Смартфон - Поисковый запрос: ${searchQuery.query}")
                    Log.d(TAG, "Смартфон - Тип товара: ${searchQuery.productType}")
                    Log.d(TAG, "Смартфон - Модель: ${searchQuery.modelName}")
                    Log.d(TAG, "Смартфон - Бренд: ${searchQuery.brand}")

                    // Здесь код для отправки запроса в API Ozon и Wildberries
                    searchInMarketplaces(searchQuery)
                }

                override fun onError(e: Exception) {
                    Log.e(TAG, "Произошла ошибка: ${e.message}")
                }
            }
        )
    }

    /**
     * Демонстрирует использование GeminiApiClient для кубика Рубика
     */
    fun runRubiksCubeExample() {
        // Создаем клиент Gemini API
        val geminiClient = GeminiApiClient(apiKey)

        // Данные от Google Vision API для кубика Рубика
        val logoDescriptions = emptyList<Pair<String, Float>>()
        val labels = listOf(
            Pair("Puzzle", 0.8571942f),
            Pair("Rubik's Cube", 0.85232925f),
            Pair("Plastic", 0.6610691f),
            Pair("Gadget", 0.59006643f),
            Pair("Educational toy", 0.58328617f)
        )
        val textDescriptions = listOf("奇艺", "奇", "艺")
        val dominantColors = listOf(
            Triple(199, 192, 180),
            Triple(239, 100, 125),
            Triple(236, 59, 98)
        )

        // Получаем поисковый запрос
        geminiClient.getSearchQuery(
            logoDescriptions,
            labels,
            textDescriptions,
            dominantColors,
            object : GeminiApiClient.GeminiApiListener {
                override fun onSuccess(searchQuery: SearchQuery) {
                    Log.d(TAG, "Кубик Рубика - Поисковый запрос: ${searchQuery.query}")
                    Log.d(TAG, "Кубик Рубика - Тип товара: ${searchQuery.productType}")
                    Log.d(TAG, "Кубик Рубика - Модель: ${searchQuery.modelName}")
                    Log.d(TAG, "Кубик Рубика - Бренд: ${searchQuery.brand}")

                    // Здесь код для отправки запроса в API Ozon и Wildberries
                    searchInMarketplaces(searchQuery)
                }

                override fun onError(e: Exception) {
                    Log.e(TAG, "Произошла ошибка: ${e.message}")
                }
            }
        )
    }

    /**
     * Пример метода для поиска в маркетплейсах по полученному запросу
     */
    private fun searchInMarketplaces(searchQuery: SearchQuery) {
        // Здесь будет код для вызова API Ozon и Wildberries
        Log.d(TAG, "Выполняется поиск в маркетплейсах: ${searchQuery.query}")

        // Пример для Ozon API (заглушка)
        searchInOzon(searchQuery.query)

        // Пример для Wildberries API (заглушка)
        searchInWildberries(searchQuery.query)
    }

    private fun searchInOzon(query: String) {
        // Заглушка для поиска в Ozon
        Log.d(TAG, "Поиск в Ozon: $query")
    }

    private fun searchInWildberries(query: String) {
        // Заглушка для поиска в Wildberries
        Log.d(TAG, "Поиск в Wildberries: $query")
    }
}