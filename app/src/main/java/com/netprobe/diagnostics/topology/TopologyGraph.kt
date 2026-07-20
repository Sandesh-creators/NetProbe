package com.netprobe.diagnostics.topology

import com.netprobe.diagnostics.data.model.HostInfo

data class TopologyGraph(
    val nodes: List<TopologyNode> = emptyList(),
    val edges: List<TopologyEdge> = emptyList()
) {
    companion object {
        fun fromHosts(hosts: List<HostInfo>, gatewayIp: String?, selfIp: String?): TopologyGraph {
            val nodeMap = mutableMapOf<String, TopologyNode>()
            val edges = mutableListOf<TopologyEdge>()

            gatewayIp?.let { gw ->
                nodeMap["gw"] = TopologyNode(
                    id = "gw",
                    label = "Gateway",
                    type = NodeType.GATEWAY,
                    ipAddress = gw,
                    isGateway = true
                )
            }

            selfIp?.let { self ->
                nodeMap["self"] = TopologyNode(
                    id = "self",
                    label = "This Device",
                    type = NodeType.SELF,
                    ipAddress = self,
                    isSelf = true
                )
                gatewayIp?.let {
                    edges.add(TopologyEdge(fromId = "self", toId = "gw"))
                }
            }

            hosts.forEach { host ->
                if (host.ip == gatewayIp || host.ip == selfIp) return@forEach
                val nodeId = "host_${host.ip}"
                nodeMap[nodeId] = TopologyNode(
                    id = nodeId,
                    label = host.hostname ?: host.ip,
                    type = NodeType.HOST,
                    macAddress = host.macAddress,
                    ipAddress = host.ip
                )
                edges.add(TopologyEdge(
                    fromId = nodeId,
                    toId = "gw",
                    latencyMs = host.latencyMs
                ))
            }

            return TopologyGraph(
                nodes = nodeMap.values.toList(),
                edges = edges
            )
        }
    }
}
