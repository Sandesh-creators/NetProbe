package com.netprobe.diagnostics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netprobe.diagnostics.ui.theme.*
import com.netprobe.diagnostics.viewmodel.TopologyViewModel

@Composable
fun TopologyMapScreen(
    viewModel: TopologyViewModel,
    hostCount: Int
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> NETWORK TOPOLOGY",
                style = MaterialTheme.typography.titleMedium,
                color = TerminalAmber
            )
            Text(
                text = "${uiState.graph.nodes.size} NODES",
                style = MaterialTheme.typography.labelMedium,
                color = TerminalGreen
            )
        }

        if (uiState.graph.nodes.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = TextDisabled
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "NO TOPOLOGY DATA",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Run a LAN scan first, then tap\nBUILD TOPOLOGY to visualize",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                }
            }
        } else {
            TopologyCanvas(
                graph = uiState.graph,
                onNodeTap = { viewModel.selectNode(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        NodeDetailPopup(
            node = uiState.selectedNode,
            onDismiss = { viewModel.clearSelection() }
        )

        Button(
            onClick = { /* handled externally */ },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TerminalAmber,
                contentColor = SurfaceDark
            ),
            shape = RoundedCornerShape(6.dp),
            enabled = false
        ) {
            Icon(Icons.Default.Map, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TOPOLOGY: $hostCount HOSTS MAPPED",
                style = MaterialTheme.typography.labelLarge,
                color = SurfaceDark
            )
        }
    }
}
