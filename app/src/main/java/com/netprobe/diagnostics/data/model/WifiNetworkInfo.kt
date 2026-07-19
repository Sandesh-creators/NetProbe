package com.netprobe.diagnostics.data.model

data class WifiNetworkInfo(
    val ssid: String,
    val bssid: String,
    val frequency: Int,
    val channel: Int,
    val band: WifiBand,
    val rssi: Int,
    val capabilities: String
)

enum class WifiBand(val displayName: String) {
    BAND_2_4_GHZ("2.4 GHz"),
    BAND_5_GHZ("5 GHz"),
    BAND_6_GHZ("6 GHz"),
    UNKNOWN("Unknown")
}

data class ChannelOccupancy(
    val channel: Int,
    val band: WifiBand,
    val networkCount: Int,
    val networks: List<WifiNetworkInfo>,
    val congestionLevel: CongestionLevel
)

enum class CongestionLevel(val label: String) {
    CLEAR("Clear"),
    LOW("Low"),
    MODERATE("Moderate"),
    HIGH("High"),
    CRITICAL("Critical")
}
