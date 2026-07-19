package com.netprobe.diagnostics.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netprobe.diagnostics.data.model.ChannelOccupancy
import com.netprobe.diagnostics.data.model.WifiBand
import com.netprobe.diagnostics.data.model.WifiNetworkInfo
import com.netprobe.diagnostics.scanner.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChannelAnalyzerViewModel(application: Application) : AndroidViewModel(application) {

    private val wifiScanner = WifiScanner(application)

    private val _analyzerState = MutableStateFlow<AnalyzerState>(AnalyzerState.Idle)
    val analyzerState: StateFlow<AnalyzerState> = _analyzerState.asStateFlow()

    private val _selectedBand = MutableStateFlow<WifiBand?>(null)
    val selectedBand: StateFlow<WifiBand?> = _selectedBand.asStateFlow()

    private var wifiScanJob: Job? = null
    private var emptyScanCount = 0

    fun startContinuousScan() {
        wifiScanJob?.cancel()
        emptyScanCount = 0

        val diag = wifiScanner.getDiagnostics()
        if (!diag.isOk()) {
            _analyzerState.value = AnalyzerState.Error(
                message = diag.issues.joinToString("\n") { "- $it" }
            )
            return
        }

        _analyzerState.value = AnalyzerState.Scanning(
            networks = emptyList(),
            channelOccupancy = emptyList(),
            bleChannelCount = 0,
            rawScanCount = 0,
            lastError = null
        )

        wifiScanJob = viewModelScope.launch {
            wifiScanner.scanWifiNetworks().collect { scanResult ->
                val occupancy = try {
                    wifiScanner.computeChannelOccupancy(scanResult.networks)
                } catch (_: Exception) {
                    emptyList()
                }

                if (scanResult.networks.isEmpty()) {
                    emptyScanCount++
                } else {
                    emptyScanCount = 0
                }

                // After 3 consecutive empty scans, show error with diagnostics
                if (emptyScanCount >= 3 && scanResult.networks.isEmpty()) {
                    val errorMsg = buildString {
                        appendLine("Scan returned 0 usable networks after $emptyScanCount attempts.")
                        appendLine()
                        if (scanResult.error != null) {
                            appendLine("DIAGNOSTIC: ${scanResult.error}")
                            appendLine()
                        }
                        appendLine("TROUBLESHOOTING:")
                        appendLine("1. Ensure Wi-Fi is ON and connected")
                        appendLine("2. Enable Location Services (Settings > Location > ON)")
                        appendLine("3. Grant Location permission to this app")
                        appendLine("4. Some devices need Wi-Fi scanning turned ON in")
                        appendLine("   Settings > Location > Wi-Fi scanning")
                        appendLine()
                        appendLine("Raw scan results: ${scanResult.rawCount}")
                        appendLine("After filtering: ${scanResult.filteredCount}")
                    }
                    _analyzerState.value = AnalyzerState.Error(errorMsg)
                    wifiScanJob?.cancel()
                    return@collect
                }

                _analyzerState.value = AnalyzerState.Scanning(
                    networks = scanResult.networks,
                    channelOccupancy = occupancy,
                    bleChannelCount = 3,
                    rawScanCount = scanResult.rawCount,
                    lastError = scanResult.error
                )
            }
        }
    }

    fun stopScan() {
        wifiScanJob?.cancel()
        wifiScanJob = null
    }

    fun selectBand(band: WifiBand?) {
        _selectedBand.value = band
    }

    fun getFilteredOccupancy(occupancy: List<ChannelOccupancy>): List<ChannelOccupancy> {
        val band = _selectedBand.value ?: return occupancy
        return occupancy.filter { it.band == band }
    }

    override fun onCleared() {
        super.onCleared()
        wifiScanJob?.cancel()
    }
}

sealed class AnalyzerState {
    data object Idle : AnalyzerState()
    data class Scanning(
        val networks: List<WifiNetworkInfo>,
        val channelOccupancy: List<ChannelOccupancy>,
        val bleChannelCount: Int,
        val rawScanCount: Int = 0,
        val lastError: String? = null
    ) : AnalyzerState()
    data class Error(val message: String) : AnalyzerState()
}
