package com.netprobe.diagnostics.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netprobe.diagnostics.scanner.LanScanner
import com.netprobe.diagnostics.topology.ForceDirectedLayout
import com.netprobe.diagnostics.topology.TopologyGraph
import com.netprobe.diagnostics.topology.TopologyNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TopologyUiState(
    val graph: TopologyGraph = TopologyGraph(),
    val isLayoutDone: Boolean = false,
    val selectedNode: TopologyNode? = null
)

class TopologyViewModel(application: Application) : AndroidViewModel(application) {

    private val lanScanner = LanScanner(application)
    private val _uiState = MutableStateFlow(TopologyUiState())
    val uiState: StateFlow<TopologyUiState> = _uiState.asStateFlow()

    fun buildTopology(hosts: List<com.netprobe.diagnostics.data.model.HostInfo>) {
        viewModelScope.launch {
            val subnetInfo = lanScanner.getSubnetInfo()
            val graph = TopologyGraph.fromHosts(
                hosts = hosts,
                gatewayIp = subnetInfo?.gatewayIp,
                selfIp = subnetInfo?.deviceIp
            )
            _uiState.value = _uiState.value.copy(graph = graph, isLayoutDone = false)

            val layoutGraph = ForceDirectedLayout.layout(
                graph = graph,
                canvasWidth = 800f,
                canvasHeight = 600f
            )
            _uiState.value = _uiState.value.copy(graph = layoutGraph, isLayoutDone = true)
        }
    }

    fun selectNode(node: TopologyNode?) {
        _uiState.value = _uiState.value.copy(selectedNode = node)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedNode = null)
    }
}
