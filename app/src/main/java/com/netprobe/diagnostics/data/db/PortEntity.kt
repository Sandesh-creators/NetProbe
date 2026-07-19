package com.netprobe.diagnostics.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "port_dictionary")
data class PortEntity(
    @PrimaryKey val port: Int,
    val service: String,
    val protocol: String,
    val description: String
)
