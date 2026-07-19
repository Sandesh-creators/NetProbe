package com.netprobe.diagnostics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.netprobe.diagnostics.data.model.ChannelOccupancy
import com.netprobe.diagnostics.data.model.CongestionLevel
import com.netprobe.diagnostics.data.model.WifiBand
import com.netprobe.diagnostics.ui.components.ChannelCongestionChart
import com.netprobe.diagnostics.ui.components.CongestionLegend
import com.netprobe.diagnostics.ui.theme.*
import com.netprobe.diagnostics.viewmodel.AnalyzerState
import com.netprobe.diagnostics.viewmodel.ChannelAnalyzerViewModel

@Composable
fun ChannelAnalyzerScreen(viewModel: ChannelAnalyzerViewModel) {
    val analyzerState by viewModel.analyzerState.collectAsState()
    val selectedBand by viewModel.selectedBand.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> CHANNEL ANALYSIS",
                style = MaterialTheme.typography.titleMedium,
                color = TerminalAmber
            )
            val networkCount = when (analyzerState) {
                is AnalyzerState.Scanning -> (analyzerState as AnalyzerState.Scanning).networks.size
                else -> 0
            }
            Text(
                text = "$networkCount APs",
                style = MaterialTheme.typography.labelMedium,
                color = TerminalAmber
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BandChip("ALL", selectedBand == null) { viewModel.selectBand(null) }
            BandChip("2.4 GHz", selectedBand == WifiBand.BAND_2_4_GHZ) { viewModel.selectBand(WifiBand.BAND_2_4_GHZ) }
            BandChip("5 GHz", selectedBand == WifiBand.BAND_5_GHZ) { viewModel.selectBand(WifiBand.BAND_5_GHZ) }
        }

        // ── Error State ───────────────────────────────────────
        if (analyzerState is AnalyzerState.Error) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(TerminalRed.copy(alpha = 0.1f))
                    .border(1.dp, TerminalRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = TerminalRed)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (analyzerState as AnalyzerState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TerminalRed
                    )
                }
            }
        }

        // ── Congestion Chart ──────────────────────────────────
        val occupancy = when (analyzerState) {
            is AnalyzerState.Scanning -> (analyzerState as AnalyzerState.Scanning).channelOccupancy
            else -> emptyList()
        }

        val filteredOccupancy = remember(occupancy, selectedBand) {
            viewModel.getFilteredOccupancy(occupancy)
        }

        if (filteredOccupancy.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "SPECTRUM CONGESTION MAP",
                        style = MaterialTheme.typography.labelLarge,
                        color = TerminalAmber
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ChannelCongestionChart(
                        occupancy = filteredOccupancy,
                        bandFilter = selectedBand
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CongestionLegend()
                }
            }
        }

        // ── BLE Note ──────────────────────────────────────────
        if (selectedBand == null || selectedBand == WifiBand.BAND_2_4_GHZ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = TerminalCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BLE ADV on Ch 37/38/39 (2402/2426/2480 MHz) overlaps Wi-Fi Ch 1-13",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // ── Channel List ──────────────────────────────────────
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            items(filteredOccupancy, key = { it.channel }) { occ ->
                ChannelDetailRow(occ)
            }
        }

        // ── Scan Button ───────────────────────────────────────
        Button(
            onClick = {
                when (analyzerState) {
                    is AnalyzerState.Scanning -> viewModel.stopScan()
                    else -> viewModel.startContinuousScan()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (analyzerState is AnalyzerState.Scanning) TerminalRed else TerminalAmber,
                contentColor = SurfaceDark
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(
                imageVector = if (analyzerState is AnalyzerState.Scanning) Icons.Default.Stop else Icons.Default.Wifi,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (analyzerState is AnalyzerState.Scanning) "STOP SCAN" else "START SPECTRUM SCAN",
                style = MaterialTheme.typography.labelLarge,
                color = SurfaceDark
            )
        }
    }
}

@Composable
private fun ChannelDetailRow(occ: ChannelOccupancy) {
    val congestionColor = when (occ.congestionLevel) {
        CongestionLevel.CLEAR -> CongestionClear
        CongestionLevel.LOW -> CongestionLow
        CongestionLevel.MODERATE -> CongestionModerate
        CongestionLevel.HIGH -> CongestionHigh
        CongestionLevel.CRITICAL -> CongestionCritical
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceCardDark)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(congestionColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Ch ${occ.channel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = occ.band.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDisabled
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${occ.networkCount} AP${if (occ.networkCount != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = congestionColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = occ.congestionLevel.label,
                style = MaterialTheme.typography.labelSmall,
                color = congestionColor
            )
        }
    }
}

@Composable
private fun BandChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) TerminalAmber.copy(alpha = 0.15f)
                else SurfaceCardDark
            )
            .border(
                width = 1.dp,
                color = if (isSelected) TerminalAmber else SurfaceOverlay,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) TerminalAmber else TextSecondary
        )
    }
}
