package com.netprobe.diagnostics.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netprobe.diagnostics.BuildConfig
import com.netprobe.diagnostics.ui.theme.*
import com.netprobe.diagnostics.viewmodel.*

enum class Tab(val label: String, val icon: ImageVector, val tag: String) {
    LAN("LAN Scan", Icons.Default.NetworkCheck, "lan"),
    BLUETOOTH("BT Explorer", Icons.Default.Bluetooth, "bt"),
    CHANNELS("Channel Map", Icons.Default.SignalCellularAlt, "ch"),
    DEVICE_INFO("Net Info", Icons.Default.SettingsEthernet, "net"),
    TOOLS("Tools", Icons.Default.Build, "tools")
}

enum class ToolItem(val label: String, val icon: ImageVector, val tag: String) {
    HEATMAP("Heatmap", Icons.Default.GridView, "heatmap"),
    ASSETS("Assets", Icons.Default.DevicesOther, "assets"),
    TOPOLOGY("Topology", Icons.Default.AccountTree, "topology")
}

@Composable
fun MainScreen(
    lanScanViewModel: LanScanViewModel = viewModel(),
    bluetoothViewModel: BluetoothViewModel = viewModel(),
    channelAnalyzerViewModel: ChannelAnalyzerViewModel = viewModel(),
    heatmapViewModel: HeatmapViewModel = viewModel(),
    assetDiscoveryViewModel: AssetDiscoveryViewModel = viewModel(),
    topologyViewModel: TopologyViewModel = viewModel(),
    pcapExportViewModel: PcapExportViewModel = viewModel()
) {
    var activeTab by remember { mutableStateOf(Tab.LAN) }
    var activeTool by remember { mutableStateOf(ToolItem.HEATMAP) }

    val scanState by lanScanViewModel.scanState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        TopStatusBar()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Tab.entries.forEach { tab ->
                val isActive = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isActive) TerminalGreen.copy(alpha = 0.12f)
                            else SurfaceDark
                        )
                        .border(
                            width = 1.dp,
                            color = if (isActive) TerminalGreen.copy(alpha = 0.4f) else SurfaceOverlay,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { activeTab = tab }
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(14.dp),
                            tint = if (isActive) TerminalGreen else TextDisabled
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) TerminalGreen else TextSecondary,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = SurfaceOverlay,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                fadeIn() + slideInHorizontally { it / 4 } togetherWith
                    fadeOut() + slideOutHorizontally { -it / 4 }
            },
            label = "tab_transition"
        ) { tab ->
            when (tab) {
                Tab.LAN -> LanScanScreen(lanScanViewModel)
                Tab.BLUETOOTH -> BluetoothExplorerScreen(bluetoothViewModel)
                Tab.CHANNELS -> ChannelAnalyzerScreen(channelAnalyzerViewModel)
                Tab.DEVICE_INFO -> DeviceInfoScreen()
                Tab.TOOLS -> ToolsScreen(
                    activeTool = activeTool,
                    onToolSelected = { activeTool = it },
                    heatmapViewModel = heatmapViewModel,
                    assetDiscoveryViewModel = assetDiscoveryViewModel,
                    topologyViewModel = topologyViewModel,
                    pcapExportViewModel = pcapExportViewModel,
                    scanState = scanState,
                    lanScanViewModel = lanScanViewModel
                )
            }
        }
    }
}

@Composable
private fun ToolsScreen(
    activeTool: ToolItem,
    onToolSelected: (ToolItem) -> Unit,
    heatmapViewModel: HeatmapViewModel,
    assetDiscoveryViewModel: AssetDiscoveryViewModel,
    topologyViewModel: TopologyViewModel,
    pcapExportViewModel: PcapExportViewModel,
    scanState: LanScanState,
    lanScanViewModel: LanScanViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolItem.entries.forEach { tool ->
                val isActive = activeTool == tool
                val color = when (tool) {
                    ToolItem.HEATMAP -> TerminalAmber
                    ToolItem.ASSETS -> TerminalCyan
                    ToolItem.TOPOLOGY -> TerminalGreen
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isActive) color.copy(alpha = 0.12f) else SurfaceCardDark)
                        .border(1.dp, if (isActive) color.copy(alpha = 0.4f) else SurfaceOverlay, RoundedCornerShape(4.dp))
                        .clickable { onToolSelected(tool) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            tool.icon,
                            contentDescription = tool.label,
                            modifier = Modifier.size(12.dp),
                            tint = if (isActive) color else TextDisabled
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            tool.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) color else TextSecondary,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = SurfaceOverlay, thickness = 1.dp)

        AnimatedContent(
            targetState = activeTool,
            transitionSpec = {
                fadeIn() + slideInVertically { it / 4 } togetherWith
                    fadeOut() + slideOutVertically { -it / 4 }
            },
            label = "tool_transition"
        ) { tool ->
            when (tool) {
                ToolItem.HEATMAP -> HeatmapScreen(heatmapViewModel)
                ToolItem.ASSETS -> AssetDiscoveryScreen(assetDiscoveryViewModel)
                ToolItem.TOPOLOGY -> {
                    val hosts = when (scanState) {
                        is LanScanState.Scanning -> scanState.aliveHosts
                        is LanScanState.Complete -> scanState.aliveHosts
                        else -> emptyList()
                    }
                    Column {
                        PcapExportSection(pcapExportViewModel)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (hosts.isNotEmpty()) {
                            TopologyMapScreen(
                                viewModel = topologyViewModel,
                                hostCount = hosts.size
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Run a LAN scan first to build topology",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextDisabled
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopStatusBar() {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
        while (true) {
            currentTime = sdf.format(java.util.Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCardDark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NETPROBE",
                style = MaterialTheme.typography.titleLarge,
                color = TerminalGreen,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.labelSmall,
                color = TextDisabled
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusIndicator("SYS", TerminalGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentTime,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StatusIndicator(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
