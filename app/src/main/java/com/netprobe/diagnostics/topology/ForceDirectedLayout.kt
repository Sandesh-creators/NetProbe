package com.netprobe.diagnostics.topology

import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

object ForceDirectedLayout {

    private const val REPULSION = 8000f
    private const val ATTRACTION = 0.005f
    private const val DAMPING = 0.85f
    private const val MIN_DISTANCE = 50f
    private const val ITERATIONS = 120

    fun layout(
        graph: TopologyGraph,
        canvasWidth: Float,
        canvasHeight: Float,
        iterations: Int = ITERATIONS
    ): TopologyGraph {
        if (graph.nodes.isEmpty()) return graph

        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        val radius = minOf(canvasWidth, canvasHeight) * 0.35f

        val nodes = graph.nodes.map { node ->
            when (node.type) {
                NodeType.SELF -> node.copy(position = Offset(centerX, centerY + radius * 0.3f))
                NodeType.GATEWAY -> node.copy(position = Offset(centerX, centerY - radius * 0.3f))
                else -> {
                    val angle = (Math.random() * 2 * Math.PI).toFloat()
                    val r = (Math.random() * radius).toFloat()
                    node.copy(position = Offset(centerX + r * kotlin.math.cos(angle), centerY + r * kotlin.math.sin(angle)))
                }
            }
        }.toMutableList()

        val nodePositions = nodes.associateBy({ it.id }, { it.position }).toMutableMap()

        repeat(iterations) {
            val forces = mutableMapOf<String, Offset>()

            for (i in nodes.indices) {
                var fx = 0f
                var fy = 0f

                for (j in nodes.indices) {
                    if (i == j) continue
                    val posA = nodePositions[nodes[i].id] ?: continue
                    val posB = nodePositions[nodes[j].id] ?: continue
                    val dx = posA.x - posB.x
                    val dy = posA.y - posB.y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(MIN_DISTANCE)
                    val force = REPULSION / (dist * dist)
                    fx += (dx / dist) * force
                    fy += (dy / dist) * force
                }

                for (edge in graph.edges) {
                    val otherId = when (nodes[i].id) {
                        edge.fromId -> edge.toId
                        edge.toId -> edge.fromId
                        else -> null
                    } ?: continue
                    val posA = nodePositions[nodes[i].id] ?: continue
                    val posB = nodePositions[otherId] ?: continue
                    val dx = posB.x - posA.x
                    val dy = posB.y - posA.y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(MIN_DISTANCE)
                    fx += dx * ATTRACTION
                    fy += dy * ATTRACTION
                }

                forces[nodes[i].id] = Offset(fx, fy)
            }

            for (node in nodes) {
                val force = forces[node.id] ?: continue
                val currentPos = nodePositions[node.id] ?: continue
                val newX = (currentPos.x + force.x).coerceIn(40f, canvasWidth - 40f)
                val newY = (currentPos.y + force.y).coerceIn(40f, canvasHeight - 40f)
                nodePositions[node.id] = Offset(newX, newY)
            }
        }

        val finalNodes = nodes.map { node ->
            node.copy(position = nodePositions[node.id] ?: node.position)
        }

        return graph.copy(nodes = finalNodes)
    }
}
