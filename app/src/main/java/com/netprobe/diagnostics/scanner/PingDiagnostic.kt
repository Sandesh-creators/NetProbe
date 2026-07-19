package com.netprobe.diagnostics.scanner

import com.netprobe.diagnostics.data.model.PingResult
import com.netprobe.diagnostics.data.model.PingStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class PingDiagnostic {

    companion object {
        private const val MAX_RTTS = 100
        private val RTT_PATTERN = Regex("time=(\\d+\\.?\\d*)")
        private val PING_PATHS = arrayOf("/system/bin/ping", "/vendor/bin/ping", "/system/xbin/ping")
    }

    fun continuousPing(
        targetIp: String,
        intervalMs: Long = 1000,
        timeoutMs: Int = 2000
    ): Flow<PingEvent> = flow {
        var sequence = 0
        val rtts = mutableListOf<Long>()
        val pingBinary = findPingBinary()

        while (true) {
            sequence++
            val result = executePing(pingBinary, targetIp, sequence, timeoutMs)
            rtts.add(result.rttMs)
            if (rtts.size > MAX_RTTS) rtts.removeAt(0)

            val stats = computeStats(targetIp, rtts, sequence)

            emit(PingEvent.Result(result))
            emit(PingEvent.Stats(stats))

            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    private fun findPingBinary(): String {
        for (path in PING_PATHS) {
            if (java.io.File(path).exists()) return path
        }
        return "/system/bin/ping"
    }

    private fun executePing(
        pingBinary: String,
        targetIp: String,
        sequence: Int,
        timeoutMs: Int
    ): PingResult {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    pingBinary,
                    "-c", "1",
                    "-W", "${timeoutMs / 1000}",
                    targetIp
                )
            )
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText()

            val rttMs = parseRttFromOutput(output)

            PingResult(
                sequenceNumber = sequence,
                rttMs = rttMs,
                isAlive = exitCode == 0
            )
        } catch (e: Exception) {
            PingResult(
                sequenceNumber = sequence,
                rttMs = 0L,
                isAlive = false
            )
        }
    }

    private fun parseRttFromOutput(output: String): Long {
        val match = RTT_PATTERN.find(output) ?: return 0L
        return match.groupValues[1].toDoubleOrNull()?.toLong() ?: 0L
    }

    private fun computeStats(host: String, rtts: List<Long>, sequence: Int): PingStats {
        val received = rtts.filter { it > 0 }
        val total = sequence
        val lost = total - received.size

        val minRtt = received.minOrNull() ?: 0L
        val maxRtt = received.maxOrNull() ?: 0L
        val avgRtt = if (received.isNotEmpty()) received.sum() / received.size else 0L

        val jitter = if (received.size >= 2) {
            val diffs = received.zipWithNext().map { (a, b) -> kotlin.math.abs(a - b) }
            diffs.sum() / diffs.size
        } else 0L

        return PingStats(
            host = host,
            totalPackets = total,
            receivedPackets = received.size,
            lostPackets = lost,
            minRtt = minRtt,
            maxRtt = maxRtt,
            avgRtt = avgRtt,
            jitter = jitter,
            packetLossPercent = if (total > 0) (lost.toFloat() / total) * 100f else 0f
        )
    }
}

sealed class PingEvent {
    data class Result(val ping: PingResult) : PingEvent()
    data class Stats(val stats: PingStats) : PingEvent()
}
