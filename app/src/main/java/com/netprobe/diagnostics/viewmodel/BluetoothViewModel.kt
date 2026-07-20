package com.netprobe.diagnostics.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netprobe.diagnostics.data.model.BluetoothDeviceInfo
import com.netprobe.diagnostics.scanner.BleScanner
import com.netprobe.diagnostics.service.BatteryMonitorService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bleScanner = BleScanner(application)

    private val _btState = MutableStateFlow<BluetoothState>(BluetoothState.Initializing)
    val btState: StateFlow<BluetoothState> = _btState.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BluetoothDeviceInfo?>(null)
    val selectedDevice: StateFlow<BluetoothDeviceInfo?> = _selectedDevice.asStateFlow()

    private var scanJob: Job? = null
    private var stateMonitorJob: Job? = null

    init {
        checkBluetoothState()
        startStateMonitor()
    }

    private fun checkBluetoothState() {
        if (!bleScanner.isBluetoothEnabled()) {
            _btState.value = BluetoothState.BluetoothOff
        } else {
            if (_btState.value is BluetoothState.Initializing) {
                _btState.value = BluetoothState.Ready
            }
        }
    }

    private fun startStateMonitor() {
        stateMonitorJob?.cancel()
        stateMonitorJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                val isEnabled = bleScanner.isBluetoothEnabled()
                val current = _btState.value

                if (!isEnabled && current !is BluetoothState.BluetoothOff && current !is BluetoothState.Initializing) {
                    scanJob?.cancel()
                    scanJob = null
                    _btState.value = BluetoothState.BluetoothOff
                } else if (isEnabled && current is BluetoothState.BluetoothOff) {
                    _btState.value = BluetoothState.Ready
                }
            }
        }
    }

    fun startScan() {
        scanJob?.cancel()
        if (!bleScanner.isBluetoothEnabled()) {
            _btState.value = BluetoothState.BluetoothOff
            return
        }

        _btState.value = BluetoothState.Scanning(emptyList())
        scanJob = viewModelScope.launch {
            try {
                val classic = bleScanner.getBondedClassicDevices()
                _btState.value = BluetoothState.Scanning(classic)

                bleScanner.scanBleDevices().collect { bleDevices ->
                    val merged = (classic + bleDevices)
                        .distinctBy { it.address }
                        .sortedByDescending { it.rssi }
                    _btState.value = BluetoothState.Scanning(merged)
                }
            } catch (e: Exception) {
                _btState.value = BluetoothState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        val current = _btState.value
        if (current is BluetoothState.Scanning) {
            _btState.value = BluetoothState.Paused(current.devices)
        }
    }

    fun selectDevice(device: BluetoothDeviceInfo) {
        _selectedDevice.value = device
    }

    fun clearSelection() {
        _selectedDevice.value = null
    }

    fun enableBluetooth() {
        val context = getApplication<Application>()
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun startBatteryMonitor(device: BluetoothDeviceInfo) {
        val context = getApplication<Application>()
        val intent = Intent(context, BatteryMonitorService::class.java).apply {
            putExtra(BatteryMonitorService.EXTRA_DEVICE_ADDRESS, device.address)
            putExtra(BatteryMonitorService.EXTRA_DEVICE_NAME, device.name ?: "Unknown")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopBatteryMonitor() {
        val context = getApplication<Application>()
        context.stopService(Intent(context, BatteryMonitorService::class.java))
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        stateMonitorJob?.cancel()
    }
}

sealed class BluetoothState {
    data object Initializing : BluetoothState()
    data object BluetoothOff : BluetoothState()
    data object Ready : BluetoothState()
    data class Scanning(val devices: List<BluetoothDeviceInfo>) : BluetoothState()
    data class Paused(val devices: List<BluetoothDeviceInfo>) : BluetoothState()
    data class Error(val message: String) : BluetoothState()
}
