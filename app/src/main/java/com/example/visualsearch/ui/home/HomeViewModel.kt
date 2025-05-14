package com.example.visualsearch.ui.home

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.visualsearch.remote.gemini.SearchQuery

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Отсканируйте предмет для распознавания"
    }
    val text: LiveData<String> = _text
    
    // Для хранения данных из истории сканирования
    private val _currentSearchQuery = MutableLiveData<SearchQuery?>()
    val currentSearchQuery: LiveData<SearchQuery?> = _currentSearchQuery
    
    private val _currentBitmap = MutableLiveData<Bitmap?>()
    val currentBitmap: LiveData<Bitmap?> = _currentBitmap
    
    private val _fromHistory = MutableLiveData<Boolean>()
    val fromHistory: LiveData<Boolean> = _fromHistory
    
    fun setCurrentSearchQuery(searchQuery: SearchQuery) {
        _currentSearchQuery.value = searchQuery
        _fromHistory.value = true
    }
    
    fun setCurrentBitmap(bitmap: Bitmap) {
        _currentBitmap.value = bitmap
    }
    
    fun resetHistoryData() {
        _fromHistory.value = false
        _currentSearchQuery.value = null
        _currentBitmap.value = null
    }
}