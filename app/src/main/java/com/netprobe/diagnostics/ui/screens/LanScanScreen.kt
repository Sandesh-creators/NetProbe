package com.netprobe.diagnostics.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.netprobe.diagnostics.data.model.HostInfo
import com.netprobe.diagnostics.data.model.PortInfo
import com.netprobe.diagnostics.data.model.PortState
import com.netprobe.diagnostics.ui.theme.*
import com.netprobe.diagnostics.viewmodel.*

@Composable
fun LanScanScreen(viewModel: LanScanViewModel) {
    val scanState by viewModel.scanState.collectAsState()
    val portScanState by viewModel.portScanState.collectAsState()
    val pingState by viewModel.pingState.collectAsState()

    var selectedHost by remember { mutableStateOf<HostInfo?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(horizontal = 12.dp)
    ) {
        // ── Header ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> SUBNET RECON",
                style = MaterialTheme.typography.titleMedium,
                color = TerminalGreen
            )
            Text(
                text = when (scanState) {
                    is LanScanState.Scanning -> "${(scanState as LanScanState.Scanning).aliveHosts.size} HOSTS"
                    is LanScanState.Complete -> "${(scanState as LanScanState.Complete).aliveHosts.size} HOSTS"
                    else -> "IDLE"
                },
                style = MaterialTheme.typography.labelMedium,
                color = TerminalAmber
            )
        }

        // ── Network Info Bar ──────────────────────────────────
        AnimatedVisibility(visible = scanState is LanScanState.Scanning || scanState is LanScanState.Complete) {
            val info = when (scanState) {
                is LanScanState.Scanning -> "SUBNET: ${(scanState as LanScanState.Scanning).subnet}  GW: ${(scanState as LanScanState.Scanning).gateway}"
                is LanScanState.Complete -> "SUBNET: ${(scanState as LanScanState.Complete).subnet}  GW: ${(scanState as LanScanState.Complete).gateway}"
                else -> ""
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceCardDark)
                    .padding(10.dp)
            ) {
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodySmall,
                    color = TerminalCyan
                )
            }
        }

        // ── Scan Progress ─────────────────────────────────────
        if (scanState is LanScanState.Scanning) {
            val s = scanState as LanScanState.Scanning
            LinearProgressIndicator(
                progress = { s.scannedHosts.toFloat() / s.totalHosts },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = TerminalGreen,
                trackColor = TerminalDim,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scanning ${s.scannedHosts}/${s.totalHosts}...",
                style = MaterialTheme.typography.bodySmall,
                color = TextDisabled
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Host List ─────────────────────────────────────────
        val hosts = when (scanState) {
            is LanScanState.Scanning -> (scanState as LanScanState.Scanning).aliveHosts
            is LanScanState.Complete -> (scanState as LanScanState.Complete).aliveHosts
            else -> emptyList()
        }

        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(hosts, key = { it.ip }) { host ->
                HostRow(
                    host = host,
                    isSelected = selectedHost?.ip == host.ip,
                    onClick = { selectedHost = if (selectedHost?.ip == host.ip) null else host },
                    onPortScan = { viewModel.startPortScan(host.ip) },
                    onPing = { viewModel.startPing(host.ip) },
                    onStopPing = { viewModel.stopPing() },
                    isPinging = pingState is PingState.Pinging && (pingState as PingState.Pinging).host == host.ip,
                    pingState = if (pingState is PingState.Pinging && (pingState as PingState.Pinging).host == host.ip) {
                        pingState as PingState.Pinging
                    } else null
                )
            }
        }

        // ── Port Scan Results Panel ───────────────────────────
        AnimatedVisibility(
            visible = portScanState !is PortScanState.Idle,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PortScanPanel(
                portScanState = portScanState,
                onDismiss = { viewModel.dismissPortScan() }
            )
        }

        // ── Action Button ─────────────────────────────────────
        Button(
            onClick = {
                when (scanState) {
                    is LanScanState.Scanning -> viewModel.stopScan()
                    else -> viewModel.startSubnetScan()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (scanState is LanScanState.Scanning) TerminalRed else TerminalGreen,
                contentColor = SurfaceDark
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(
                imageVector = if (scanState is LanScanState.Scanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (scanState is LanScanState.Scanning) "STOP SCAN" else "INITIATE SCAN",
                style = MaterialTheme.typography.labelLarge,
                color = SurfaceDark
            )
        }
    }
}

@Composable
private fun HostRow(
    host: HostInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPortScan: () -> Unit,
    onPing: () -> Unit,
    onStopPing: () -> Unit,
    isPinging: Boolean,
    pingState: PingState.Pinging?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) SurfaceElevated else SurfaceCardDark)
            .border(
                width = 1.dp,
                color = if (isSelected) TerminalGreen.copy(alpha = 0.4f) else SurfaceOverlay,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = host.hostname?.let { "$it  //  ${host.ip}" } ?: host.ip,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TerminalGreen,
                    fontWeight = FontWeight.Bold
                )
                if (host.latencyMs > 0) {
                    Text(
                        text = "RTT: ${host.latencyMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            if (host.latencyMs > 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                host.latencyMs < 10 -> SignalStrong
                                host.latencyMs < 50 -> SignalMedium
                                else -> SignalWeak
                            }
                        )
                )
            }
        }

        // ── Expanded actions + live ping stats ────────────────
        AnimatedVisibility(visible = isSelected) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip("PORT SCAN", onPortScan)
                    if (isPinging) {
                        ActionChip("STOP PING", onStopPing, color = TerminalRed)
                    } else {
                        ActionChip("PING", onPing, color = TerminalCyan)
                    }
                }

                // ── Live Ping Feed ────────────────────────────
                if (pingState != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PingStatsRow(pingState)
                    pingState.lastResult?.let { result ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append("#${result.sequenceNumber} ")
                                append(if (result.isAlive) "${result.rttMs}ms" else "TIMEOUT")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (result.isAlive) TerminalGreen else TerminalRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PingStatsRow(state: PingState.Pinging) {
    val stats = state.stats ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(SurfaceDark)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatBadge("TX", "${stats.totalPackets}", TextSecondary)
        StatBadge("RX", "${stats.receivedPackets}", TerminalGreen)
        StatBadge("LOSS", "${String.format("%.1f", stats.packetLossPercent)}%", TerminalRed)
        StatBadge("AVG", "${stats.avgRtt}ms", TerminalCyan)
        StatBadge("JIT", "${stats.jitter}ms", TerminalAmber)
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextDisabled)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PortScanPanel(
    portScanState: PortScanState,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceCardDark)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> PORT SCAN: ${(portScanState as? PortScanState.Scanning)?.targetIp ?: (portScanState as PortScanState.Complete).targetIp}",
                style = MaterialTheme.typography.labelLarge,
                color = TerminalAmber
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

        if (portScanState is PortScanState.Scanning) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { portScanState.scannedPorts.toFloat() / portScanState.totalPorts },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = TerminalAmber,
                trackColor = TerminalDim,
            )
        }

        val openPorts = when (portScanState) {
            is PortScanState.Scanning -> portScanState.openPorts
            is PortScanState.Complete -> portScanState.openPorts
            is PortScanState.Idle -> emptyList()
        }

        if (openPorts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            openPorts.forEach { port ->
                PortRow(port)
            }
        } else if (portScanState is PortScanState.Complete) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No open ports detected on standard scan set.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDisabled
            )
        }
    }
}

@Composable
private fun PortRow(port: PortInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PortOpenColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${port.port}",
                style = MaterialTheme.typography.bodyMedium,
                color = TerminalGreen,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = port.service,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
        Text(
            text = port.protocol,
            style = MaterialTheme.typography.bodySmall,
            color = TextDisabled
        )
    }
}

@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = TerminalGreen
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
