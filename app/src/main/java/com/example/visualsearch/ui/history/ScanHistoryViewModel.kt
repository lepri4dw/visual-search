package com.example.visualsearch.ui.history

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.example.visualsearch.auth.AuthManager
import com.example.visualsearch.data.entity.ScanHistoryEntity
import com.example.visualsearch.data.repository.FirestoreScanRepository
import com.example.visualsearch.remote.gemini.SearchQuery
import kotlinx.coroutines.launch

class ScanHistoryViewModel(
    application: Application,
    private val scanRepository: FirestoreScanRepository
) : AndroidViewModel(application) {

    private val TAG = "ScanHistoryViewModel"
    private val authManager = AuthManager.getInstance()
    
    // Оборачиваем userScans из репозитория в MediatorLiveData для мониторинга изменений
    private val _mediatorUserScans = MediatorLiveData<List<ScanHistoryEntity>>()
    
    // Получение списка сканирований текущего пользователя
    val userScans: LiveData<List<ScanHistoryEntity>> = _mediatorUserScans
    
    init {
        // Проверяем статус авторизации
        val isLoggedIn = isUserLoggedIn()
        Log.d(TAG, "ViewModel создан, пользователь авторизован: $isLoggedIn, userId: ${authManager.getCurrentUserId()}")
        
        // Добавляем источник и обработчик
        _mediatorUserScans.addSource(scanRepository.userScans) { scans ->
            Log.d(TAG, "Получены данные из репозитория: ${scans.size} элементов")
            _mediatorUserScans.value = scans
        }
    }

    /**
     * Проверяет, авторизован ли пользователь
     */
    fun isUserLoggedIn(): Boolean {
        return authManager.isLoggedIn()
    }

    /**
     * Получает ID текущего пользователя
     */
    fun getCurrentUserId(): String {
        return authManager.getCurrentUserId()
    }

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
        // Добавляем сканирование только если пользователь авторизован
        if (isUserLoggedIn()) {
            viewModelScope.launch {
                scanRepository.insertScan(
                    query,
                    productType,
                    modelName,
                    brand,
                    color,
                    imagePath
                )
            }
        }
    }

    /**
     * Удаление сканирования из истории
     */
    fun deleteScan(scan: ScanHistoryEntity) {
        if (scan.id.isNotEmpty()) {
            viewModelScope.launch {
                scanRepository.deleteScan(scan.id)
            }
        }
    }

    /**
     * Удаление всех сканирований пользователя
     */
    fun deleteAllUserScans() {
        viewModelScope.launch {
            scanRepository.deleteUserScans()
        }
    }

    /**
     * Получение информации о сканировании по ID
     */
    suspend fun getScanById(scanId: String): ScanHistoryEntity? {
        return scanRepository.getScanById(scanId)
    }

    /**
     * Сохранение сканирования с изображением
     */
    fun saveScanWithBitmap(searchQuery: SearchQuery, bitmap: Bitmap) {
        // Сохраняем сканирование только если пользователь авторизован
        if (isUserLoggedIn()) {
            viewModelScope.launch {
                scanRepository.insertScanWithBitmap(
                    searchQuery.query,
                    searchQuery.productType,
                    searchQuery.modelName,
                    searchQuery.brand,
                    searchQuery.color,
                    bitmap
                )
            }
        }
    }

    /**
     * Обновление списка сканирований пользователя
     */
    fun refreshUserScans() {
        Log.d(TAG, "Обновление списка сканирований")
        
        // Проверяем статус авторизации
        val isLoggedIn = isUserLoggedIn()
        Log.d(TAG, "Пользователь авторизован: $isLoggedIn")
        
        // Обновляем слушатель в репозитории
        scanRepository.updateUserScansListener()
    }
}