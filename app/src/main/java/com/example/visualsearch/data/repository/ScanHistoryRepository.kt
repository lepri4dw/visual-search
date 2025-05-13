package com.example.visualsearch.data.repository

import androidx.lifecycle.LiveData
import com.example.visualsearch.auth.AuthManager
import com.example.visualsearch.data.dao.ScanHistoryDao
import com.example.visualsearch.data.entity.ScanHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Репозиторий для работы с историей сканирований
 */
class ScanHistoryRepository(private val scanHistoryDao: ScanHistoryDao) {

    private val authManager = AuthManager.getInstance()

    /**
     * Добавление нового сканирования в историю
     */
    suspend fun insertScan(
        query: String,
        productType: String,
        modelName: String,
        brand: String,
        color: String,
        imagePath: String
    ): Long {
        return withContext(Dispatchers.IO) {
            val userId = authManager.getCurrentUserId()
            val scanHistory = ScanHistoryEntity(
                userId = userId,
                query = query,
                productType = productType,
                modelName = modelName,
                brand = brand,
                color = color,
                imagePath = imagePath
            )
            scanHistoryDao.insertScan(scanHistory)
        }
    }

    /**
     * Получение всех сканирований текущего пользователя
     */
    fun getUserScans(): LiveData<List<ScanHistoryEntity>> {
        val userId = authManager.getCurrentUserId()
        // Если пользователь не авторизован, возвращаем пустой список
        return if (userId.isNotEmpty()) {
            scanHistoryDao.getScansByUserId(userId)
        } else {
            scanHistoryDao.getAllScans()
        }
    }

    /**
     * Получение информации о сканировании по ID
     */
    suspend fun getScanById(scanId: Long): ScanHistoryEntity? {
        return withContext(Dispatchers.IO) {
            scanHistoryDao.getScanById(scanId)
        }
    }

    /**
     * Удаление сканирования из истории
     */
    suspend fun deleteScan(scanHistory: ScanHistoryEntity) {
        withContext(Dispatchers.IO) {
            scanHistoryDao.deleteScan(scanHistory)
        }
    }

    /**
     * Удаление всех сканирований текущего пользователя
     */
    suspend fun deleteUserScans() {
        withContext(Dispatchers.IO) {
            val userId = authManager.getCurrentUserId()
            if (userId.isNotEmpty()) {
                scanHistoryDao.deleteUserScans(userId)
            }
        }
    }
}