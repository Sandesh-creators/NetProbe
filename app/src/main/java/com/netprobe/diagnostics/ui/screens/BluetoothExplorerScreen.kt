package com.netprobe.diagnostics.ui.screens

import android.bluetooth.BluetoothAdapter
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.netprobe.diagnostics.data.model.BluetoothDeviceInfo
import com.netprobe.diagnostics.data.model.DeviceType
import com.netprobe.diagnostics.ui.theme.*
import com.netprobe.diagnostics.viewmodel.BluetoothState
import com.netprobe.diagnostics.viewmodel.BluetoothViewModel

@Composable
fun BluetoothExplorerScreen(viewModel: BluetoothViewModel) {
    val btState by viewModel.btState.collectAsState()
    val selectedDevice by viewModel.selectedDevice.collectAsState()

    var sortCriterion by remember { mutableStateOf("RSSI") }
    var typeFilter by remember { mutableStateOf("ALL") }

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
                text = "> BT RECON",
                style = MaterialTheme.typography.titleMedium,
                color = TerminalCyan
            )
            val deviceCount = when (btState) {
                is BluetoothState.Scanning -> (btState as BluetoothState.Scanning).devices.size
                is BluetoothState.Paused -> (btState as BluetoothState.Paused).devices.size
                else -> 0
            }
            Text(
                text = "$deviceCount DEVICES",
                style = MaterialTheme.typography.labelMedium,
                color = TerminalAmber
            )
        }

        // ── Bluetooth Off Warning + Enable Button ─────────────
        if (btState is BluetoothState.BluetoothOff) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(TerminalRed.copy(alpha = 0.1f))
                    .border(1.dp, TerminalRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .clickable { BluetoothAdapter.getDefaultAdapter()?.enable() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BluetoothDisabled, contentDescription = null, tint = TerminalRed)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("BLUETOOTH IS DISABLED", style = MaterialTheme.typography.bodyMedium, color = TerminalRed)
                    Text("Tap to enable Bluetooth", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TerminalRed.copy(alpha = 0.2f))
                            .border(1.dp, TerminalRed.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ENABLE BLUETOOTH",
                            style = MaterialTheme.typography.labelMedium,
                            color = TerminalRed
                        )
                    }
                }
            }
        }

        // ── Device Type Legend ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LegendDot("CLASSIC", BtClassicColor)
            LegendDot("DUAL", BtDualColor)
            LegendDot("BLE", BtLeColor)
            LegendDot("UNK", BtUnknownColor)
        }

        // ── Filter / Sort Row ─────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = typeFilter == "ALL",
                onClick = { typeFilter = "ALL" },
                label = { Text("ALL", style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TerminalCyan.copy(alpha = 0.15f),
                    selectedLabelColor = TerminalCyan
                )
            )
            FilterChip(
                selected = typeFilter == "CLASSIC",
                onClick = { typeFilter = "CLASSIC" },
                label = { Text("CLASSIC", style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BtClassicColor.copy(alpha = 0.15f),
                    selectedLabelColor = BtClassicColor
                )
            )
            FilterChip(
                selected = typeFilter == "BLE",
                onClick = { typeFilter = "BLE" },
                label = { Text("BLE", style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BtLeColor.copy(alpha = 0.15f),
                    selectedLabelColor = BtLeColor
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceCardDark)
                    .border(1.dp, SurfaceOverlay, RoundedCornerShape(4.dp))
                    .clickable {
                        sortCriterion = if (sortCriterion == "RSSI") "NAME" else "RSSI"
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (sortCriterion == "RSSI") "SORT: RSSI" else "SORT: NAME",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalAmber
                )
            }
        }

        // ── Device List ───────────────────────────────────────
        val devices = when (btState) {
            is BluetoothState.Scanning -> (btState as BluetoothState.Scanning).devices
            is BluetoothState.Paused -> (btState as BluetoothState.Paused).devices
            else -> emptyList()
        }

        val filteredAndSorted = remember(devices, typeFilter, sortCriterion) {
            val filtered = when (typeFilter) {
                "CLASSIC" -> devices.filter { it.type == DeviceType.CLASSIC || it.type == DeviceType.DUAL }
                "BLE" -> devices.filter { it.type == DeviceType.LE }
                else -> devices
            }
            when (sortCriterion) {
                "NAME" -> filtered.sortedBy { it.name?.lowercase() ?: "" }
                else -> filtered.sortedByDescending { it.rssi }
            }
        }

        if (filteredAndSorted.isEmpty() && btState !is BluetoothState.BluetoothOff) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.BluetoothSearching,
                        contentDescription = null,
                        tint = TextDisabled,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tap START BT SCAN to discover nearby devices",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredAndSorted, key = { it.address }) { device ->
                    DeviceRow(
                        device = device,
                        isSelected = selectedDevice?.address == device.address,
                        onClick = {
                            if (selectedDevice?.address == device.address) viewModel.clearSelection()
                            else viewModel.selectDevice(device)
                        }
                    )
                }
            }
        }

        // ── Selected Device Detail Panel ──────────────────────
        AnimatedVisibility(
            visible = selectedDevice != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedDevice?.let { device ->
                DeviceDetailPanel(
                    device = device,
                    onMonitorBattery = { viewModel.startBatteryMonitor(device) },
                    onStopMonitor = { viewModel.stopBatteryMonitor() }
                )
            }
        }

        // ── Scan Button ───────────────────────────────────────
        Button(
            onClick = {
                when (btState) {
                    is BluetoothState.Scanning -> viewModel.stopScan()
                    else -> viewModel.startScan()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (btState) {
                    is BluetoothState.Scanning -> TerminalRed
                    else -> TerminalCyan
                },
                contentColor = SurfaceDark
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Icon(
                imageVector = when (btState) {
                    is BluetoothState.Scanning -> Icons.Default.Stop
                    else -> Icons.Default.Bluetooth
                },
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (btState) {
                    is BluetoothState.Scanning -> "STOP SCAN"
                    else -> "START BT SCAN"
                },
                style = MaterialTheme.typography.labelLarge,
                color = SurfaceDark
            )
        }
    }
}

