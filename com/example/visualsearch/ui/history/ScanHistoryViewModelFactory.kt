package com.example.visualsearch.ui.history

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.visualsearch.data.repository.ScanHistoryRepository

/**
 * Factory для создания ScanHistoryViewModel с необходимыми зависимостями
 */
class ScanHistoryViewModelFactory(
    private val application: Application,
    private val scanHistoryRepository: ScanHistoryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanHistoryViewModel::class.java)) {
            return ScanHistoryViewModel(application, scanHistoryRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}