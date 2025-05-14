package com.example.visualsearch.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import androidx.room.TypeConverters
import com.example.visualsearch.data.converter.DateConverter
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Сущность для хранения истории сканирований, используемая для Firebase Firestore
 * Аннотации Room добавлены для совместимости с процессором аннотаций
 */
@Entity(tableName = "scan_history")
@TypeConverters(DateConverter::class)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    var roomId: Long = 0, // Для совместимости с Room
    
    // Основные поля
    @Ignore
    var firestoreId: String = "", // ID для Firestore 
    
    @ColumnInfo(name = "user_id")
    var userId: String = "", // ID пользователя из Firebase
    
    @ColumnInfo(name = "query")
    var query: String = "", // Поисковый запрос
    
    @ColumnInfo(name = "product_type")
    var productType: String = "", // Тип продукта
    
    @ColumnInfo(name = "model_name")
    var modelName: String = "", // Название модели
    
    @ColumnInfo(name = "brand")
    var brand: String = "", // Бренд
    
    @ColumnInfo(name = "color")
    var color: String = "", // Цвет
    
    @ColumnInfo(name = "image_path")
    var imagePath: String = "", // Путь к изображению
    
    @ColumnInfo(name = "scan_date")
    var scanDate: Date = Date() // Дата сканирования
) {
    // Конструктор без параметров необходим для Firestore
    constructor() : this(
        roomId = 0,
        firestoreId = "",
        userId = "",
        query = "",
        productType = "",
        modelName = "",
        brand = "",
        color = "",
        imagePath = "",
        scanDate = Date()
    )
    
    // Для совместимости с существующим кодом
    val id: String
        get() = firestoreId.takeIf { it.isNotEmpty() } ?: roomId.toString()
    
    // Вспомогательные методы для конвертации
    fun getDateAsJavaDate(): Date {
        return scanDate
    }
    
    // Вспомогательные методы для Firestore
    fun getFirebaseTimestamp(): Timestamp {
        return Timestamp(scanDate)
    }
    
    // Создаем Map для Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "user_id" to userId,
            "query" to query,
            "product_type" to productType,
            "model_name" to modelName,
            "brand" to brand,
            "color" to color,
            "image_path" to imagePath,
            "scan_date" to Timestamp(scanDate)
        )
    }
    
    // Для удобства отладки
    override fun toString(): String {
        return "ScanHistoryEntity(id=$id, firestoreId=$firestoreId, query=$query, productType=$productType, imagePath=$imagePath)"
    }
}
