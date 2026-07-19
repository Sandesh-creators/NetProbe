package com.netprobe.diagnostics.scanner

import android.content.Context
import com.netprobe.diagnostics.data.model.HostInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress
import java.net.NetworkInterface

class LanScanner(private val context: Context) {

    data class SubnetInfo(
        val subnetPrefix: String,
        val gatewayIp: String,
        val deviceIp: String,
        val cidr: Int
    )

    fun getSubnetInfo(): SubnetInfo? {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager

        @Suppress("DEPRECATION")
        val dhcpInfo = wifiManager.dhcpInfo ?: return null
        val deviceIp = dhcpInfo.ipAddress

        val ipString = intToIp(deviceIp)
        val gatewayString = intToIp(dhcpInfo.gateway)
        val subnetPrefix = ipString.substringBeforeLast(".")

        return SubnetInfo(
            subnetPrefix = subnetPrefix,
            gatewayIp = gatewayString,
            deviceIp = ipString,
            cidr = 24
        )
    }

    suspend fun pingHost(ip: String, timeoutMs: Int = 2000): HostInfo = coroutineScope {
        try {
            val start = System.currentTimeMillis()
            val process = Runtime.getRuntime().exec(
                arrayOf("/system/bin/ping", "-c", "1", "-W", "${timeoutMs / 1000}", ip)
            )
            val completed = process.waitFor()
            val elapsed = System.currentTimeMillis() - start

            val hostname = try {
                val addr = InetAddress.getByName(ip)
                addr.canonicalHostName?.takeIf { it != ip }
            } catch (_: Exception) { null }

            val mac = getMacFromArpTable(ip)

            HostInfo(
                ip = ip,
                hostname = hostname,
                macAddress = mac,
                isAlive = completed == 0 && elapsed < timeoutMs,
                latencyMs = if (completed == 0) elapsed else 0L
            )
        } catch (e: Exception) {
            HostInfo(ip = ip, hostname = null, macAddress = null, isAlive = false)
        }
    }

    fun scanSubnet(): Flow<ScanProgress> = flow {
        val subnet = getSubnetInfo() ?: run {
            emit(ScanProgress.Error("Could not determine local subnet. Is Wi-Fi connected?"))
            return@flow
        }

        emit(ScanProgress.Scanning(
            totalHosts = 254,
            scannedHosts = 0,
            aliveHosts = emptyList(),
            subnetInfo = subnet
        ))

        val aliveHosts = mutableListOf<HostInfo>()
        val batchSize = 30

        for (batchStart in 1..254 step batchSize) {
            val batchEnd = minOf(batchStart + batchSize - 1, 254)
            val batch = coroutineScope {
                (batchStart..batchEnd).map { i ->
                    async(Dispatchers.IO) {
                        val ip = "${subnet.subnetPrefix}.$i"
                        pingHost(ip, timeoutMs = 1500)
                    }
                }
            }

            val results = batch.awaitAll()
            val alive = results.filter { it.isAlive }
            aliveHosts.addAll(alive)

            emit(ScanProgress.Scanning(
                totalHosts = 254,
                scannedHosts = batchEnd,
                aliveHosts = aliveHosts.sortedBy { ipToInt(it.ip) },
                subnetInfo = subnet
            ))

            delay(100)
        }

        emit(ScanProgress.Complete(
            aliveHosts = aliveHosts.sortedBy { ipToInt(it.ip) },
            subnetInfo = subnet
        ))
    }.flowOn(Dispatchers.Default)

    private fun getMacFromArpTable(ip: String): String? {
        return try {
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                reader.readLines().drop(1).forEach { line ->
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 4 && parts[0] == ip) {
                        val mac = parts[3]
                        if (mac != "00:00:00:00:00:00" && mac.contains(":")) {
                            return mac.uppercase()
                        }
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun intToIp(ipInt: Int): String {
            return "${ipInt and 0xFF}.${(ipInt shr 8) and 0xFF}.${(ipInt shr 16) and 0xFF}.${(ipInt shr 24) and 0xFF}"
        }

        fun ipToInt(ip: String): Int {
            val parts = ip.split(".")
            return parts[0].toInt() or (parts[1].toInt() shl 8) or
                (parts[2].toInt() shl 16) or (parts[3].toInt() shl 24)
        }
    }
}

sealed class ScanProgress {
    data class Scanning(
        val totalHosts: Int,
        val scannedHosts: Int,
        val aliveHosts: List<HostInfo>,
        val subnetInfo: LanScanner.SubnetInfo
    ) : ScanProgress()

    data class Complete(
        val aliveHosts: List<HostInfo>,
        val subnetInfo: LanScanner.SubnetInfo
    ) : ScanProgress()

    data class Error(val message: String) : ScanProgress()
}
