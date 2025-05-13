package com.example.visualsearch.ui.history

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.visualsearch.data.entity.ScanHistoryEntity
import com.example.visualsearch.data.repository.ScanHistoryRepository
import com.example.visualsearch.remote.gemini.SearchQuery
import com.example.visualsearch.util.ImageSaver
import kotlinx.coroutines.launch

class ScanHistoryViewModel(
    application: Application,
    private val scanHistoryRepository: ScanHistoryRepository
) : AndroidViewModel(application) {

    // Получение списка сканирований текущего пользователя
    val userScans: LiveData<List<ScanHistoryEntity>> = scanHistoryRepository.getUserScans()

    /**
     * Добавление сканирования в историю
     */
    fun addScan(
        query: String,
        productType: String,
        modelName: String,
        brand: String,
        color: String,
        imagePath: String
    ) {
        viewModelScope.launch {
            scanHistoryRepository.insertScan(
                query,
                productType,
                modelName,
                brand,
                color,
                imagePath
            )
        }
    }

    /**
     * Удаление сканирования из истории
     */
    fun deleteScan(scan: ScanHistoryEntity) {
        viewModelScope.launch {
            scanHistoryRepository.deleteScan(scan)
        }
    }

    /**
     * Удаление всех сканирований пользователя
     */
    fun deleteAllUserScans() {
        viewModelScope.launch {
            scanHistoryRepository.deleteUserScans()
        }
    }

    /**
     * Получение информации о сканировании по ID
     */
    suspend fun getScanById(scanId: Long): ScanHistoryEntity? {
        return scanHistoryRepository.getScanById(scanId)
    }

    /**
     * Сохранение сканирования с изображением
     */
    fun saveScanWithBitmap(searchQuery: SearchQuery, bitmap: Bitmap) {
        val imagePath = ImageSaver.saveImageToInternalStorage(getApplication(), bitmap)
        if (imagePath != null) {
            viewModelScope.launch {
                scanHistoryRepository.insertScan(
                    searchQuery.query,
                    searchQuery.productType,
                    searchQuery.modelName,
                    searchQuery.brand,
                    searchQuery.color,
                    imagePath
                )
            }
        }
    }
}