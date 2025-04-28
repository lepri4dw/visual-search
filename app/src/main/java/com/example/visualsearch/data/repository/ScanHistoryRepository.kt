package com.example.visualsearch.data.repository

import androidx.lifecycle.LiveData
import com.example.visualsearch.data.dao.ScanHistoryDao
import com.example.visualsearch.data.entity.ScanHistoryEntity

class ScanHistoryRepository(private val scanHistoryDao: ScanHistoryDao) {
    val allScans: LiveData<List<ScanHistoryEntity>> = scanHistoryDao.getAllScans()

    suspend fun insertScan(scanHistory: ScanHistoryEntity): Long {
        return scanHistoryDao.insertScan(scanHistory)
    }

    suspend fun getScanById(id: Long): ScanHistoryEntity? {
        return scanHistoryDao.getScanById(id)
    }

    suspend fun deleteScan(scanHistory: ScanHistoryEntity) {
        scanHistoryDao.deleteScan(scanHistory)
    }

    suspend fun deleteAllScans() {
        scanHistoryDao.deleteAllScans()
    }
}
