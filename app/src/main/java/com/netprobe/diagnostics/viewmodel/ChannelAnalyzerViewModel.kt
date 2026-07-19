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

    fun startContinuousScan() {
        wifiScanJob?.cancel()

        if (!wifiScanner.isWifiEnabled()) {
            _analyzerState.value = AnalyzerState.Error("Wi-Fi is disabled. Enable Wi-Fi to scan.")
            return
        }

        _analyzerState.value = AnalyzerState.Scanning(
            networks = emptyList(),
            channelOccupancy = emptyList(),
            bleChannelCount = 0
        )

        wifiScanJob = viewModelScope.launch {
            try {
                wifiScanner.scanWifiNetworks().collect { networks ->
                    val occupancy = try {
                        wifiScanner.computeChannelOccupancy(networks)
                    } catch (_: Exception) {
                        emptyList()
                    }
                    _analyzerState.value = AnalyzerState.Scanning(
                        networks = networks,
                        channelOccupancy = occupancy,
                        bleChannelCount = 3
                    )
                }
            } catch (_: Exception) {
                _analyzerState.value = AnalyzerState.Error("Scan failed. Check Wi-Fi and location permissions.")
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
        val bleChannelCount: Int
    ) : AnalyzerState()
    data class Error(val message: String) : AnalyzerState()
}
