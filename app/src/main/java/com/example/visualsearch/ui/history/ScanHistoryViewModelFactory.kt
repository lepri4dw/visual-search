package com.example.visualsearch.ui.history

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.visualsearch.data.repository.ScanHistoryRepository

/**
 * Factory класс для создания ScanHistoryViewModel
 */
class ScanHistoryViewModelFactory(private val application: Application, private val repository: ScanHistoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScanHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScanHistoryViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}