package com.example.visualsearch.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.visualsearch.auth.AuthManager
import com.example.visualsearch.data.dao.ScanHistoryDao
import com.example.visualsearch.data.entity.ScanHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Репозиторий для работы с историей сканирований - заглушка для обратной совместимости
 * Реальные данные хранятся в Firebase Firestore через FirestoreScanRepository
 */
class ScanHistoryRepository(private val scanHistoryDao: ScanHistoryDao) {

    private val authManager = AuthManager.getInstance()
    private val _emptyList = MutableLiveData<List<ScanHistoryEntity>>(emptyList())

    // Возвращаем пустой список для всех методов, так как используем FirestoreScanRepository
    val allScans: LiveData<List<ScanHistoryEntity>> = _emptyList
    val userScansList: LiveData<List<ScanHistoryEntity>> = _emptyList

    /**
     * Добавление нового сканирования в историю
     * Операция перенаправляется в FirestoreScanRepository
     */
    suspend fun insertScan(
        query: String,
        productType: String,
        modelName: String,
        brand: String,
        color: String,
        imagePath: String
    ): Long = withContext(Dispatchers.IO) {
        // Теперь в Room больше ничего не сохраняем
        return@withContext 0L
    }

    /**
     * Получение всех сканирований текущего пользователя
     * Операция перенаправляется в FirestoreScanRepository
     */
    fun getUserScans(): LiveData<List<ScanHistoryEntity>> {
        // Возвращаем пустой список, так как используем FirestoreScanRepository
        return _emptyList
    }

    /**
     * Получение информации о сканировании по ID
     * Операция перенаправляется в FirestoreScanRepository
     */
    suspend fun getScanById(scanId: Long): ScanHistoryEntity? = withContext(Dispatchers.IO) {
        // Возвращаем null, так как используем FirestoreScanRepository
        return@withContext null
    }

    /**
     * Удаление сканирования из истории
     * Операция перенаправляется в FirestoreScanRepository
     */
    suspend fun deleteScan(scanHistory: ScanHistoryEntity) {
        // Ничего не делаем, так как используем FirestoreScanRepository
    }

    /**
     * Удаление всех сканирований текущего пользователя
     * Операция перенаправляется в FirestoreScanRepository
     */
    suspend fun deleteUserScans() {
        // Ничего не делаем, так как используем FirestoreScanRepository
    }
} 