@Composable
private fun DeviceRow(
    device: BluetoothDeviceInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val typeColor = when (device.type) {
        DeviceType.CLASSIC -> BtClassicColor
        DeviceType.DUAL -> BtDualColor
        DeviceType.LE -> BtLeColor
        DeviceType.UNKNOWN -> BtUnknownColor
    }

    val rssiColor = when {
        device.rssi == 0 -> TextDisabled
        device.rssi > -50 -> SignalStrong
        device.rssi > -70 -> SignalMedium
        device.rssi > -85 -> SignalWeak
        else -> SignalDead
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) SurfaceElevated else SurfaceCardDark)
            .border(
                width = 1.dp,
                color = if (isSelected) TerminalCyan.copy(alpha = 0.4f) else SurfaceOverlay,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(typeColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                    Text(
                        text = device.type.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = typeColor
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (device.rssi != 0) {
                Text(
                    text = "${device.rssi}dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = rssiColor,
                    fontWeight = FontWeight.Bold
                )
            }
            SignalBars(rssi = device.rssi, color = rssiColor)
        }
    }
}

@Composable
private fun SignalBars(rssi: Int, color: androidx.compose.ui.graphics.Color) {
    val bars = when {
        rssi == 0 -> 0
        rssi > -50 -> 4
        rssi > -65 -> 3
        rssi > -80 -> 2
        else -> 1
    }
    Row(
        modifier = Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((4 + i * 3).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i <= bars) color else SurfaceOverlay)
            )
        }
    }
}

@Composable
private fun DeviceDetailPanel(
    device: BluetoothDeviceInfo,
    onMonitorBattery: () -> Unit,
    onStopMonitor: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceCardDark)
            .padding(12.dp)
    ) {
        Text(
            text = "> DEVICE DETAIL",
            style = MaterialTheme.typography.labelLarge,
            color = TerminalCyan
        )
        Spacer(modifier = Modifier.height(8.dp))

        DetailRow("Name", device.name ?: "N/A")
        DetailRow("MAC", device.address)
        DetailRow("Type", device.type.displayName)
        DetailRow("RSSI", "${device.rssi} dBm")
        DetailRow("Bond", bondStateLabel(device.bondState))

        if (device.batteryLevel != null && device.batteryLevel > 0) {
            DetailRow("Battery", "${device.batteryLevel}%")
        }

        if (device.uuids.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("UUIDs:", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
            device.uuids.forEach { uuid ->
                Text(
                    text = "  $uuid",
                    style = MaterialTheme.typography.bodySmall,
                    color = TerminalAmber
                )
            }
        }

        if (device.txPower != null) {
            DetailRow("TX Power", "${device.txPower} dBm")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(TerminalCyan.copy(alpha = 0.1f))
                    .border(1.dp, TerminalCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .clickable { onMonitorBattery() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("MONITOR BATTERY", style = MaterialTheme.typography.labelSmall, color = TerminalCyan)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(TerminalRed.copy(alpha = 0.1f))
                    .border(1.dp, TerminalRed.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .clickable { onStopMonitor() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("STOP", style = MaterialTheme.typography.labelSmall, color = TerminalRed)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextDisabled)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

private fun bondStateLabel(state: Int): String = when (state) {
    android.bluetooth.BluetoothDevice.BOND_NONE -> "None"
    android.bluetooth.BluetoothDevice.BOND_BONDING -> "Bonding"
    android.bluetooth.BluetoothDevice.BOND_BONDED -> "Bonded"
    else -> "Unknown"
}
