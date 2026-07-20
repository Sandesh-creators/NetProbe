package com.netprobe.diagnostics.topology

data class TopologyEdge(
    val fromId: String,
    val toId: String,
    val isActive: Boolean = true,
    val latencyMs: Long = 0
)
