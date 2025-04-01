package com.example.visualsearch.remote.gemini

import android.util.Log
import com.example.visualsearch.remote.gemini.GeminiRequest
import com.example.visualsearch.remote.gemini.GeminiResponse
import com.example.visualsearch.remote.gemini.GeminiService
import com.example.visualsearch.remote.gemini.SearchQuery
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class GeminiApiClient(private val apiKey: String) {
    private val service: GeminiService

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    }

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        service = retrofit.create(GeminiService::class.java)
    }

    interface GeminiApiListener {
        fun onSuccess(searchQuery: SearchQuery)
        fun onError(e: Exception)
    }

    // Метод для получения поискового запроса на основе тегов
    fun getSearchQuery(
        logoDescriptions: List<Pair<String, Float>> = emptyList(),
        labels: List<Pair<String, Float>> = emptyList(),
        textDescriptions: List<String> = emptyList(),
        dominantColors: List<Triple<Int, Int, Int>> = emptyList(),
        listener: GeminiApiListener
    ) {
        val prompt = buildSearchPrompt(
            logoDescriptions,
            labels,
            textDescriptions,
            dominantColors
        )

        val request = GeminiRequest(prompt)

        service.generateContent(apiKey, request).enqueue(object : Callback<GeminiResponse> {
            override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val text = response.body()?.candidates?.get(0)?.content?.parts?.get(0)?.text ?: ""
                        val searchQuery = parseGeminiResponse(text)
                        listener.onSuccess(searchQuery)
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при обработке ответа Gemini: ${e.message}")
                        // В случае ошибки создаем базовый объект поиска на основе имеющихся тегов
                        val fallbackQuery = createFallbackSearchQuery(logoDescriptions, labels, textDescriptions, dominantColors)
                        listener.onSuccess(fallbackQuery)
                    }
                } else {
                    try {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e(TAG, "Ошибка API: $errorBody")
                        val fallbackQuery = createFallbackSearchQuery(logoDescriptions, labels, textDescriptions, dominantColors)
                        listener.onSuccess(fallbackQuery)
                    } catch (e: IOException) {
                        val fallbackQuery = createFallbackSearchQuery(logoDescriptions, labels, textDescriptions, dominantColors)
                        listener.onSuccess(fallbackQuery)
                    }
                }
            }

            override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                Log.e(TAG, "Ошибка соединения: ${t.message}")
                val fallbackQuery = createFallbackSearchQuery(logoDescriptions, labels, textDescriptions, dominantColors)
                listener.onSuccess(fallbackQuery)
            }
        })
    }

    // Создание промпта для Gemini
    private fun buildSearchPrompt(
        logoDescriptions: List<Pair<String, Float>>,
        labels: List<Pair<String, Float>>,
        textDescriptions: List<String>,
        dominantColors: List<Triple<Int, Int, Int>>
    ): String {
        val sb = StringBuilder()

        sb.append("На основе следующих тегов и описаний, определи, какой именно товар на фотографии, ")
        sb.append("и сформируй оптимальный поисковый запрос для его поиска на маркетплейсах Ozon и Wildberries.\n\n")

        // Добавляем информацию о логотипах
        if (logoDescriptions.isNotEmpty()) {
            sb.append("Логотипы (с оценкой достоверности):\n")
            logoDescriptions.forEach { (desc, score) ->
                sb.append("- $desc (${String.format("%.2f", score)})\n")
            }
            sb.append("\n")
        }

        // Добавляем информацию о метках/лейблах
        if (labels.isNotEmpty()) {
            sb.append("Метки (с оценкой достоверности):\n")
            labels.forEach { (desc, score) ->
                sb.append("- $desc (${String.format("%.2f", score)})\n")
            }
            sb.append("\n")
        }

        // Добавляем информацию о тексте на изображении
        if (textDescriptions.isNotEmpty()) {
            sb.append("Текст на изображении:\n")
            textDescriptions.forEach { text ->
                sb.append("- $text\n")
            }
            sb.append("\n")
        }

        // Добавляем информацию о доминирующих цветах
        if (dominantColors.isNotEmpty()) {
            sb.append("Доминирующие цвета (RGB):\n")
            dominantColors.forEachIndexed { index, (r, g, b) ->
                val colorName = rgbToColorName(r, g, b)
                sb.append("- Цвет ${index + 1}: R:$r, G:$g, B:$b ($colorName)\n")
            }
            sb.append("\n")
        }

        sb.append("Важно: Внимательно анализируй все метки. Если есть метки, указывающие на конкретный тип товара ")
        sb.append(", они имеют приоритет перед общими категориями ")
        sb.append("типа 'Gadget' или 'Electronics'. Если метки указывают на обувь, одежду, аксессуары или игрушки, ")
        sb.append("то это не электроника.\n\n")

        sb.append("Обрати особое внимание на текст изображения - он может содержать название модели или бренда. ")
        sb.append("Игнорируй бессмысленные последовательности букв и цифр, но используй осмысленные слова.\n\n")

        sb.append("Сформируй точный и лаконичный поисковый запрос, который наилучшим образом описывает товар ")
        sb.append("и поможет найти его на маркетплейсах. Включи название бренда (если определено), ")
        sb.append("тип продукта, ключевые характеристики и основной цвет товара. ")
        sb.append("Не добавляй в запрос слова \"купить\", \"заказать\" и подобные. ")
        sb.append("Если это возможно, определи конкретную модель устройства. ")
        sb.append("Ответь в следующем формате без дополнительных комментариев:\n\n")

        sb.append("Поисковый запрос: [короткий и точный запрос с указанием цвета, если он значим]\n")
        sb.append("Тип товара: [общая категория товара]\n")
        sb.append("Название/Модель: [если определяется конкретная модель или название]\n")
        sb.append("Бренд: [если определяется бренд]\n")
        sb.append("Цвет: [основной цвет товара]\n")

        return sb.toString()
    }

    // Парсинг ответа от Gemini
    private fun parseGeminiResponse(text: String): SearchQuery {
        var searchQuery = ""
        var productType = ""
        var modelName = ""
        var brand = ""
        var color = ""

        val lines = text.split("\n")
        for (line in lines) {
            when {
                line.startsWith("Поисковый запрос:") ->
                    searchQuery = line.substringAfter("Поисковый запрос:").trim()
                line.startsWith("Тип товара:") ->
                    productType = line.substringAfter("Тип товара:").trim()
                line.startsWith("Название/Модель:") ->
                    modelName = line.substringAfter("Название/Модель:").trim()
                line.startsWith("Бренд:") ->
                    brand = line.substringAfter("Бренд:").trim()
                line.startsWith("Цвет:") ->
                    color = line.substringAfter("Цвет:").trim()
            }
        }

        return SearchQuery(searchQuery, productType, modelName, brand, color)
    }

    // Создание резервного поискового запроса в случае ошибки
    private fun createFallbackSearchQuery(
        logoDescriptions: List<Pair<String, Float>>,
        labels: List<Pair<String, Float>>,
        textDescriptions: List<String>,
        dominantColors: List<Triple<Int, Int, Int>>
    ): SearchQuery {
        // Получаем бренд из логотипов с высоким score или из текста
        val brand = logoDescriptions.firstOrNull { it.second > 0.8 }?.first ?:
        textDescriptions.firstOrNull { it.length > 2 } ?: ""

        // Определяем категорию товара на основе меток
        val isFootwearOrClothing = labels.any {
            it.first.lowercase().matches(Regex(".*(shoe|footwear|sneaker|clothing|apparel|bag|handbag|fashion).*"))
        }

        val isToy = labels.any {
            it.first.lowercase().matches(Regex(".*(toy|puzzle|game|rubik).*"))
        }

        val productType = when {
            isFootwearOrClothing -> "Обувь и одежда"
            isToy -> "Игрушки и игры"
            else -> labels.firstOrNull {
                it.first.matches(Regex(".*", RegexOption.IGNORE_CASE))
            }?.first ?: "Товар"
        }

        // Определяем основной цвет
        val dominantColor = if (dominantColors.isNotEmpty()) {
            rgbToColorName(
                dominantColors[0].first,
                dominantColors[0].second,
                dominantColors[0].third
            )
        } else {
            ""
        }

        // Создаем базовый поисковый запрос
        val query = buildString {
            if (brand.isNotEmpty()) {
                append("$brand ")
            }

            // Берем первые 1-2 метки с высоким score, которые не являются брендом
            val topLabels = labels
                .filter { it.second > 0.7 && !it.first.equals(brand, ignoreCase = true) }
                .sortedByDescending { it.second }
                .take(2)
                .map { it.first }

            append(topLabels.joinToString(" "))

            if (dominantColor.isNotEmpty()) {
                append(" $dominantColor")
            }
        }.trim()

        return SearchQuery(query, productType, "", brand, dominantColor)
    }

    // Преобразование RGB в название цвета
    private fun rgbToColorName(r: Int, g: Int, b: Int): String {
        // Простая версия определения цвета по RGB
        return when {
            r > 200 && g > 200 && b > 200 -> "белый"
            r < 60 && g < 60 && b < 60 -> "черный"
            r > 200 && g < 100 && b < 100 -> "красный"
            r < 100 && g > 150 && b < 100 -> "зеленый"
            r < 100 && g < 100 && b > 150 -> "синий"
            r > 200 && g > 150 && b < 100 -> "желтый"
            r > 200 && g > 100 && b < 100 -> "оранжевый"
            r > 150 && g < 100 && b > 150 -> "фиолетовый"
            r < 150 && g > 150 && b > 150 -> "голубой"
            r > 150 && g > 100 && b > 150 -> "розовый"
            r > 150 && g > 100 && b < 100 -> "коричневый"
            r < 200 && r > 100 && g < 200 && g > 100 && b < 200 && b > 100 -> "серый"
            else -> "многоцветный"
        }
    }
}