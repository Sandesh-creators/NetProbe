package com.netprobe.diagnostics.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rssi_samples",
    indices = [
        Index("sourceType"),
        Index("sourceAddress"),
        Index("timestamp")
    ]
)
data class RssiSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceType: String,
    val sourceAddress: String,
    val sourceName: String?,
    val rssi: Int,
    val frequency: Int?,
    val timestamp: Long
)
