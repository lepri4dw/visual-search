package com.example.visualsearch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.visualsearch.data.converter.DateConverter
import com.example.visualsearch.data.dao.ScanHistoryDao
import com.example.visualsearch.data.entity.ScanHistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * База данных приложения
 * ScanHistoryEntity остается в списке entities для обратной совместимости,
 * но реальные данные хранятся в Firebase Firestore
 */
@Database(entities = [ScanHistoryEntity::class], version = 5, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    // Возвращаем DAO для обратной совместимости
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "visual_search_database"
                )
                .fallbackToDestructiveMigration() // Добавляем для обработки изменения схемы
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Метод для очистки базы данных при необходимости
        fun clearDatabase() {
            INSTANCE?.let { db ->
                CoroutineScope(Dispatchers.IO).launch {
                    db.clearAllTables()
                }
            }
        }
    }
}
