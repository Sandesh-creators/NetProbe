package com.netprobe.diagnostics.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netprobe.diagnostics.pcap.PcapExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class PcapExportState {
    data object Idle : PcapExportState()
    data object Exporting : PcapExportState()
    data class Success(val message: String) : PcapExportState()
    data class Error(val message: String) : PcapExportState()
}

class PcapExportViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<PcapExportState>(PcapExportState.Idle)
    val state: StateFlow<PcapExportState> = _state.asStateFlow()

    fun exportAndShare() {
        viewModelScope.launch {
            _state.value = PcapExportState.Exporting
            try {
                val context = getApplication<Application>()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val filename = "netprobe_capture_$timestamp.pcap"
                val file = withContext(Dispatchers.IO) {
                    PcapExporter.exportToFile(context, filename)
                }
                withContext(Dispatchers.Main) {
                    PcapExporter.shareFile(context, file)
                }
                _state.value = PcapExportState.Success("Exported ${file.name}")
            } catch (e: Exception) {
                _state.value = PcapExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun reset() {
        _state.value = PcapExportState.Idle
    }
}
