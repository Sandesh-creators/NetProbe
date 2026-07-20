package com.netprobe.diagnostics.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "asset_events",
    indices = [
        Index("assetId"),
        Index("eventTime")
    ]
)
data class AssetEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetId: Long,
    val eventType: String,
    val ipAddress: String?,
    val rssi: Int?,
    val eventTime: Long
)
