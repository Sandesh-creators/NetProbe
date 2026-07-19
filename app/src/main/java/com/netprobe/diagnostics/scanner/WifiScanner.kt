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

    fun isLocationEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    fun isWifiEnabled(): Boolean {
        return try {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled
        } catch (_: Exception) {
            false
        }
    }

    fun getDiagnostics(): ScanDiagnostics {
        val issues = mutableListOf<String>()
        val isWifi = isWifiEnabled()
        val isLoc = isLocationEnabled()

        if (!isWifi) issues.add("Wi-Fi is OFF")
        if (!isLoc) issues.add("Location services are OFF")

        return ScanDiagnostics(
            isWifiEnabled = isWifi,
            isLocationEnabled = isLoc,
            issues = issues
        )
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

    /**
     * Emits scan results with full diagnostic info so the caller can
     * distinguish between "scan ran but found nothing" vs "scan failed".
     */
    fun scanWifiNetworks(): Flow<WifiScanResult> = flow {
        var consecutiveEmpty = 0

        while (true) {
            val scanEvent = doSingleScan()
            emit(scanEvent)

            if (scanEvent.networks.isEmpty()) {
                consecutiveEmpty++
            } else {
                consecutiveEmpty = 0
            }

            // Throttle delay: shorter if scan is working, longer if repeatedly empty
            val delayMs = if (consecutiveEmpty >= 3) 15_000L else 4_000L
            delay(delayMs)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun doSingleScan(): WifiScanResult {
        // Pre-check
        val diag = getDiagnostics()
        if (!diag.isOk()) {
            return WifiScanResult(
                networks = emptyList(),
                rawCount = 0,
                filteredCount = 0,
                error = "Pre-check failed: ${diag.issues.joinToString("; ")}"
            )
        }

        // Trigger scan
        var scanStarted = false
        try {
            @Suppress("MissingPermission")
            scanStarted = wifiManager.startScan()
        } catch (e: SecurityException) {
            return WifiScanResult(
                networks = emptyList(), rawCount = 0, filteredCount = 0,
                error = "Missing permission: ${e.message}"
            )
        } catch (e: Exception) {
            return WifiScanResult(
                networks = emptyList(), rawCount = 0, filteredCount = 0,
                error = "startScan() exception: ${e.message}"
            )
        }

        if (!scanStarted) {
            return WifiScanResult(
                networks = emptyList(), rawCount = 0, filteredCount = 0,
                error = "Scan throttled by Android (startScan returned false)"
            )
        }

        // Wait for results
        delay(4000)

        // Read results
        return try {
            @Suppress("MissingPermission")
            val rawResults = wifiManager.scanResults
            if (rawResults == null) {
                return WifiScanResult(
                    networks = emptyList(), rawCount = 0, filteredCount = 0,
                    error = "scanResults is null"
                )
            }

            val rawCount = rawResults.size
            val mapped = rawResults.mapNotNull { sr -> scanResultToNetworkInfo(sr) }
            val filtered = mapped.distinctBy { it.bssid }.sortedByDescending { it.rssi }

            val error = if (rawCount > 0 && filtered.isEmpty()) {
                "Scan returned $rawCount results but all had 0 frequency (Location Services may be needed for channel data)"
            } else if (rawCount == 0) {
                "Scan returned 0 APs — no networks visible"
            } else {
                null
            }

            WifiScanResult(
                networks = filtered,
                rawCount = rawCount,
                filteredCount = filtered.size,
                error = error
            )
        } catch (e: SecurityException) {
            WifiScanResult(
                networks = emptyList(), rawCount = 0, filteredCount = 0,
                error = "Permission denied reading scan results: ${e.message}"
            )
        } catch (e: Exception) {
            WifiScanResult(
                networks = emptyList(), rawCount = 0, filteredCount = 0,
                error = "Exception reading scan results: ${e.message}"
            )
        }
    }

    private fun scanResultToNetworkInfo(sr: android.net.wifi.ScanResult): WifiNetworkInfo? {
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
}

data class ScanDiagnostics(
    val isWifiEnabled: Boolean,
    val isLocationEnabled: Boolean,
    val issues: List<String>
) {
    fun isOk() = issues.isEmpty()
}

data class WifiScanResult(
    val networks: List<WifiNetworkInfo>,
    val rawCount: Int,
    val filteredCount: Int,
    val error: String? = null
)
