package com.netprobe.diagnostics.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netprobe.diagnostics.data.model.HostInfo
import com.netprobe.diagnostics.data.model.PortInfo
import com.netprobe.diagnostics.data.model.PortState
import com.netprobe.diagnostics.scanner.TracerouteHop
import com.netprobe.diagnostics.ui.theme.*
import com.netprobe.diagnostics.viewmodel.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LanScanScreen(viewModel: LanScanViewModel) {
    val scanState by viewModel.scanState.collectAsState()
    val portScanState by viewModel.portScanState.collectAsState()
    val pingState by viewModel.pingState.collectAsState()
    val tracerouteState by viewModel.tracerouteState.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val sshDialogState by viewModel.sshDialogState.collectAsState()
    val context = LocalContext.current

    var selectedHost by remember { mutableStateOf<HostInfo?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    if (sshDialogState is SshDialogState.Showing) {
        val apps = (sshDialogState as SshDialogState.Showing).apps
        AlertDialog(
            onDismissRequest = { viewModel.dismissSshDialog() },
            containerColor = SurfaceCardDark,
            titleContentColor = TerminalAmber,
            textContentColor = TextSecondary,
            title = { Text("> SSH CLIENT REQUIRED") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "No SSH client found on this device. Install one to connect directly from NetProbe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                    Text(
                        text = "Installed apps will auto-launch when you tap SSH IN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                    HorizontalDivider(color = SurfaceOverlay)
                    apps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    viewModel.openInstallUrl(app.installUrl)
                                    viewModel.dismissSshDialog()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TerminalCyan
                            )
                            Text(
                                text = "INSTALL",
                                style = MaterialTheme.typography.labelSmall,
                                color = TerminalGreen
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSshDialog() }) {
                    Text("CLOSE", color = TerminalAmber)
                }
            }
        )
    }

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
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceCardDark)
                        .padding(10.dp)
                ) {
                    Text(text = info, style = MaterialTheme.typography.bodySmall, color = TerminalCyan)
                }
            }

            if (scanState is LanScanState.Error) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(TerminalRed.copy(alpha = 0.1f))
                        .border(1.dp, TerminalRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = (scanState as LanScanState.Error).message,
                        style = MaterialTheme.typography.bodySmall,
                        color = TerminalRed
                    )
                }
            }

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

            val hosts = when (scanState) {
                is LanScanState.Scanning -> (scanState as LanScanState.Scanning).aliveHosts
                is LanScanState.Complete -> (scanState as LanScanState.Complete).aliveHosts
                else -> emptyList()
            }

            if (hosts.isEmpty() && scanState is LanScanState.Idle) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Radar,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = TextDisabled
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO SCAN DATA",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap INITIATE SCAN below to discover\nhosts on your local network",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDisabled
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = rememberLazyListState(),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(hosts, key = { it.ip }) { host ->
                        val openCount = portScanState.let { ps ->
                            when (ps) {
                                is PortScanState.Complete -> if (ps.targetIp == host.ip) ps.openPorts.size else null
                                is PortScanState.Scanning -> if (ps.targetIp == host.ip) ps.openPorts.size else null
                                else -> null
                            }
                        }
                        val hasSsh = portScanState.let { ps ->
                            (ps as? PortScanState.Complete)?.let {
                                it.targetIp == host.ip && it.openPorts.any { p -> p.port == 22 }
                            } == true || (ps as? PortScanState.Scanning)?.let {
                                it.targetIp == host.ip && it.openPorts.any { p -> p.port == 22 }
                            } == true
                        }

                        HostRow(
                            host = host,
                            isSelected = selectedHost?.ip == host.ip,
                            onClick = { selectedHost = if (selectedHost?.ip == host.ip) null else host },
                            onPortScan = { viewModel.startPortScan(host.ip) },
                            onPing = { viewModel.startPing(host.ip) },
                            onStopPing = { viewModel.stopPing() },
                            onSsh = { viewModel.openSsh(host.ip) },
                            onTraceroute = { viewModel.startTraceroute(host.ip) },
                            hasSsh = hasSsh,
                            openPortsCount = openCount,
                            isPinging = pingState is PingState.Pinging && (pingState as PingState.Pinging).host == host.ip,
                            pingState = if (pingState is PingState.Pinging && (pingState as PingState.Pinging).host == host.ip) {
                                pingState as PingState.Pinging
                            } else null,
                            tracerouteState = if (tracerouteState is TracerouteViewState.Tracing &&
                                (tracerouteState as TracerouteViewState.Tracing).targetIp == host.ip
                            ) {
                                tracerouteState as TracerouteViewState.Tracing
                            } else null,
                            isTracing = tracerouteState is TracerouteViewState.Tracing &&
                                (tracerouteState as TracerouteViewState.Tracing).targetIp == host.ip,
                            onStopTraceroute = { viewModel.stopTraceroute() }
                        )
                    }
                }
            }

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

            AnimatedVisibility(
                visible = tracerouteState is TracerouteViewState.Tracing ||
                    tracerouteState is TracerouteViewState.Complete,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TraceroutePanel(
                    tracerouteState = tracerouteState,
                    onDismiss = { viewModel.stopTraceroute() }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        when (scanState) {
                            is LanScanState.Scanning -> viewModel.stopScan()
                            else -> viewModel.startSubnetScan()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (scanState is LanScanState.Scanning) TerminalRed else TerminalGreen,
                        contentColor = SurfaceDark
                    ),
                    shape = RoundedCornerShape(6.dp)
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

                if (hosts.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val text = buildExportText(hosts, portScanState, scanState)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_SUBJECT, "NetProbe LAN Scan Results")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Scan Results"))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TerminalCyan.copy(alpha = 0.15f))
                            .border(1.dp, TerminalCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Export Results",
                            tint = TerminalCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
    onSsh: () -> Unit,
    onTraceroute: () -> Unit,
    hasSsh: Boolean,
    openPortsCount: Int?,
    isPinging: Boolean,
    pingState: PingState.Pinging?,
    tracerouteState: TracerouteViewState.Tracing?,
    isTracing: Boolean,
    onStopTraceroute: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) SurfaceElevated else SurfaceCardDark)
            .border(
                width = 1.dp,
                color = if (isSelected) TerminalGreen.copy(alpha = 0.4f) else SurfaceOverlay,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .animateContentSize()
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    host.latencyMs < 10 -> SignalStrong
                                    host.latencyMs < 50 -> SignalMedium
                                    host.latencyMs > 0 -> SignalWeak
                                    else -> TextDisabled
                                }
                            )
                    )
                    Text(
                        text = host.hostname?.let { "$it  //  ${host.ip}" } ?: host.ip,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TerminalGreen,
                        fontWeight = FontWeight.Bold
                    )
                    if (openPortsCount != null && openPortsCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TerminalAmber.copy(alpha = 0.15f))
                                .border(1.dp, TerminalAmber.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$openPortsCount OPEN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TerminalAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                if (host.latencyMs > 0) {
                    Text(
                        text = "RTT: ${host.latencyMs}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            if (hasSsh) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(TerminalCyan.copy(alpha = 0.1f))
                        .border(1.dp, TerminalCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .clickable { onSsh() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = "SSH",
                            modifier = Modifier.size(12.dp),
                            tint = TerminalCyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SSH",
                            style = MaterialTheme.typography.labelSmall,
                            color = TerminalCyan
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = isSelected) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ActionChip("PORTS", Icons.Default.ScatterPlot, onPortScan)
                    if (isPinging) {
                        ActionChip("STOP PING", Icons.Default.Stop, onStopPing, color = TerminalRed)
                    } else {
                        ActionChip("PING", Icons.Default.Speed, onPing, color = TerminalCyan)
                    }
                    if (isTracing) {
                        ActionChip("STOP TRACE", Icons.Default.Stop, onStopTraceroute, color = TerminalRed)
                    } else {
                        ActionChip("TRACE", Icons.Default.Route, onTraceroute, color = TerminalAmber)
                    }
                    if (hasSsh) {
                        ActionChip("SSH IN", Icons.Default.Terminal, onSsh, color = TerminalCyan)
                    }
                }

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
private fun TraceroutePanel(
    tracerouteState: TracerouteViewState,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceCardDark)
            .padding(10.dp)
            .animateContentSize()
    ) {
        val targetIp = when (tracerouteState) {
            is TracerouteViewState.Tracing -> tracerouteState.targetIp
            is TracerouteViewState.Complete -> tracerouteState.targetIp
            else -> return
        }
        val hops = when (tracerouteState) {
            is TracerouteViewState.Tracing -> tracerouteState.hops
            is TracerouteViewState.Complete -> tracerouteState.hops
            else -> emptyList()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "> TRACEROUTE: $targetIp",
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

        if (tracerouteState is TracerouteViewState.Tracing) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = TerminalAmber,
                trackColor = TerminalDim,
            )
        }

        if (hops.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            hops.forEach { hop ->
                TracerouteHopRow(hop)
            }
        }
    }
}

