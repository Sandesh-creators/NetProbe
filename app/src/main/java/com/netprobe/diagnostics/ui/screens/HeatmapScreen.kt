package com.netprobe.diagnostics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netprobe.diagnostics.ui.theme.*
import com.netprobe.diagnostics.viewmodel.HeatmapViewModel

@Composable
fun HeatmapScreen(viewModel: HeatmapViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val summaries by viewModel.deviceSummaries.collectAsState()
    var selectedDevice by remember { mutableStateOf<String?>(null) }

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
                text = "> SIGNAL HEATMAP",
                style = MaterialTheme.typography.titleMedium,
                color = TerminalAmber
            )
            Text(
                text = "${summaries.size} DEVICES",
                style = MaterialTheme.typography.labelMedium,
                color = TerminalGreen
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("2s" to 2000L, "5s" to 5000L, "10s" to 10000L).forEach { (label, ms) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (uiState.intervalMs == ms) TerminalCyan.copy(alpha = 0.15f) else SurfaceCardDark
                        )
                        .border(
                            1.dp,
                            if (uiState.intervalMs == ms) TerminalCyan.copy(alpha = 0.4f) else SurfaceOverlay,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { viewModel.setInterval(ms) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "INTERVAL: $label",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.intervalMs == ms) TerminalCyan else TextSecondary
                    )
                }
            }
        }

        SignalHeatmapGrid(
            summaries = summaries,
            selectedDevice = selectedDevice,
            onDeviceSelected = { selectedDevice = if (selectedDevice == it) null else it }
        )

        if (summaries.isNotEmpty()) {
            RssiTimeSeriesChart(
                summaries = summaries,
                selectedDevice = selectedDevice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = { viewModel.toggleCollecting() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isCollecting) TerminalRed else TerminalGreen,
                contentColor = SurfaceDark
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(
                imageVector = if (uiState.isCollecting) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (uiState.isCollecting) "STOP COLLECTION" else "START COLLECTION",
                style = MaterialTheme.typography.labelLarge,
                color = SurfaceDark
            )
        }
    }
}
