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

    companion object {
        private val PING_PATHS = arrayOf("/system/bin/ping", "/vendor/bin/ping", "/system/xbin/ping")
    }

    fun traceRoute(
        targetIp: String,
        maxHops: Int = 30,
        timeoutMs: Int = 3000
    ): Flow<TracerouteEvent> = flow {
        val hops = mutableListOf<TracerouteHop>()
        val pingBinary = findPingBinary()

        for (ttl in 1..maxHops) {
            val hop = executeHop(pingBinary, targetIp, ttl, timeoutMs)
            hops.add(hop)

            emit(TracerouteEvent.Hop(hop, hops.toList()))

            if (hop.ip == targetIp || hop.hostname == targetIp) {
                emit(TracerouteEvent.Complete(hops.toList()))
                return@flow
            }

            if (hop.ip == null) {
                val retry = executeHop(pingBinary, targetIp, ttl, timeoutMs)
                if (retry.ip != null) {
                    hops[hops.lastIndex] = retry
                    emit(TracerouteEvent.Hop(retry, hops.toList()))
                }
            }
        }

        emit(TracerouteEvent.Complete(hops.toList()))
    }.flowOn(Dispatchers.IO)

    private fun findPingBinary(): String {
        for (path in PING_PATHS) {
            if (java.io.File(path).exists()) return path
        }
        return "/system/bin/ping"
    }

    private suspend fun executeHop(
        pingBinary: String,
        targetIp: String,
        ttl: Int,
        timeoutMs: Int
    ): TracerouteHop {
        return try {
            val start = System.nanoTime()
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    pingBinary,
                    "-c", "1",
                    "-W", "${timeoutMs / 1000}",
                    "-m", "$ttl",
                    targetIp
                )
            )
            val exitCode = process.waitFor()
            val elapsed = (System.nanoTime() - start) / 1_000_000

            val output = process.inputStream.bufferedReader().readText()
            val hostname = extractHostname(output)
            val respondingIp = extractIp(output)

            val isIntermediate = output.contains("Time to live exceeded") ||
                (output.contains("ttl=") && respondingIp != targetIp)

            if (exitCode == 0 || respondingIp != null) {
                val resolvedName = respondingIp?.let { ip ->
                    try {
                        InetAddress.getByName(ip).canonicalHostName?.takeIf { it != ip }
                    } catch (_: Exception) { null }
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
        val match = Regex("(?:from\\s+|reply from\\s+)(\\d+\\.\\d+\\.\\d+\\.\\d+)").find(output)
            ?: Regex("(\\d+\\.\\d+\\.\\d+\\.\\d+):").find(output)
        return match?.groupValues?.get(1)
    }
}

sealed class TracerouteEvent {
    data class Hop(val hop: TracerouteHop, val allHops: List<TracerouteHop>) : TracerouteEvent()
    data class Complete(val hops: List<TracerouteHop>) : TracerouteEvent()
}
