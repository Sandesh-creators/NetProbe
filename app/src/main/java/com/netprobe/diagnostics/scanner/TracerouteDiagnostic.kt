package com.netprobe.diagnostics.scanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.InetAddress

data class TracerouteHop(
    val ttl: Int,
    val ip: String?,
    val hostname: String?,
    val rttMs: Long
)

class TracerouteDiagnostic {

    /**
     * Runs a traceroute by sending ICMP pings with incrementing TTL values.
     * Each TTL value causes one more router hop to be revealed.
     * Works without root on most Android devices.
     */
    fun traceRoute(
        targetIp: String,
        maxHops: Int = 30,
        timeoutMs: Int = 3000
    ): Flow<TracerouteEvent> = flow {
        val hops = mutableListOf<TracerouteHop>()

        for (ttl in 1..maxHops) {
            val hop = executeHop(targetIp, ttl, timeoutMs)
            hops.add(hop)

            emit(TracerouteEvent.Hop(hop, hops.toList()))

            // If we reached the target, stop
            if (hop.ip == targetIp || hop.hostname == targetIp) {
                emit(TracerouteEvent.Complete(hops.toList()))
                return@flow
            }

            // Timeout with no response — try once more
            if (hop.ip == null) {
                val retry = executeHop(targetIp, ttl, timeoutMs)
                if (retry.ip != null) {
                    hops[hops.lastIndex] = retry
                    emit(TracerouteEvent.Hop(retry, hops.toList()))
                }
            }
        }

        emit(TracerouteEvent.Complete(hops.toList()))
    }.flowOn(Dispatchers.IO)

    private suspend fun executeHop(
        targetIp: String,
        ttl: Int,
        timeoutMs: Int
    ): TracerouteHop {
        return try {
            val start = System.nanoTime()
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "/system/bin/ping",
                    "-c", "1",
                    "-W", "${timeoutMs / 1000}",
                    "-t", "$ttl",
                    targetIp
                )
            )
            val exitCode = process.waitFor()
            val elapsed = (System.nanoTime() - start) / 1_000_000

            // Parse output for intermediate IP
            val output = process.inputStream.bufferedReader().readText()
            val hostname = extractHostname(output)
            val respondingIp = extractIp(output)

            // TTL exceeded = intermediate hop, 0 = reached target
            val isIntermediate = output.contains("Time to live exceeded") ||
                output.contains("ttl=") && respondingIp != targetIp

            if (exitCode == 0 || respondingIp != null) {
                val resolvedName = respondingIp?.let { ip ->
                    try { InetAddress.getByName(ip).canonicalHostName?.takeIf { it != ip } }
                    catch (_: Exception) { null }
                }

                TracerouteHop(
                    ttl = ttl,
                    ip = respondingIp ?: "*",
                    hostname = hostname ?: resolvedName,
                    rttMs = elapsed
                )
            } else {
                TracerouteHop(ttl = ttl, ip = null, hostname = null, rttMs = 0)
            }
        } catch (_: Exception) {
            TracerouteHop(ttl = ttl, ip = null, hostname = null, rttMs = 0)
        }
    }

    private fun extractHostname(output: String): String? {
        val match = Regex("from\\s+(\\S+)\\s+\\(").find(output)
        return match?.groupValues?.get(1)
    }

    private fun extractIp(output: String): String? {
        // Match patterns like "from 192.168.1.1" or "64 bytes from 192.168.1.1"
        val match = Regex("(?:from\\s+|reply from\\s+)(\\d+\\.\\d+\\.\\d+\\.\\d+)").find(output)
            ?: Regex("(\\d+\\.\\d+\\.\\d+\\.\\d+):").find(output)
        return match?.groupValues?.get(1)
    }
}

sealed class TracerouteEvent {
    data class Hop(val hop: TracerouteHop, val allHops: List<TracerouteHop>) : TracerouteEvent()
    data class Complete(val hops: List<TracerouteHop>) : TracerouteEvent()
}
