package com.netprobe.diagnostics.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.netprobe.diagnostics.topology.TopologyNode
import com.netprobe.diagnostics.topology.NodeType
import com.netprobe.diagnostics.ui.theme.*

@Composable
fun NodeDetailPopup(
    node: TopologyNode?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = node != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        node?.let { n ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceCardDark)
                    .border(1.dp, SurfaceOverlay, RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "> NODE: ${n.label}",
                        style = MaterialTheme.typography.labelLarge,
                        color = when (n.type) {
                            NodeType.SELF -> NodeSelf
                            NodeType.GATEWAY -> NodeGateway
                            NodeType.HOST -> NodeHost
                            NodeType.UNKNOWN -> NodeUnknown
                        }
                    )
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onDismiss() },
                        tint = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Type", n.type.name)
                n.ipAddress?.let { DetailRow("IP", it) }
                n.macAddress?.let { DetailRow("MAC", it) }
                n.vendor?.let { DetailRow("Vendor", it) }
                DetailRow("Role", when {
                    n.isSelf -> "This Device"
                    n.isGateway -> "Gateway / Router"
                    else -> "Network Host"
                })
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextDisabled)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}
