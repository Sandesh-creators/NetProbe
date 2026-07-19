package com.netprobe.diagnostics.data.model

data class PingResult(
    val sequenceNumber: Int,
    val rttMs: Long,
    val isAlive: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class PingStats(
    val host: String,
    val totalPackets: Int = 0,
    val receivedPackets: Int = 0,
    val lostPackets: Int = 0,
    val minRtt: Long = Long.MAX_VALUE,
    val maxRtt: Long = 0L,
    val avgRtt: Long = 0L,
    val jitter: Long = 0L,
    val packetLossPercent: Float = 0f
)
