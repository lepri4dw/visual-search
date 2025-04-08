package com.example.visualsearch.remote.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiApiClient(private val apiKey: String) {
    private val service: GeminiService

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    }

    init {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(GeminiService::class.java)
    }

    interface GeminiApiListener {
        fun onSuccess(searchQuery: SearchQueryResult)
        fun onError(e: Exception)
    }
    
    fun analyzeImage(bitmap: Bitmap, listener: GeminiApiListener) {
        try {
            // Convert bitmap to base64
            val base64Image = bitmapToBase64(bitmap)

            // Create the request body as JSON
            val requestJson = buildImageAnalysisRequest(base64Image)
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            // Make API call
            service.generateContent(apiKey, requestBody).enqueue(object : Callback<GeminiResponse> {
                override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                            val searchQuery = parseGeminiResponse(text)
                            listener.onSuccess(searchQuery)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing Gemini response: ${e.message}")
                            listener.onError(e)
                        }
                    } else {
                        try {
                            val errorBody = response.errorBody()?.string() ?: "Unknown error"
                            Log.e(TAG, "API Error: $errorBody")
                            listener.onError(IOException("API Error: $errorBody"))
                        } catch (e: IOException) {
                            listener.onError(e)
                        }
                    }
                }

                override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                    Log.e(TAG, "Connection error: ${t.message}")
                    listener.onError(Exception(t))
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing request: ${e.message}")
            listener.onError(e)
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val resizedBitmap = resizeBitmapIfNeeded(bitmap)
        val byteArrayOutputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxSize: Int = 1024): Bitmap {
        // Resize bitmap if it's too large to avoid API limits
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun buildImageAnalysisRequest(base64Image: String): JsonObject {
        val gson = Gson()

        // Create the text instruction part
        val textPart = JsonObject().apply {
            addProperty("text", buildAnalysisPrompt())
        }

        // Create the image part with inline_data
        val imagePart = JsonObject().apply {
            val inlineData = JsonObject().apply {
                addProperty("mime_type", "image/jpeg")
                addProperty("data", base64Image)
            }
            add("inline_data", inlineData)
        }

        // Create contents array with both parts
        val contentsParts = JsonObject()
        contentsParts.add("parts", gson.toJsonTree(listOf(textPart, imagePart)))

        // Build the complete request
        val request = JsonObject()
        request.add("contents", gson.toJsonTree(listOf(contentsParts)))

        return request
    }

    private fun buildAnalysisPrompt(): String {
        return """
        Проанализируй это изображение товара и определи следующие детали:
        1. Тип товара (конкретный тип, например: "Смартфон", "Наушники", "Кроссовки")
        2. Бренд (если виден)
        3. Модель (если определяется)
        4. Основной цвет товара
        
        На основе этих данных составь два поисковых запроса:
        1. Основной запрос - максимально точный для поиска именно этого товара
        2. Альтернативный запрос - более общий для поиска похожих товаров (категория или тип товара)
        
        Важно: Определи тип товара максимально точно. Когда определяешь тип товара:
        1. Укажи конкретный тип продукта, а не общую категорию
        2. Если на изображении есть текст - он может содержать важную информацию о бренде и модели
        3. Обрати внимание на логотипы - они часто указывают на бренд
        4. Используй только релевантную информацию для поиска
        
        Ответь в следующем формате без дополнительных комментариев:
        
        Основной запрос: [короткий и точный запрос для конкретного товара]
        Альтернативный запрос: [более общий запрос для похожих товаров]
        Тип товара: [конкретный тип товара]
        Название/Модель: [если определяется конкретная модель или название]
        Бренд: [если определяется бренд]
        Цвет: [основной цвет товара]
    """.trimIndent()
    }
    data class SearchQueryResult(
        val mainQuery: SearchQuery,
        val alternativeQuery: SearchQuery
    )

    private fun parseGeminiResponse(text: String): SearchQueryResult {
        var mainSearchQuery = ""
        var alternativeSearchQuery = ""
        var productType = ""
        var modelName = ""
        var brand = ""
        var color = ""

        val lines = text.split("\n")
        for (line in lines) {
            when {
                line.startsWith("Основной запрос:") ->
                    mainSearchQuery = line.substringAfter("Основной запрос:").trim()
                line.startsWith("Альтернативный запрос:") ->
                    alternativeSearchQuery = line.substringAfter("Альтернативный запрос:").trim()
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

        // Если основной запрос пустой, создаем его из имеющихся данных
        if (mainSearchQuery.isEmpty() && (productType.isNotEmpty() || brand.isNotEmpty())) {
            val queryParts = mutableListOf<String>()
            if (brand.isNotEmpty()) queryParts.add(brand)
            if (modelName.isNotEmpty()) queryParts.add(modelName)
            if (productType.isNotEmpty()) queryParts.add(productType)
            if (color.isNotEmpty() && color != "многоцветный") queryParts.add(color)

            mainSearchQuery = queryParts.joinToString(" ")
        }

        // Если альтернативный запрос пустой, используем тип товара или часть основного запроса
        if (alternativeSearchQuery.isEmpty()) {
            alternativeSearchQuery = if (productType.isNotEmpty()) {
                // Используем только тип товара как альтернативный запрос
                productType
            } else {
                // Если нет типа товара, используем первое слово из основного запроса
                mainSearchQuery.split(" ").firstOrNull() ?: mainSearchQuery
            }
        }

        val main = SearchQuery(mainSearchQuery, productType, modelName, brand, color)
        val alternative = SearchQuery(alternativeSearchQuery, productType, "", "", "")

        return SearchQueryResult(main, alternative)
    }
}