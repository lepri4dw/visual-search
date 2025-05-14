package com.example.visualsearch.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.visualsearch.data.entity.ScanHistoryEntity

/**
 * DAO для истории сканирований - заглушка для обратной совместимости
 * Реальные данные хранятся в Firebase Firestore
 */
@Dao
interface ScanHistoryDao {
    // Пустые методы, необходимые для компиляции
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scanHistory: ScanHistoryEntity): Long
    
    @Query("SELECT * FROM scan_history WHERE user_id = :userId ORDER BY scan_date DESC LIMIT 1")
    fun getScansByUserId(userId: String): LiveData<List<ScanHistoryEntity>>
    
    @Query("SELECT * FROM scan_history ORDER BY scan_date DESC LIMIT 1")
    fun getAllScans(): LiveData<List<ScanHistoryEntity>>
    
    @Query("SELECT * FROM scan_history WHERE roomId = :scanId LIMIT 1")
    suspend fun getScanById(scanId: Long): ScanHistoryEntity?
    
    @Delete
    suspend fun deleteScan(scanHistory: ScanHistoryEntity)
    
    @Query("DELETE FROM scan_history WHERE user_id = :userId")
    suspend fun deleteUserScans(userId: String)
} 