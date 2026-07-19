package com.netprobe.diagnostics.ui.screens

import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netprobe.diagnostics.ui.theme.*
import kotlinx.coroutines.delay
import java.net.Inet4Address
import java.net.NetworkInterface

@Composable
fun DeviceInfoScreen() {
    val context = LocalContext.current
    var refreshTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            refreshTick++
        }
    }

    val info = remember(refreshTick) { loadNetworkInfo(context) }

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
                text = "> NETWORK INFO",
                style = MaterialTheme.typography.titleMedium,
                color = TerminalAmber
            )
            Text(
                text = if (info.isConnected) "CONNECTED" else "DISCONNECTED",
                style = MaterialTheme.typography.labelMedium,
                color = if (info.isConnected) TerminalGreen else TerminalRed
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            item {
                InfoCard("INTERFACE", info.interfaceName, TerminalCyan)
            }
            item {
                InfoCard("IPv4 ADDRESS", info.ipv4Address, TerminalGreen)
            }
            item {
                InfoCard("MAC ADDRESS", info.macAddress, TerminalAmber)
            }
            item {
                InfoCard("GATEWAY", info.gateway, TerminalCyan)
            }
            item {
                InfoCard("SUBNET MASK", info.subnetMask, TerminalGreen)
            }

            if (info.dnsServers.isNotEmpty()) {
                item {
                    Text(
                        text = "> DNS SERVERS",
                        style = MaterialTheme.typography.labelMedium,
                        color = TerminalAmber,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(info.dnsServers.size) { idx ->
                    InfoCard(
                        label = "DNS ${idx + 1}",
                        value = info.dnsServers[idx],
                        valueColor = TerminalCyan
                    )
                }
            }

            item {
                InfoCard("DHCP SERVER", info.dhcpServer, TerminalGreen)
            }
            item {
                InfoCard("WI-FI SSID", info.wifiSsid, TerminalAmber)
            }
            item {
                InfoCard("WI-FI BSSID", info.wifiBssid, TerminalCyan)
            }
            item {
                InfoCard("LINK SPEED", info.linkSpeed, TerminalGreen)
            }
            item {
                InfoCard("SIGNAL RSSI", info.rssi, TerminalAmber)
            }
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardDark),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextDisabled
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                color = if (value == "N/A") TextDisabled else valueColor
            )
        }
    }
}

private data class NetworkInfo(
    val isConnected: Boolean = false,
    val interfaceName: String = "N/A",
    val ipv4Address: String = "N/A",
    val macAddress: String = "N/A",
    val gateway: String = "N/A",
    val subnetMask: String = "N/A",
    val dnsServers: List<String> = emptyList(),
    val dhcpServer: String = "N/A",
    val wifiSsid: String = "N/A",
    val wifiBssid: String = "N/A",
    val linkSpeed: String = "N/A",
    val rssi: String = "N/A"
)

private fun loadNetworkInfo(context: Context): NetworkInfo {
    return try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val linkProps = connectivityManager?.activeNetwork?.let { connectivityManager.getLinkProperties(it) }
        val wifiInfo = @Suppress("DEPRECATION") wifiManager?.connectionInfo
        val network = connectivityManager?.activeNetwork
        val isConnected = network != null

        val interfaceName = linkProps?.interfaceName
            ?: run {
                NetworkInterface.getNetworkInterfaces()?.asSequence()?.firstOrNull {
                    it.isUp && !it.isLoopback && it.name.startsWith("wlan")
                }?.name
            }
            ?: "N/A"

        val macAddress = NetworkInterface.getByName(interfaceName)?.hardwareAddress?.let { mac ->
            mac.joinToString(":") { String.format("%02X", it) }
        } ?: "N/A"

        val linkAddr = linkProps?.linkAddresses?.firstOrNull { addr ->
            addr.address is Inet4Address
        }
        val ipv4Address = linkAddr?.address?.hostAddress
            ?: @Suppress("DEPRECATION") wifiInfo?.ipAddress?.let { ip ->
                "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
            }
            ?: "N/A"

        val gateway = linkProps?.routes?.firstOrNull { route ->
            route.gateway != null
        }?.gateway?.hostAddress
            ?: @Suppress("DEPRECATION") wifiManager?.dhcpInfo?.gateway?.let { gw ->
                "${gw and 0xFF}.${gw shr 8 and 0xFF}.${gw shr 16 and 0xFF}.${gw shr 24 and 0xFF}"
            }
            ?: "N/A"

        val prefixLength = linkAddr?.prefixLength
        val subnetMask = prefixLength?.let { pl ->
            val mask = if (pl == 0) 0 else (0xFFFFFFFF.toInt() shl (32 - pl))
            "${mask shr 24 and 0xFF}.${mask shr 16 and 0xFF}.${mask shr 8 and 0xFF}.${mask and 0xFF} (/$pl)"
        } ?: @Suppress("DEPRECATION") wifiManager?.dhcpInfo?.netmask?.let { nm ->
            if (nm != 0) {
                "${nm and 0xFF}.${nm shr 8 and 0xFF}.${nm shr 16 and 0xFF}.${nm shr 24 and 0xFF}"
            } else "N/A"
        } ?: "N/A"

        val dnsServers = linkProps?.dnsServers?.mapNotNull { it.hostAddress }
            ?: @Suppress("DEPRECATION") run {
                val dhcp = wifiManager?.dhcpInfo
                listOfNotNull(
                    dhcp?.dns1?.takeIf { it != 0 }?.let { d ->
                        "${d and 0xFF}.${d shr 8 and 0xFF}.${d shr 16 and 0xFF}.${d shr 24 and 0xFF}"
                    },
                    dhcp?.dns2?.takeIf { it != 0 }?.let { d ->
                        "${d and 0xFF}.${d shr 8 and 0xFF}.${d shr 16 and 0xFF}.${d shr 24 and 0xFF}"
                    }
                )
            }

        val dhcpServer = @Suppress("DEPRECATION") wifiManager?.dhcpInfo?.serverAddress?.let { ip ->
            if (ip != 0) {
                "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
            } else "N/A"
        } ?: "N/A"

        val ssid = @Suppress("DEPRECATION") wifiInfo?.ssid?.let { s ->
            s.removeSurrounding("\"")
        } ?: "N/A"
        val wifiSsid = if (ssid == "<unknown ssid>" || ssid.isEmpty()) "N/A" else ssid

        val wifiBssid = @Suppress("DEPRECATION") wifiInfo?.bssid ?: "N/A"

        val linkSpeed = @Suppress("DEPRECATION") wifiInfo?.linkSpeed?.let { "$it Mbps" } ?: "N/A"

        val rssi = @Suppress("DEPRECATION") wifiInfo?.rssi?.let { "$it dBm" } ?: "N/A"

        NetworkInfo(
            isConnected = isConnected,
            interfaceName = interfaceName,
            ipv4Address = ipv4Address,
            macAddress = macAddress,
            gateway = gateway,
            subnetMask = subnetMask,
            dnsServers = dnsServers,
            dhcpServer = dhcpServer,
            wifiSsid = wifiSsid,
            wifiBssid = wifiBssid,
            linkSpeed = linkSpeed,
            rssi = rssi
        )
    } catch (e: Exception) {
        NetworkInfo()
    }
}
