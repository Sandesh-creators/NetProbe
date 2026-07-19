package com.netprobe.diagnostics.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netprobe.diagnostics.data.db.AppDatabase
import com.netprobe.diagnostics.data.model.HostInfo
import com.netprobe.diagnostics.data.model.PingStats
import com.netprobe.diagnostics.data.model.PortInfo
import com.netprobe.diagnostics.scanner.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SshApp(
    val id: String,
    val name: String,
    val packageName: String,
    val installUrl: String
)

private val KNOWN_SSH_APPS = listOf(
    SshApp("termux", "Termux", "com.termux", "https://f-droid.org/en/packages/com.termux/"),
    SshApp("juicessh", "JuiceSSH", "com.sonelli.juicessh", "https://play.google.com/store/apps/details?id=com.sonelli.juicessh"),
    SshApp("connectbot", "ConnectBot", "org.connectbot", "https://play.google.com/store/apps/details?id=org.connectbot"),
    SshApp("termius", "Termius", "com.techvss.termius", "https://play.google.com/store/apps/details?id=com.techvss.termius"),
    SshApp("serverauditor", "Server Auditor", "com.server.auditor.server.auditor", "https://play.google.com/store/apps/details?id=com.server.auditor.server.auditor"),
    SshApp("sshclient", "SSH Client", "com.westcliin.sshclient", "https://play.google.com/store/apps/details?id=com.westcliin.sshclient"),
)

class LanScanViewModel(application: Application) : AndroidViewModel(application) {

    private val lanScanner = LanScanner(application)
    private val database = AppDatabase.getDatabase(application)
    private val portScanner = PortScanner(database.portDao())
    private val pingDiagnostic = PingDiagnostic()
    private val tracerouteDiagnostic = TracerouteDiagnostic()
    private val context = application

    private val _scanState = MutableStateFlow<LanScanState>(LanScanState.Idle)
    val scanState: StateFlow<LanScanState> = _scanState.asStateFlow()

    private val _portScanState = MutableStateFlow<PortScanState>(PortScanState.Idle)
    val portScanState: StateFlow<PortScanState> = _portScanState.asStateFlow()

    private val _pingState = MutableStateFlow<PingState>(PingState.Idle)
    val pingState: StateFlow<PingState> = _pingState.asStateFlow()

    private val _tracerouteState = MutableStateFlow<TracerouteViewState>(TracerouteViewState.Idle)
    val tracerouteState: StateFlow<TracerouteViewState> = _tracerouteState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _sshDialogState = MutableStateFlow<SshDialogState>(SshDialogState.Hidden)
    val sshDialogState: StateFlow<SshDialogState> = _sshDialogState.asStateFlow()

    private var scanJob: Job? = null
    private var portScanJob: Job? = null
    private var pingJob: Job? = null
    private var tracerouteJob: Job? = null

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
                    is PortScanProgress.Scanning ->
                        PortScanState.Scanning(
                            targetIp = progress.targetIp,
                            scannedPorts = progress.scannedPorts,
                            totalPorts = progress.totalPorts,
                            openPorts = progress.openPorts
                        )
                    is PortScanProgress.Complete ->
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

    fun startTraceroute(targetIp: String) {
        tracerouteJob?.cancel()
        _tracerouteState.value = TracerouteViewState.Tracing(targetIp)
        tracerouteJob = viewModelScope.launch {
            tracerouteDiagnostic.traceRoute(targetIp).collect { event ->
                when (event) {
                    is TracerouteEvent.Hop -> {
                        _tracerouteState.value = TracerouteViewState.Tracing(
                            targetIp = targetIp,
                            hops = event.allHops
                        )
                    }
                    is TracerouteEvent.Complete -> {
                        _tracerouteState.value = TracerouteViewState.Complete(
                            targetIp = targetIp,
                            hops = event.hops
                        )
                    }
                }
            }
        }
    }

    fun stopTraceroute() {
        tracerouteJob?.cancel()
        tracerouteJob = null
        _tracerouteState.value = TracerouteViewState.Idle
    }

    fun sendWol(macAddress: String) {
        viewModelScope.launch {
            val result = WakeOnLan.sendMagicPacket(macAddress)
            _toastMessage.value = result.getOrElse { "WoL failed: ${it.message}" }
        }
    }

    fun openSsh(host: String, port: Int = 22) {
        val installedApps = KNOWN_SSH_APPS.filter { app ->
            try {
                context.packageManager.getPackageInfo(app.packageName, 0)
                true
            } catch (_: Exception) {
                false
            }
        }

        if (installedApps.isEmpty()) {
            _sshDialogState.value = SshDialogState.Showing(KNOWN_SSH_APPS)
            return
        }

        val preferred = installedApps.firstOrNull { it.id == "termux" }
            ?: installedApps.firstOrNull { it.id == "juicessh" }
            ?: installedApps.firstOrNull { it.id == "connectbot" }
            ?: installedApps.first()

        when (preferred.id) {
            "termux" -> openTermux(host, port, installedApps)
            "juicessh" -> openSshUri("ssh://$host:$port", preferred, installedApps)
            "connectbot" -> openSshUri("ssh://$host:$port", preferred, installedApps)
            "termius" -> openSshUri("ssh://$host:$port", preferred, installedApps)
            "serverauditor" -> openSshUri("ssh://$host:$port", preferred, installedApps)
            else -> openSshUri("ssh://$host:$port", preferred, installedApps)
        }
    }

    private fun openTermux(host: String, port: Int, installedApps: List<SshApp>) {
        val cmd = if (port == 22) "ssh $host" else "ssh -p $port $host"

        // Try Termux RUN_COMMAND intent (executes directly without clipboard)
        try {
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                setClassName("com.termux", "com.termux.app.RunCommandActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/ssh")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-p", port.toString(), host))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            }
            context.startActivity(intent)
            _toastMessage.value = "Opening SSH in Termux"
            return
        } catch (_: Exception) { }

        // Fallback: copy to clipboard + launch Termux
        try {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("ssh", cmd)
            clipboard.setPrimaryClip(clip)
        } catch (_: Exception) { }

        try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.termux")
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                _toastMessage.value = "SSH command copied — paste in Termux"
                return
            }
        } catch (_: Exception) { }

        _sshDialogState.value = SshDialogState.Showing(installedApps)
    }

    private fun openSshUri(uriString: String, app: SshApp, installedApps: List<SshApp>) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                setPackage(app.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            _toastMessage.value = "Opening SSH in ${app.name}"
            return
        } catch (_: Exception) { }

        try {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                _toastMessage.value = "Opened ${app.name} — connect manually to SSH"
                return
            }
        } catch (_: Exception) { }

        _sshDialogState.value = SshDialogState.Showing(installedApps)
    }

    fun dismissSshDialog() {
        _sshDialogState.value = SshDialogState.Hidden
    }

    fun openInstallUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            _toastMessage.value = "Could not open install link"
        }
    }

    fun clearToast() {
        _toastMessage.value = null
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
        tracerouteJob?.cancel()
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

sealed class TracerouteViewState {
    data object Idle : TracerouteViewState()
    data class Tracing(
        val targetIp: String,
        val hops: List<TracerouteHop> = emptyList()
    ) : TracerouteViewState()
    data class Complete(
        val targetIp: String,
        val hops: List<TracerouteHop>
    ) : TracerouteViewState()
}

sealed class SshDialogState {
    data object Hidden : SshDialogState()
    data class Showing(val apps: List<SshApp>) : SshDialogState()
}
