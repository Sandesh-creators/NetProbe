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
import com.netprobe.diagnostics.ui.theme.*
import com.netprobe.diagnostics.viewmodel.BluetoothViewModel
import com.netprobe.diagnostics.viewmodel.ChannelAnalyzerViewModel
import com.netprobe.diagnostics.viewmodel.LanScanViewModel

enum class Tab(val label: String, val icon: ImageVector, val tag: String) {
    LAN("LAN Scan", Icons.Default.NetworkCheck, "lan"),
    BLUETOOTH("BT Explorer", Icons.Default.Bluetooth, "bt"),
    CHANNELS("Channel Map", Icons.Default.SignalCellularAlt, "ch")
}

@Composable
fun MainScreen(
    lanScanViewModel: LanScanViewModel = viewModel(),
    bluetoothViewModel: BluetoothViewModel = viewModel(),
    channelAnalyzerViewModel: ChannelAnalyzerViewModel = viewModel()
) {
    var activeTab by remember { mutableStateOf(Tab.LAN) }

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
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(14.dp),
                            tint = if (isActive) TerminalGreen else TextDisabled
                        )
                        Spacer(modifier = Modifier.width(4.dp))
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
            }
        }
    }
}

@Composable
private fun TopStatusBar() {
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
                text = "v1.1",
                style = MaterialTheme.typography.labelSmall,
                color = TextDisabled
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusIndicator("SYS", TerminalGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date()),
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
