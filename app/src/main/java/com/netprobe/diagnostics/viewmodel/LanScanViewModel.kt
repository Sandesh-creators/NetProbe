package com.netprobe.diagnostics.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netprobe.diagnostics.data.db.AppDatabase
import com.netprobe.diagnostics.data.model.HostInfo
import com.netprobe.diagnostics.data.model.PingStats
import com.netprobe.diagnostics.data.model.PortInfo
import com.netprobe.diagnostics.scanner.LanScanner
import com.netprobe.diagnostics.scanner.PingDiagnostic
import com.netprobe.diagnostics.scanner.PingEvent
import com.netprobe.diagnostics.scanner.PortScanner
import com.netprobe.diagnostics.scanner.ScanProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LanScanViewModel(application: Application) : AndroidViewModel(application) {

    private val lanScanner = LanScanner(application)
    private val database = AppDatabase.getDatabase(application)
    private val portScanner = PortScanner(database.portDao())
    private val pingDiagnostic = PingDiagnostic()

    // ── Scan State ──────────────────────────────────────────────
    private val _scanState = MutableStateFlow<LanScanState>(LanScanState.Idle)
    val scanState: StateFlow<LanScanState> = _scanState.asStateFlow()

    // ── Port Scan State ─────────────────────────────────────────
    private val _portScanState = MutableStateFlow<PortScanState>(PortScanState.Idle)
    val portScanState: StateFlow<PortScanState> = _portScanState.asStateFlow()

    // ── Ping State ──────────────────────────────────────────────
    private val _pingState = MutableStateFlow<PingState>(PingState.Idle)
    val pingState: StateFlow<PingState> = _pingState.asStateFlow()

    private var scanJob: Job? = null
    private var portScanJob: Job? = null
    private var pingJob: Job? = null

    fun startSubnetScan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _scanState.value = LanScanState.Loading
            lanScanner.scanSubnet().collect { progress ->
                _scanState.value = when (progress) {
                    is ScanProgress.Scanning -> LanScanState.Scanning(
                        scannedHosts = progress.scannedHosts,
                        totalHosts = progress.totalHosts,
                        aliveHosts = progress.aliveHosts,
                        subnet = "${progress.subnetInfo.subnetPrefix}.0/${progress.subnetInfo.cidr}",
                        gateway = progress.subnetInfo.gatewayIp
                    )
                    is ScanProgress.Complete -> LanScanState.Complete(
                        aliveHosts = progress.aliveHosts,
                        subnet = "${progress.subnetInfo.subnetPrefix}.0/${progress.subnetInfo.cidr}",
                        gateway = progress.subnetInfo.gatewayIp
                    )
                    is ScanProgress.Error -> LanScanState.Error(progress.message)
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }

    fun startPortScan(targetIp: String) {
        portScanJob?.cancel()
        portScanJob = viewModelScope.launch {
            _portScanState.value = PortScanState.Scanning(targetIp, 0, 22, emptyList())
            portScanner.scanPorts(targetIp).collect { progress ->
                _portScanState.value = when (progress) {
                    is com.netprobe.diagnostics.scanner.PortScanProgress.Scanning ->
                        PortScanState.Scanning(
                            targetIp = progress.targetIp,
                            scannedPorts = progress.scannedPorts,
                            totalPorts = progress.totalPorts,
                            openPorts = progress.openPorts
                        )
                    is com.netprobe.diagnostics.scanner.PortScanProgress.Complete ->
                        PortScanState.Complete(
                            targetIp = progress.targetIp,
                            openPorts = progress.openPorts
                        )
                }
            }
        }
    }

    fun startPing(hostIp: String) {
        pingJob?.cancel()
        _pingState.value = PingState.Pinging(hostIp)
        pingJob = viewModelScope.launch {
            pingDiagnostic.continuousPing(hostIp).collect { event ->
                when (event) {
                    is PingEvent.Result -> {
                        val current = _pingState.value as? PingState.Pinging
                        _pingState.value = PingState.Pinging(
                            host = hostIp,
                            lastResult = event.ping,
                            stats = current?.stats
                        )
                    }
                    is PingEvent.Stats -> {
                        val current = _pingState.value as? PingState.Pinging
                        _pingState.value = PingState.Pinging(
                            host = hostIp,
                            lastResult = current?.lastResult,
                            stats = event.stats
                        )
                    }
                }
            }
        }
    }

    fun stopPing() {
        pingJob?.cancel()
        pingJob = null
        _pingState.value = PingState.Idle
    }

    fun dismissPortScan() {
        portScanJob?.cancel()
        _portScanState.value = PortScanState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        portScanJob?.cancel()
        pingJob?.cancel()
    }
}

sealed class LanScanState {
    data object Idle : LanScanState()
    data object Loading : LanScanState()
    data class Scanning(
        val scannedHosts: Int,
        val totalHosts: Int,
        val aliveHosts: List<HostInfo>,
        val subnet: String,
        val gateway: String
    ) : LanScanState()
    data class Complete(
        val aliveHosts: List<HostInfo>,
        val subnet: String,
        val gateway: String
    ) : LanScanState()
    data class Error(val message: String) : LanScanState()
}

sealed class PortScanState {
    data object Idle : PortScanState()
    data class Scanning(
        val targetIp: String,
        val scannedPorts: Int,
        val totalPorts: Int,
        val openPorts: List<PortInfo>
    ) : PortScanState()
    data class Complete(
        val targetIp: String,
        val openPorts: List<PortInfo>
    ) : PortScanState()
}

sealed class PingState {
    data object Idle : PingState()
    data class Pinging(
        val host: String,
        val lastResult: com.netprobe.diagnostics.data.model.PingResult? = null,
        val stats: PingStats? = null
    ) : PingState()
}
