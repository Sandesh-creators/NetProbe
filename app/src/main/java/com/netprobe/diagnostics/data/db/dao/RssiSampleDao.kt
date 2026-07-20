package com.netprobe.diagnostics.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.netprobe.diagnostics.data.db.entity.RssiSampleEntity
import kotlinx.coroutines.flow.Flow

data class DeviceRssiSummary(
    val sourceAddress: String,
    val sourceName: String?,
    val sourceType: String,
    val avgRssi: Int,
    val minRssi: Int,
    val maxRssi: Int,
    val sampleCount: Int
)

@Dao
interface RssiSampleDao {
    @Insert
    suspend fun insert(sample: RssiSampleEntity)

    @Insert
    suspend fun insertAll(samples: List<RssiSampleEntity>)

    @Query("SELECT * FROM rssi_samples WHERE sourceType = :type AND timestamp > :since ORDER BY timestamp ASC")
    suspend fun getSamplesSince(type: String, since: Long): List<RssiSampleEntity>

    @Query("SELECT * FROM rssi_samples WHERE sourceAddress = :address AND timestamp > :since ORDER BY timestamp ASC")
    suspend fun getSamplesForDevice(address: String, since: Long): List<RssiSampleEntity>

    @Query("SELECT * FROM rssi_samples WHERE timestamp > :since ORDER BY timestamp ASC")
    fun getAllSamplesFlow(since: Long): Flow<List<RssiSampleEntity>>

    @Query("SELECT DISTINCT sourceAddress FROM rssi_samples WHERE timestamp > :since")
    suspend fun getActiveDeviceAddresses(since: Long): List<String>

    @Query("DELETE FROM rssi_samples WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)

    @Query("""
        SELECT sourceAddress, sourceName, sourceType, 
               AVG(rssi) as avgRssi, MIN(rssi) as minRssi, 
               MAX(rssi) as maxRssi, COUNT(*) as sampleCount 
        FROM rssi_samples 
        WHERE timestamp > :since 
        GROUP BY sourceAddress
    """)
    suspend fun getDeviceSummary(since: Long): List<DeviceRssiSummary>
}
