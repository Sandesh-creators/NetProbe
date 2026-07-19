package com.netprobe.diagnostics.scanner

import android.content.Context
import android.net.wifi.WifiManager
import com.netprobe.diagnostics.data.model.CongestionLevel
import com.netprobe.diagnostics.data.model.ChannelOccupancy
import com.netprobe.diagnostics.data.model.WifiBand
import com.netprobe.diagnostics.data.model.WifiNetworkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class WifiScanner(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun frequencyToChannel(frequencyMhz: Int): Pair<Int, WifiBand> {
        return when {
            frequencyMhz in 2412..2484 -> {
                val channel = when (frequencyMhz) {
                    2484 -> 14
                    else -> (frequencyMhz - 2412) / 5 + 1
                }
                channel to WifiBand.BAND_2_4_GHZ
            }
            frequencyMhz in 4915..5825 -> {
                val channel = (frequencyMhz - 5180) / 20 + 36
                channel to WifiBand.BAND_5_GHZ
            }
            frequencyMhz in 5955..7115 -> {
                val channel = (frequencyMhz - 5955) / 5 + 1
                channel to WifiBand.BAND_6_GHZ
            }
            else -> 0 to WifiBand.UNKNOWN
        }
    }

    fun getChannelLabel(frequencyMhz: Int): String {
        val (channel, band) = frequencyToChannel(frequencyMhz)
        return "Ch $channel (${frequencyMhz} MHz \u00B7 ${band.displayName})"
    }

    fun scanWifiNetworks(): Flow<List<WifiNetworkInfo>> = flow {
        while (true) {
            @Suppress("MissingPermission")
            wifiManager.startScan()
            delay(3500)

            @Suppress("MissingPermission")
            val results = wifiManager.scanResults?.mapNotNull { scanResult ->
                scanResultToNetworkInfo(scanResult)
            }?.distinctBy { it.bssid }?.sortedByDescending { it.rssi } ?: emptyList()

            emit(results)
            delay(15_000)
        }
    }.flowOn(Dispatchers.IO)

    @Suppress("MissingPermission")
    suspend fun singleScan(): List<WifiNetworkInfo> = withContext(Dispatchers.IO) {
        wifiManager.startScan()
        delay(3000)
        wifiManager.scanResults?.mapNotNull { scanResultToNetworkInfo(it) }
            ?.distinctBy { it.bssid }
            ?.sortedByDescending { it.rssi }
            ?: emptyList()
    }

    private fun scanResultToNetworkInfo(sr: android.net.wifi.ScanResult): WifiNetworkInfo? {
        val ssid = sr.SSID?.takeIf { it.isNotEmpty() } ?: return null
        val (channel, band) = frequencyToChannel(sr.frequency)

        return WifiNetworkInfo(
            ssid = ssid,
            bssid = sr.BSSID ?: "Unknown",
            frequency = sr.frequency,
            channel = channel,
            band = band,
            rssi = sr.level,
            capabilities = sr.capabilities ?: ""
        )
    }

    fun computeChannelOccupancy(networks: List<WifiNetworkInfo>): List<ChannelOccupancy> {
        val grouped = networks.groupBy { it.channel }

        return grouped.map { (channel, channelNetworks) ->
            val band = channelNetworks.first().band
            val count = channelNetworks.size

            val overlappingCount = if (band == WifiBand.BAND_2_4_GHZ) {
                networks.count { other ->
                    other.band == WifiBand.BAND_2_4_GHZ &&
                        kotlin.math.abs(other.channel - channel) <= 2
                }
            } else {
                count
            }

            val congestion = when {
                overlappingCount >= 8 -> CongestionLevel.CRITICAL
                overlappingCount >= 5 -> CongestionLevel.HIGH
                overlappingCount >= 3 -> CongestionLevel.MODERATE
                overlappingCount >= 1 -> CongestionLevel.LOW
                else -> CongestionLevel.CLEAR
            }

            ChannelOccupancy(
                channel = channel,
                band = band,
                networkCount = count,
                networks = channelNetworks,
                congestionLevel = congestion
            )
        }.sortedBy { it.channel }
    }

    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled
}
