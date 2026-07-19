package com.netprobe.diagnostics.scanner

import com.netprobe.diagnostics.data.model.PingResult
import com.netprobe.diagnostics.data.model.PingStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class PingDiagnostic {

    /**
     * Runs continuous ICMP ping against a target, emitting each result
     * and updated statistics. Runs until cancelled.
     */
    fun continuousPing(
        targetIp: String,
        intervalMs: Long = 1000,
        timeoutMs: Int = 2000
    ): Flow<PingEvent> = flow {
        var sequence = 0
        val rtts = mutableListOf<Long>()

        while (true) {
            sequence++
            val result = executePing(targetIp, sequence, timeoutMs)
            rtts.add(result.rttMs)

            val stats = computeStats(targetIp, rtts, sequence)

            emit(PingEvent.Result(result))
            emit(PingEvent.Stats(stats))

            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun executePing(
        targetIp: String,
        sequence: Int,
        timeoutMs: Int
    ): PingResult {
        return try {
            val start = System.nanoTime()
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "/system/bin/ping",
                    "-c", "1",
                    "-W", "${timeoutMs / 1000}",
                    "-i", "0.2",
                    targetIp
                )
            )
            val exitCode = process.waitFor()
            val elapsedNs = System.nanoTime() - start
            val rttMs = elapsedNs / 1_000_000

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

    private fun computeStats(host: String, rtts: List<Long>, sequence: Int): PingStats {
        val received = rtts.filter { it > 0 }
        val total = sequence
        val lost = total - received.size

        val minRtt = received.minOrNull() ?: 0L
        val maxRtt = received.maxOrNull() ?: 0L
        val avgRtt = if (received.isNotEmpty()) received.sum() / received.size else 0L

        // Jitter: mean deviation between consecutive RTTs
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
