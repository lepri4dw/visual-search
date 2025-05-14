package com.example.visualsearch.ui.history

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.visualsearch.data.repository.FirestoreScanRepository

/**
 * Factory класс для создания ScanHistoryViewModel
 */
class ScanHistoryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Используем FirestoreScanRepository вместо ScanHistoryRepository
            return ScanHistoryViewModel(application, FirestoreScanRepository.getInstance()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}