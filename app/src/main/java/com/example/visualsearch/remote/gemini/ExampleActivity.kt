package com.example.visualsearch.remote.gemini

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.visualsearch.R

class ExampleActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ExampleActivity"
        private const val API_KEY = "AIzaSyCl8qzbbpDJ-ltNr_86SSFMTnbC6vhXvCk" // Замените на ваш ключ API
    }

    private lateinit var resultTextView: TextView
    private lateinit var gucciBagButton: Button
    private lateinit var rubiksCubeButton: Button
    private lateinit var customSearchButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        // Инициализация UI элементов
        resultTextView = findViewById(R.id.resultTextView)
        gucciBagButton = findViewById(R.id.gucciBagButton)
        rubiksCubeButton = findViewById(R.id.rubiksCubeButton)
        customSearchButton = findViewById(R.id.customSearchButton)

        // Обработчики нажатий
        gucciBagButton.setOnClickListener {
            resultTextView.text = "Выполняется запрос для сумки Gucci..."
            runGucciBagExample()
        }

        rubiksCubeButton.setOnClickListener {
            resultTextView.text = "Выполняется запрос для кубика Рубика..."
            runRubiksCubeExample()
        }

        customSearchButton.setOnClickListener {
            resultTextView.text = "Выполняется произвольный запрос с вашими данными..."
            runCustomExample()
        }
    }

    /**
     * Пример для сумки Gucci
     */
    private fun runGucciBagExample() {
        val geminiClient = GeminiApiClient(API_KEY)

        // Данные от Google Vision API для сумки Gucci
        val logoDescriptions = listOf(Pair("Gucci", 0.998f))
        val labels = listOf(
            Pair("Bag", 0.762f),
            Pair("Silver", 0.719f),
            Pair("Leather", 0.718f)
        )
        val textDescriptions = listOf("க P D G 3 3 3 G 333 888 333 333 3333 8 C CO 3333 8 8 8 333 8 8 GO 3 3333 33")
        val dominantColors = listOf(
            Triple(90, 85, 109),
            Triple(221, 224, 231),
            Triple(116, 112, 139)
        )

        // Получаем поисковый запрос
        geminiClient.getSearchQuery(
            logoDescriptions,
            labels,
            textDescriptions,
            dominantColors,
            object : GeminiApiClient.GeminiApiListener {
                override fun onSuccess(searchQuery: SearchQuery) {
                    val resultText = """
                        Результат для сумки Gucci:
                        
                        Поисковый запрос: ${searchQuery.query}
                        Тип товара: ${searchQuery.productType}
                        Модель: ${searchQuery.modelName}
                        Бренд: ${searchQuery.brand}
                        Цвет: ${searchQuery.color}
                    """.trimIndent()

                    runOnUiThread {
                        resultTextView.text = resultText
                    }

                    Log.d(TAG, resultText)
                }

                override fun onError(e: Exception) {
                    val errorText = "Произошла ошибка: ${e.message}"

                    runOnUiThread {
                        resultTextView.text = errorText
                    }

                    Log.e(TAG, errorText)
                }
            }
        )
    }

    /**
     * Пример для кубика Рубика
     */
    private fun runRubiksCubeExample() {
        val geminiClient = GeminiApiClient(API_KEY)

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
                    val resultText = """
                        Результат для кубика Рубика:
                        
                        Поисковый запрос: ${searchQuery.query}
                        Тип товара: ${searchQuery.productType}
                        Модель: ${searchQuery.modelName}
                        Бренд: ${searchQuery.brand}
                        Цвет: ${searchQuery.color}
                    """.trimIndent()

                    runOnUiThread {
                        resultTextView.text = resultText
                    }

                    Log.d(TAG, resultText)
                }

                override fun onError(e: Exception) {
                    val errorText = "Произошла ошибка: ${e.message}"

                    runOnUiThread {
                        resultTextView.text = errorText
                    }

                    Log.e(TAG, errorText)
                }
            }
        )
    }

    /**
     * Пример с пользовательскими данными - кроссовки Nike
     */
    private fun runCustomExample() {
        val geminiClient = GeminiApiClient(API_KEY)

        // Пример данных для Nike Air Max
        val logoDescriptions = listOf(Pair("Nike", 0.95f))
        val labels = listOf(
            Pair("Footwear", 0.92f),
            Pair("Shoe", 0.91f),
            Pair("Athletic shoe", 0.85f),
            Pair("Sneakers", 0.82f),
            Pair("Sports equipment", 0.75f)
        )
        val textDescriptions = listOf("Nike", "Air Max")
        val dominantColors = listOf(
            Triple(255, 255, 255),
            Triple(0, 0, 0),
            Triple(255, 0, 0)
        )

        // Получаем поисковый запрос
        geminiClient.getSearchQuery(
            logoDescriptions,
            labels,
            textDescriptions,
            dominantColors,
            object : GeminiApiClient.GeminiApiListener {
                override fun onSuccess(searchQuery: SearchQuery) {
                    val resultText = """
                        Результат для кроссовок Nike:
                        
                        Поисковый запрос: ${searchQuery.query}
                        Тип товара: ${searchQuery.productType}
                        Модель: ${searchQuery.modelName}
                        Бренд: ${searchQuery.brand}
                        Цвет: ${searchQuery.color}
                    """.trimIndent()

                    runOnUiThread {
                        resultTextView.text = resultText
                    }

                    Log.d(TAG, resultText)
                }

                override fun onError(e: Exception) {
                    val errorText = "Произошла ошибка: ${e.message}"

                    runOnUiThread {
                        resultTextView.text = errorText
                    }

                    Log.e(TAG, errorText)
                }
            }
        )
    }
}