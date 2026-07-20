package com.netprobe.diagnostics.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.netprobe.diagnostics.data.db.entity.AssetEventEntity
import com.netprobe.diagnostics.data.db.entity.DiscoveredAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAsset(asset: DiscoveredAssetEntity): Long

    @Insert
    suspend fun insertEvent(event: AssetEventEntity)

    @Query("SELECT * FROM discovered_assets ORDER BY lastSeen DESC")
    fun getAllAssets(): Flow<List<DiscoveredAssetEntity>>

    @Query("SELECT * FROM discovered_assets WHERE isFlagged = 1 ORDER BY lastSeen DESC")
    fun getFlaggedAssets(): Flow<List<DiscoveredAssetEntity>>

    @Query("SELECT * FROM discovered_assets WHERE isKnown = 0 ORDER BY firstSeen DESC")
    fun getUnknownAssets(): Flow<List<DiscoveredAssetEntity>>

    @Query("SELECT * FROM discovered_assets WHERE macAddress = :mac LIMIT 1")
    suspend fun findByMac(mac: String): DiscoveredAssetEntity?

    @Query("UPDATE discovered_assets SET isFlagged = :flagged WHERE macAddress = :mac")
    suspend fun setFlagged(mac: String, flagged: Boolean)

    @Query("UPDATE discovered_assets SET isKnown = :known WHERE macAddress = :mac")
    suspend fun setKnown(mac: String, known: Boolean)

    @Query("UPDATE discovered_assets SET notes = :notes WHERE macAddress = :mac")
    suspend fun setNotes(mac: String, notes: String?)

    @Query("DELETE FROM discovered_assets WHERE lastSeen < :olderThan")
    suspend fun pruneStale(olderThan: Long)

    @Query("SELECT COUNT(*) FROM discovered_assets")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM discovered_assets WHERE isFlagged = 1")
    suspend fun getFlaggedCount(): Int
}
