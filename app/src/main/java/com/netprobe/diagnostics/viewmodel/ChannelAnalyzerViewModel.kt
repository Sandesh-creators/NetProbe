package com.netprobe.diagnostics.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netprobe.diagnostics.data.model.ChannelOccupancy
import com.netprobe.diagnostics.data.model.CongestionLevel
import com.netprobe.diagnostics.data.model.WifiBand
import com.netprobe.diagnostics.data.model.WifiNetworkInfo
import com.netprobe.diagnostics.scanner.BleScanner
import com.netprobe.diagnostics.scanner.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChannelAnalyzerViewModel(application: Application) : AndroidViewModel(application) {

    private val wifiScanner = WifiScanner(application)
    private val bleScanner = BleScanner(application)

    private val _analyzerState = MutableStateFlow<AnalyzerState>(AnalyzerState.Idle)
    val analyzerState: StateFlow<AnalyzerState> = _analyzerState.asStateFlow()

    private val _selectedBand = MutableStateFlow<WifiBand?>(null)
    val selectedBand: StateFlow<WifiBand?> = _selectedBand.asStateFlow()

    private var wifiScanJob: Job? = null

    fun startContinuousScan() {
        wifiScanJob?.cancel()

        if (!wifiScanner.isWifiEnabled()) {
            _analyzerState.value = AnalyzerState.Error("Wi-Fi is disabled")
            return
        }

        _analyzerState.value = AnalyzerState.Scanning(
            networks = emptyList(),
            channelOccupancy = emptyList(),
            bleChannelCount = 0
        )

        wifiScanJob = viewModelScope.launch {
            wifiScanner.scanWifiNetworks().collect { networks ->
                val occupancy = wifiScanner.computeChannelOccupancy(networks)
                _analyzerState.value = AnalyzerState.Scanning(
                    networks = networks,
                    channelOccupancy = occupancy,
                    bleChannelCount = estimateBleChannelCount()
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

    /**
     * BLE operates on channels 37-39 in the 2.4 GHz band.
     * Channel 37 = 2402 MHz, Channel 38 = 2426 MHz, Channel 39 = 2480 MHz
     * This is a rough estimate based on BLE scan activity.
     */
    private fun estimateBleChannelCount(): Int {
        // BLE advertisement channels map to Wi-Fi channels:
        // Ch 37 -> overlaps Wi-Fi Ch 1-6
        // Ch 38 -> overlaps Wi-Fi Ch 6-11
        // Ch 39 -> overlaps Wi-Fi Ch 11-14
        return 3 // BLE uses 3 advertising channels
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
