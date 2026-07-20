package com.netprobe.diagnostics.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "discovered_assets",
    indices = [
        Index("macAddress", unique = true),
        Index("isKnown"),
        Index("isFlagged")
    ]
)
data class DiscoveredAssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val macAddress: String,
    val ipAddress: String?,
    val hostname: String?,
    val deviceName: String?,
    val source: String,
    val deviceType: String,
    val firstSeen: Long,
    val lastSeen: Long,
    val isKnown: Boolean,
    val isFlagged: Boolean = false,
    val ouiVendor: String? = null,
    val notes: String? = null
)
