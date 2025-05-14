package com.example.visualsearch

import android.app.Application
import android.util.Log
import com.example.visualsearch.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Класс приложения для инициализации базы данных и других компонентов
 */
class VisualSearchApp : Application() {
    
    private val TAG = "VisualSearchApp"
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализируем базу данных и очищаем таблицы для предотвращения ошибок
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            // Очищаем базу данных при запуске
            CoroutineScope(Dispatchers.IO).launch {
                db.clearAllTables()
                Log.d(TAG, "База данных инициализирована и очищена")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при инициализации базы данных", e)
        }
    }
} 