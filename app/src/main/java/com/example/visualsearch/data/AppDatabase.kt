package com.example.visualsearch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.visualsearch.data.dao.ScanHistoryDao
import com.example.visualsearch.data.entity.ScanHistoryEntity

@Database(entities = [ScanHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
