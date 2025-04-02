package com.example.visualsearch.remote.gemini

import android.util.Log
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

    private fun buildSearchPrompt(
        logoDescriptions: List<Pair<String, Float>>,
        labels: List<Pair<String, Float>>,
        textDescriptions: List<String>,
        dominantColors: List<Triple<Int, Int, Int>>
    ): String {
        val sb = StringBuilder()

        sb.append("На основе следующих тегов и описаний, определи, какой именно товар на фотографии, ")
        sb.append("и сформируй оптимальный поисковый запрос для его поиска на маркетплейсах.\n\n")

        if (logoDescriptions.isNotEmpty()) {
            sb.append("Логотипы (с оценкой достоверности):\n")
            logoDescriptions.forEach { (desc, score) ->
                sb.append("- $desc (${String.format("%.2f", score)})\n")
            }
            sb.append("\n")
        }

        if (labels.isNotEmpty()) {
            sb.append("Метки (с оценкой достоверности):\n")
            labels.forEach { (desc, score) ->
                sb.append("- $desc (${String.format("%.2f", score)})\n")
            }
            sb.append("\n")
        }

        if (textDescriptions.isNotEmpty()) {
            sb.append("Текст на изображении:\n")
            textDescriptions.forEach { text ->
                sb.append("- $text\n")
            }
            sb.append("\n")
        }

        if (dominantColors.isNotEmpty()) {
            sb.append("Доминирующие цвета (RGB):\n")
            dominantColors.forEachIndexed { index, (r, g, b) ->
                val colorName = rgbToColorName(r, g, b)
                sb.append("- Цвет ${index + 1}: R:$r, G:$g, B:$b ($colorName)\n")
            }
            sb.append("\n")
        }

        sb.append("Важно: Определи тип товара максимально точно. Когда определяешь тип товара:\n")
        sb.append("1. Не объединяй товары в общие категории, указывай конкретный тип (например, 'Очки', 'Сумка', 'Кубик Рубика')\n")
        sb.append("2. Если тип товара не очевиден из меток, логически определи его на основе бренда или других признаков\n")
        sb.append("3. Не используй материалы, цвета или общие категории (например, 'Silver', 'Plastic', 'Carbon fibers', 'Electronics', 'Gadget') как тип товара\n\n")

        sb.append("Обрати особое внимание на текст изображения - он часто содержит название модели или бренда. ")
        sb.append("Используй только релевантную информацию для поиска - избегай включения случайных слов, материалов, ")
        sb.append("и описательных терминов, которые не помогают при поиске конкретного товара.\n\n")

        sb.append("Сформируй четкий и лаконичный поисковый запрос, который наилучшим образом описывает товар. ")
        sb.append("Включи название бренда (если определено), тип продукта и модель. ")
        sb.append("Цвет указывай только если он важен для идентификации товара. ")
        sb.append("Убери из запроса лишние слова и технические термины, не относящиеся к поиску.\n\n")

        sb.append("Ответь в следующем формате без дополнительных комментариев:\n\n")

        sb.append("Поисковый запрос: [короткий и точный запрос]\n")
        sb.append("Тип товара: [конкретный тип товара, а не категория или материал]\n")
        sb.append("Название/Модель: [если определяется конкретная модель или название]\n")
        sb.append("Бренд: [если определяется бренд]\n")
        sb.append("Цвет: [основной цвет товара]\n")

        return sb.toString()
    }

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

    private fun createFallbackSearchQuery(
        logoDescriptions: List<Pair<String, Float>>,
        labels: List<Pair<String, Float>>,
        textDescriptions: List<String>,
        dominantColors: List<Triple<Int, Int, Int>>
    ): SearchQuery {
        val brand = logoDescriptions.firstOrNull { it.second > 0.8 }?.first ?:
        extractBrandFromText(textDescriptions)

        val specificProductLabels = labels.filter {
            !isGenericLabel(it.first) && it.second > 0.7
        }

        val productType = when {
            specificProductLabels.isNotEmpty() -> specificProductLabels.first().first
            labels.isNotEmpty() -> labels.first().first
            else -> "Товар"
        }

        val dominantColor = if (dominantColors.isNotEmpty()) {
            rgbToColorName(
                dominantColors[0].first,
                dominantColors[0].second,
                dominantColors[0].third
            )
        } else {
            ""
        }

        val query = buildString {
            if (brand.isNotEmpty()) {
                append("$brand ")
            }

            val modelNumber = extractModelFromText(textDescriptions, brand)
            if (modelNumber.isNotEmpty()) {
                append("$modelNumber ")
            }

            if (productType.isNotEmpty() && !brand.contains(productType, ignoreCase = true)) {
                append("$productType ")
            }

            if (dominantColor.isNotEmpty() && !isColorRedundant(this.toString(), dominantColor)) {
                append(dominantColor)
            }
        }.trim()

        return SearchQuery(query, productType, extractModelFromText(textDescriptions, brand), brand, dominantColor)
    }

    private fun extractBrandFromText(textDescriptions: List<String>): String {
        for (text in textDescriptions) {
            val words = text.split(" ", "-", "_")
            for (word in words) {
                val cleanWord = word.trim().replace(Regex("[^A-Za-z0-9]"), "")
                if (cleanWord.length > 2 && !cleanWord.matches(Regex("^[0-9]+$"))) {
                    return cleanWord
                }
            }
        }
        return ""
    }

    private fun extractModelFromText(textDescriptions: List<String>, brand: String): String {
        for (text in textDescriptions) {
            if (text.contains(Regex("[A-Z0-9]{5,}"))) {
                return text.replace(brand, "", ignoreCase = true).trim()
            }
        }
        return ""
    }

    private fun isGenericLabel(label: String): Boolean {
        val genericTerms = listOf(
            "gadget", "electronics", "device", "product", "item",
            "plastic", "metal", "silver", "gold", "carbon", "fibers",
            "packaging", "box", "container", "material"
        )

        return genericTerms.any {
            label.lowercase().contains(it)
        }
    }

    private fun isColorRedundant(query: String, color: String): Boolean {
        return query.lowercase().contains(color.lowercase())
    }

    private fun rgbToColorName(r: Int, g: Int, b: Int): String {
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