@Composable
private fun TracerouteHopRow(hop: TracerouteHop) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = String.format("%-3d", hop.ttl),
                style = MaterialTheme.typography.bodySmall,
                color = TextDisabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = hop.ip ?: "*",
                style = MaterialTheme.typography.bodySmall,
                color = if (hop.ip != null) TerminalGreen else TextDisabled,
                fontWeight = FontWeight.Bold
            )
            hop.hostname?.let {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($it)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Text(
            text = if (hop.rttMs > 0) "${hop.rttMs}ms" else "*",
            style = MaterialTheme.typography.bodySmall,
            color = if (hop.rttMs > 0) TerminalCyan else TextDisabled
        )
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
        StatBadge("MIN", "${stats.minRtt}ms", TerminalCyan)
        StatBadge("AVG", "${stats.avgRtt}ms", TerminalCyan)
        StatBadge("MAX", "${stats.maxRtt}ms", TerminalAmber)
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
                text = "> PORT SCAN: ${(portScanState as? PortScanState.Scanning)?.targetIp ?: (portScanState as? PortScanState.Complete)?.targetIp ?: ""}",
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
    val stateColor = when (port.state) {
        PortState.OPEN -> PortOpenColor
        PortState.FILTERED -> TerminalAmber
        PortState.CLOSED -> PortClosedColor
    }
    val stateLabel = when (port.state) {
        PortState.OPEN -> "OPEN"
        PortState.FILTERED -> "FILTERED"
        PortState.CLOSED -> "CLOSED"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(stateColor)
            )
            Text(
                text = "${port.port}",
                style = MaterialTheme.typography.bodyMedium,
                color = TerminalGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = port.service,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = stateColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = port.protocol,
                style = MaterialTheme.typography.bodySmall,
                color = TextDisabled
            )
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    color: androidx.compose.ui.graphics.Color = TerminalGreen
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(11.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

private fun buildExportText(
    hosts: List<HostInfo>,
    portScanState: PortScanState,
    scanState: LanScanState
): String {
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    val subnet = when (scanState) {
        is LanScanState.Scanning -> scanState.subnet
        is LanScanState.Complete -> scanState.subnet
        else -> "N/A"
    }
    val gateway = when (scanState) {
        is LanScanState.Scanning -> scanState.gateway
        is LanScanState.Complete -> scanState.gateway
        else -> "N/A"
    }

    return buildString {
        appendLine("═══════════════════════════════════════")
        appendLine("  NetProbe LAN Scan Report")
        appendLine("═══════════════════════════════════════")
        appendLine()
        appendLine("Timestamp : $timestamp")
        appendLine("Subnet    : $subnet")
        appendLine("Gateway   : $gateway")
        appendLine("Hosts     : ${hosts.size}")
        appendLine()
        appendLine("── Hosts ──────────────────────────────")

        hosts.forEachIndexed { index, host ->
            appendLine()
            appendLine("  #${index + 1}  ${host.hostname ?: "Unknown"}")
            appendLine("      IP      : ${host.ip}")
            if (host.macAddress != null) {
                appendLine("      MAC     : ${host.macAddress}")
            }
            appendLine("      Latency : ${host.latencyMs}ms")
            if (host.openPorts.isNotEmpty()) {
                appendLine("      Ports   : ${host.openPorts.joinToString(", ") { "${it.port}/${it.service}" }}")
            }
        }

        val targetIp = when (portScanState) {
            is PortScanState.Scanning -> portScanState.targetIp
            is PortScanState.Complete -> portScanState.targetIp
            is PortScanState.Idle -> null
        }
        val allPorts = when (portScanState) {
            is PortScanState.Scanning -> portScanState.openPorts
            is PortScanState.Complete -> portScanState.openPorts
            is PortScanState.Idle -> emptyList()
        }

        if (targetIp != null) {
            appendLine()
            appendLine("── Port Scan: $targetIp ────────────────")
            if (allPorts.isNotEmpty()) {
                allPorts.forEach { port ->
                    appendLine("  ${port.port}/${port.protocol}  ${port.service}  [${port.state}]")
                }
            } else {
                appendLine("  No open ports detected.")
            }
        }

        appendLine()
        appendLine("═══════════════════════════════════════")
        appendLine("  Generated by NetProbe Diagnostics")
        appendLine("═══════════════════════════════════════")
    }
}
