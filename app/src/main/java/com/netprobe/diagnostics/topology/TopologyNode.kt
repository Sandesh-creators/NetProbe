package com.netprobe.diagnostics.topology

import androidx.compose.ui.geometry.Offset

data class TopologyNode(
    val id: String,
    val label: String,
    val type: NodeType,
    var position: Offset = Offset.Zero,
    var velocity: Offset = Offset.Zero,
    val macAddress: String? = null,
    val ipAddress: String? = null,
    val vendor: String? = null,
    val isSelf: Boolean = false,
    val isGateway: Boolean = false
)

enum class NodeType {
    SELF, GATEWAY, HOST, UNKNOWN
}
