package com.example.visualsearch.ui.history

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.visualsearch.data.AppDatabase
import com.example.visualsearch.data.entity.ScanHistoryEntity
import com.example.visualsearch.data.repository.ScanHistoryRepository
import com.example.visualsearch.remote.gemini.SearchQuery
import com.example.visualsearch.util.ImageSaver
import kotlinx.coroutines.launch

class ScanHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScanHistoryRepository
    val allScans: LiveData<List<ScanHistoryEntity>>
    private val _selectedScan = MutableLiveData<ScanHistoryEntity?>()
    val selectedScan: LiveData<ScanHistoryEntity?> = _selectedScan

    init {
        val scanHistoryDao = AppDatabase.getDatabase(application).scanHistoryDao()
        repository = ScanHistoryRepository(scanHistoryDao)
        allScans = repository.allScans
    }

    fun saveScan(searchQuery: SearchQuery, imagePath: String) {
        viewModelScope.launch {
            val scanHistory = ScanHistoryEntity(
                query = searchQuery.query,
                productType = searchQuery.productType,
                modelName = searchQuery.modelName,
                brand = searchQuery.brand,
                color = searchQuery.color,
                imagePath = imagePath
            )
            repository.insertScan(scanHistory)
        }
    }

    fun saveScanWithBitmap(searchQuery: SearchQuery, bitmap: Bitmap) {
        val imagePath = ImageSaver.saveImageToInternalStorage(getApplication(), bitmap)
        if (imagePath != null) {
            saveScan(searchQuery, imagePath)
        }
    }

    fun getScanById(id: Long) {
        viewModelScope.launch {
            _selectedScan.value = repository.getScanById(id)
        }
    }

    fun deleteScan(scan: ScanHistoryEntity) {
        viewModelScope.launch {
            ImageSaver.deleteImage(scan.imagePath)
            repository.deleteScan(scan)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val scans = allScans.value
            scans?.forEach { scan ->
                ImageSaver.deleteImage(scan.imagePath)
            }
            repository.deleteAllScans()
        }
    }
}
