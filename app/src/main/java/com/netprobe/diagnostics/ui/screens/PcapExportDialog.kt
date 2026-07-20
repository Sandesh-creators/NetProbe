package com.netprobe.diagnostics.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netprobe.diagnostics.pcap.ArpTableReader
import com.netprobe.diagnostics.pcap.DnsActivityReader
import com.netprobe.diagnostics.ui.theme.*
import com.netprobe.diagnostics.viewmodel.PcapExportState
import com.netprobe.diagnostics.viewmodel.PcapExportViewModel

@Composable
fun PcapExportSection(viewModel: PcapExportViewModel) {
    val state by viewModel.state.collectAsState()
    var showDetail by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val arpEntries = remember { ArpTableReader.read() }
    val dnsServers = remember { DnsActivityReader.read() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceCardDark)
            .border(1.dp, SurfaceOverlay, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "> PCAP EXPORT",
                    style = MaterialTheme.typography.titleSmall,
                    color = TerminalAmber
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ARP table + DNS config snapshot",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDisabled
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceDark)
                    .clickable { showDetail = !showDetail }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (showDetail) "HIDE" else "DETAIL",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalCyan
                )
            }
        }

        AnimatedVisibility(visible = showDetail) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "ARP Entries: ${arpEntries.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                arpEntries.take(5).forEach { entry ->
                    Text(
                        text = "  ${entry.ipAddress}  ${entry.macAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                }
                if (arpEntries.size > 5) {
                    Text(
                        text = "  ... and ${arpEntries.size - 5} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DNS Servers: ${dnsServers.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                dnsServers.forEach { server ->
                    Text(
                        text = "  ${server.address}${if (server.isDefault) " *" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.exportAndShare() },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (state) {
                    is PcapExportState.Exporting -> TerminalDim
                    is PcapExportState.Error -> TerminalRed
                    else -> TerminalAmber
                },
                contentColor = SurfaceDark
            ),
            shape = RoundedCornerShape(4.dp),
            enabled = state !is PcapExportState.Exporting
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (state) {
                    is PcapExportState.Exporting -> "EXPORTING..."
                    is PcapExportState.Success -> "EXPORTED"
                    is PcapExportState.Error -> "RETRY"
                    else -> "EXPORT .PCAP"
                },
                style = MaterialTheme.typography.labelLarge,
                color = SurfaceDark
            )
        }

        AnimatedVisibility(visible = state is PcapExportState.Error) {
            Text(
                text = (state as? PcapExportState.Error)?.message ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = TerminalRed,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
