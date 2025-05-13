package com.example.visualsearch.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.visualsearch.data.converter.DateConverter
import java.util.Date

@Entity(tableName = "scan_history")
@TypeConverters(DateConverter::class)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String, // ID пользователя из Firebase
    val query: String,
    val productType: String,
    val modelName: String,
    val brand: String,
    val color: String,
    val imagePath: String,  // Путь к изображению
    val scanDate: Date = Date() // Дата скана
)
