package com.netprobe.diagnostics.data.model

data class HostInfo(
    val ip: String,
    val hostname: String?,
    val macAddress: String?,
    val isAlive: Boolean,
    val latencyMs: Long = 0L,
    val openPorts: List<PortInfo> = emptyList()
)

data class PortInfo(
    val port: Int,
    val protocol: String,
    val service: String,
    val state: PortState
)

enum class PortState {
    OPEN,
    CLOSED,
    FILTERED
}
