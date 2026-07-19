package com.netprobe.diagnostics.scanner

import com.netprobe.diagnostics.data.db.PortDao
import com.netprobe.diagnostics.data.model.PortInfo
import com.netprobe.diagnostics.data.model.PortState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.InetSocketAddress
import java.net.Socket

class PortScanner(private val portDao: PortDao) {

    companion object {
        val COMMON_PORTS = intArrayOf(
            21, 22, 23, 25, 53, 80, 110, 143, 443, 445,
            993, 995, 3306, 3389, 5432, 5900, 8080, 8443,
            8888, 9090, 9200, 27017
        )

        const val CONNECT_TIMEOUT_MS = 800
        const val BATCH_SIZE = 20
    }

    fun scanPorts(
        targetIp: String,
        ports: IntArray = COMMON_PORTS
    ): Flow<PortScanProgress> = flow {
        emit(PortScanProgress.Scanning(
            targetIp = targetIp,
            totalPorts = ports.size,
            scannedPorts = 0,
            openPorts = emptyList()
        ))

        val openPorts = mutableListOf<PortInfo>()

        for (batchStart in ports.indices step BATCH_SIZE) {
            val batchEnd = minOf(batchStart + BATCH_SIZE, ports.size)
            val batch = coroutineScope {
                ports.slice(batchStart until batchEnd).map { port ->
                    async(Dispatchers.IO) {
                        scanSinglePort(targetIp, port)
                    }
                }
            }

            val results = batch.awaitAll().filterNotNull()
            openPorts.addAll(results)

            emit(PortScanProgress.Scanning(
                targetIp = targetIp,
                totalPorts = ports.size,
                scannedPorts = batchEnd,
                openPorts = openPorts.sortedBy { it.port }
            ))
        }

        emit(PortScanProgress.Complete(
            targetIp = targetIp,
            openPorts = openPorts.sortedBy { it.port }
        ))
    }.flowOn(Dispatchers.Default)

    private suspend fun scanSinglePort(targetIp: String, port: Int): PortInfo? {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(targetIp, port), CONNECT_TIMEOUT_MS)
            socket.close()

            val entity = portDao.getPortInfo(port)

            PortInfo(
                port = port,
                protocol = entity?.protocol ?: "TCP",
                service = entity?.service ?: "Unknown",
                state = PortState.OPEN
            )
        } catch (e: java.net.ConnectException) {
            null
        } catch (e: java.net.SocketTimeoutException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun isPortOpen(targetIp: String, port: Int): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(targetIp, port), CONNECT_TIMEOUT_MS)
            socket.close()
            true
        } catch (_: Exception) {
            false
        }
    }
}

sealed class PortScanProgress {
    data class Scanning(
        val targetIp: String,
        val totalPorts: Int,
        val scannedPorts: Int,
        val openPorts: List<PortInfo>
    ) : PortScanProgress()

    data class Complete(
        val targetIp: String,
        val openPorts: List<PortInfo>
    ) : PortScanProgress()
}
