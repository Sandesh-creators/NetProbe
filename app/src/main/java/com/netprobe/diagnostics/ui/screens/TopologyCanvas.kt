package com.netprobe.diagnostics.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netprobe.diagnostics.topology.*
import com.netprobe.diagnostics.ui.theme.*

@Composable
fun TopologyCanvas(
    graph: TopologyGraph,
    onNodeTap: (TopologyNode) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCardDark)
            .border(1.dp, SurfaceOverlay, RoundedCornerShape(8.dp))
            .pointerInput(graph) {
                detectTapGestures { offset ->
                    val tapped = graph.nodes.minByOrNull { node ->
                        val dx = node.position.x - offset.x
                        val dy = node.position.y - offset.y
                        dx * dx + dy * dy
                    }
                    if (tapped != null) {
                        val dx = tapped.position.x - offset.x
                        val dy = tapped.position.y - offset.y
                        if (dx * dx + dy * dy < 1500f) {
                            onNodeTap(tapped)
                        }
                    }
                }
            }
    ) {
        val nodeRadius = 18f

        for (edge in graph.edges) {
            val fromNode = graph.nodes.find { it.id == edge.fromId } ?: continue
            val toNode = graph.nodes.find { it.id == edge.toId } ?: continue
            drawLine(
                color = if (edge.isActive) EdgeActive else EdgeInactive,
                start = fromNode.position,
                end = toNode.position,
                strokeWidth = 2f
            )
        }

        for (node in graph.nodes) {
            val color = when (node.type) {
                NodeType.SELF -> NodeSelf
                NodeType.GATEWAY -> NodeGateway
                NodeType.HOST -> NodeHost
                NodeType.UNKNOWN -> NodeUnknown
            }

            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = nodeRadius + 6f,
                center = node.position
            )
            drawCircle(
                color = color,
                radius = nodeRadius,
                center = node.position
            )

            val paint = android.graphics.Paint().apply {
                this.color = android.graphics.Color.DKGRAY
                textSize = 10f * density
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                node.label.take(12),
                node.position.x,
                node.position.y + nodeRadius + 14f,
                paint
            )
        }
    }
}
