package com.netprobe.diagnostics.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netprobe.diagnostics.NetProbeApp
import com.netprobe.diagnostics.data.db.entity.RssiSampleEntity
import com.netprobe.diagnostics.heatmap.RssiCollector
import com.netprobe.diagnostics.data.db.dao.RssiSampleDao
import com.netprobe.diagnostics.data.db.dao.DeviceRssiSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HeatmapUiState(
    val isCollecting: Boolean = false,
    val intervalMs: Long = 2000,
    val selectedDeviceType: String = "ALL"
)

@OptIn(ExperimentalCoroutinesApi::class)
class HeatmapViewModel(application: Application) : AndroidViewModel(application) {

    private val rssiSampleDao: RssiSampleDao = (application as NetProbeApp).database.rssiSampleDao()
    val collector = RssiCollector(application, rssiSampleDao)

    private val _uiState = MutableStateFlow(HeatmapUiState())
    val uiState: StateFlow<HeatmapUiState> = _uiState.asStateFlow()

    val deviceSummaries: StateFlow<List<DeviceRssiSummary>> = _uiState.flatMapLatest { state ->
        flow {
            while (true) {
                val since = System.currentTimeMillis() - 300_000L
                emit(rssiSampleDao.getDeviceSummary(since))
                kotlinx.coroutines.delay(5000)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSamples: StateFlow<List<RssiSampleEntity>> = flow {
        while (true) {
            val since = System.currentTimeMillis() - 300_000L
            emit(rssiSampleDao.getSamplesSince("ALL", since))
            kotlinx.coroutines.delay(5000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleCollecting() {
        if (_uiState.value.isCollecting) {
            collector.stopCollecting()
            _uiState.update { it.copy(isCollecting = false) }
        } else {
            collector.startCollecting(viewModelScope, _uiState.value.intervalMs)
            _uiState.update { it.copy(isCollecting = true) }
        }
    }

    fun setInterval(ms: Long) {
        _uiState.update { it.copy(intervalMs = ms) }
        if (_uiState.value.isCollecting) {
            collector.stopCollecting()
            collector.startCollecting(viewModelScope, ms)
        }
    }

    fun setSelectedDeviceType(type: String) {
        _uiState.update { it.copy(selectedDeviceType = type) }
    }

    override fun onCleared() {
        super.onCleared()
        collector.stopCollecting()
    }
}
