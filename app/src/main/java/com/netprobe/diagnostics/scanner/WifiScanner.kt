package com.netprobe.diagnostics.scanner

import android.content.Context
import android.location.LocationManager
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

class WifiScanner(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * On Android 10+, Wi-Fi scan results only contain SSID and frequency
     * when Location Services (the device GPS toggle) are enabled.
     * This is separate from the ACCESS_FINE_LOCATION permission.
     */
    fun isLocationEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    fun getDiagnostics(): String {
        val issues = mutableListOf<String>()
        if (!isWifiEnabled()) issues.add("Wi-Fi disabled")
        if (!isLocationEnabled()) issues.add("Location services disabled")
        if (issues.isEmpty()) return "OK"
        return issues.joinToString(", ")
    }

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
            try {
                @Suppress("MissingPermission")
                val scanStarted = wifiManager.startScan()
                if (!scanStarted) {
                    emit(emptyList())
                    delay(15_000)
                    continue
                }
            } catch (_: SecurityException) {
                emit(emptyList())
                delay(5_000)
                continue
            } catch (_: Exception) {
                delay(5_000)
                continue
            }

            delay(4000)

            try {
                @Suppress("MissingPermission")
                val results = wifiManager.scanResults?.mapNotNull { scanResult ->
                    scanResultToNetworkInfo(scanResult)
                }?.distinctBy { it.bssid }?.sortedByDescending { it.rssi } ?: emptyList()

                emit(results)
            } catch (_: SecurityException) {
                emit(emptyList())
            } catch (_: Exception) {
                emit(emptyList())
            }

            delay(15_000)
        }
    }.flowOn(Dispatchers.IO)

    @Suppress("MissingPermission")
    suspend fun singleScan(): List<WifiNetworkInfo> {
        return try {
            wifiManager.startScan()
            delay(4000)
            wifiManager.scanResults?.mapNotNull { scanResultToNetworkInfo(it) }
                ?.distinctBy { it.bssid }
                ?.sortedByDescending { it.rssi }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun scanResultToNetworkInfo(sr: android.net.wifi.ScanResult): WifiNetworkInfo? {
        // Use SSID if available, otherwise fall back to BSSID so we still
        // capture networks even when location is off (SSID will be "<unknown ssid>")
        val ssid = sr.SSID?.takeIf { it.isNotEmpty() && it != "<unknown ssid>" }
            ?: sr.BSSID?.takeIf { it.isNotEmpty() }
            ?: return null

        val frequency = sr.frequency
        if (frequency <= 0) return null

        val (channel, band) = frequencyToChannel(frequency)

        return WifiNetworkInfo(
            ssid = ssid,
            bssid = sr.BSSID ?: "Unknown",
            frequency = frequency,
            channel = channel,
            band = band,
            rssi = sr.level,
            capabilities = sr.capabilities ?: ""
        )
    }

    fun computeChannelOccupancy(networks: List<WifiNetworkInfo>): List<ChannelOccupancy> {
        if (networks.isEmpty()) return emptyList()

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

    fun isWifiEnabled(): Boolean {
        return try {
            wifiManager.isWifiEnabled
        } catch (_: Exception) {
            false
        }
    }
}
