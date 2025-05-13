package com.example.visualsearch.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.visualsearch.data.entity.ScanHistoryEntity

@Dao
interface ScanHistoryDao {
    @Insert
    suspend fun insertScan(scanHistory: ScanHistoryEntity): Long

    @Query("SELECT * FROM scan_history ORDER BY scanDate DESC")
    fun getAllScans(): LiveData<List<ScanHistoryEntity>>
    
    @Query("SELECT * FROM scan_history WHERE userId = :userId ORDER BY scanDate DESC")
    fun getScansByUserId(userId: String): LiveData<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE id = :scanId")
    suspend fun getScanById(scanId: Long): ScanHistoryEntity?

    @Delete
    suspend fun deleteScan(scanHistory: ScanHistoryEntity)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScans()
    
    @Query("DELETE FROM scan_history WHERE userId = :userId")
    suspend fun deleteUserScans(userId: String)
}
