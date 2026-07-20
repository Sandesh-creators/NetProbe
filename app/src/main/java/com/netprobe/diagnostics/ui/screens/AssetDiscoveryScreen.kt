package com.netprobe.diagnostics.ui.screens

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
import com.netprobe.diagnostics.data.db.entity.DiscoveredAssetEntity
import com.netprobe.diagnostics.ui.theme.*
import com.netprobe.diagnostics.viewmodel.AssetDiscoveryViewModel

@Composable
fun AssetDiscoveryScreen(viewModel: AssetDiscoveryViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val allAssets by viewModel.allAssets.collectAsState()
    var selectedAsset by remember { mutableStateOf<DiscoveredAssetEntity?>(null) }
    var filterMode by remember { mutableStateOf("ALL") }

    val filteredAssets = remember(allAssets, filterMode) {
        when (filterMode) {
            "UNKNOWN" -> allAssets.filter { !it.isKnown }
            "FLAGGED" -> allAssets.filter { it.isFlagged }
            else -> allAssets
        }
    }

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
                text = "> ASSET DISCOVERY",
                style = MaterialTheme.typography.titleMedium,
                color = TerminalAmber
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BadgeCounter("ALL", allAssets.size, TerminalGreen)
                BadgeCounter("UNK", allAssets.count { !it.isKnown }, TerminalAmber)
                BadgeCounter("FLAG", allAssets.count { it.isFlagged }, TerminalRed)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "UNKNOWN", "FLAGGED").forEach { mode ->
                val color = when (mode) {
                    "UNKNOWN" -> TerminalAmber
                    "FLAGGED" -> TerminalRed
                    else -> TerminalGreen
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (filterMode == mode) color.copy(alpha = 0.15f) else SurfaceCardDark)
                        .border(1.dp, if (filterMode == mode) color.copy(alpha = 0.4f) else SurfaceOverlay, RoundedCornerShape(4.dp))
                        .clickable { filterMode = mode }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = mode,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (filterMode == mode) color else TextSecondary
                    )
                }
            }
        }

        if (filteredAssets.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.DevicesOther,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = TextDisabled
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "NO ASSETS DISCOVERED",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap START DISCOVERY to begin\nmonitoring your network",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredAssets, key = { it.macAddress }) { asset ->
                    AssetRow(
                        asset = asset,
                        isSelected = selectedAsset?.macAddress == asset.macAddress,
                        onClick = {
                            selectedAsset = if (selectedAsset?.macAddress == asset.macAddress) null else asset
                        },
                        onToggleFlag = { viewModel.toggleFlag(asset.macAddress, !asset.isFlagged) },
                        onMarkKnown = { viewModel.markKnown(asset.macAddress) }
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.toggleDiscovery() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isRunning) TerminalRed else TerminalAmber,
                contentColor = SurfaceDark
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(
                imageVector = if (uiState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (uiState.isRunning) "STOP DISCOVERY" else "START DISCOVERY",
                style = MaterialTheme.typography.labelLarge,
                color = SurfaceDark
            )
        }
    }
}

@Composable
private fun BadgeCounter(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label:$count",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AssetRow(
    asset: DiscoveredAssetEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleFlag: () -> Unit,
    onMarkKnown: () -> Unit
) {
    val bgColor = when {
        asset.isFlagged -> TerminalRed.copy(alpha = 0.08f)
        !asset.isKnown -> TerminalAmber.copy(alpha = 0.08f)
        else -> SurfaceCardDark
    }
    val borderColor = when {
        asset.isFlagged -> TerminalRed.copy(alpha = 0.4f)
        !asset.isKnown -> TerminalAmber.copy(alpha = 0.4f)
        isSelected -> TerminalGreen.copy(alpha = 0.4f)
        else -> SurfaceOverlay
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
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
                Text(
                    text = asset.deviceName ?: asset.ouiVendor ?: asset.macAddress,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = asset.macAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDisabled
                )
                if (asset.ipAddress != null) {
                    Text(
                        text = "IP: ${asset.ipAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (asset.isFlagged) TerminalRed.copy(alpha = 0.2f) else SurfaceDark)
                        .clickable { onToggleFlag() }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (asset.isFlagged) Icons.Default.Flag else Icons.Default.OutlinedFlag,
                        contentDescription = "Flag",
                        modifier = Modifier.size(14.dp),
                        tint = if (asset.isFlagged) TerminalRed else TextDisabled
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (asset.isKnown) AssetKnown.copy(alpha = 0.2f) else SurfaceDark)
                        .clickable { onMarkKnown() }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (asset.isKnown) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                        contentDescription = "Known",
                        modifier = Modifier.size(14.dp),
                        tint = if (asset.isKnown) AssetKnown else TextDisabled
                    )
                }
            }
        }

        AnimatedVisibility(visible = isSelected) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                DetailLine("Source", asset.source)
                DetailLine("Type", asset.deviceType)
                DetailLine("First Seen", formatTime(asset.firstSeen))
                DetailLine("Last Seen", formatTime(asset.lastSeen))
                if (asset.ouiVendor != null) {
                    DetailLine("Vendor", asset.ouiVendor!!)
                }
                if (asset.notes != null) {
                    DetailLine("Notes", asset.notes!!)
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextDisabled)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}

private fun formatTime(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
    return sdf.format(java.util.Date(millis))
}
