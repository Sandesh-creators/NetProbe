package com.netprobe.diagnostics.heatmap

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.net.wifi.ScanResult as WifiScanResult
import android.net.wifi.WifiManager
import com.netprobe.diagnostics.data.db.dao.RssiSampleDao
import com.netprobe.diagnostics.data.db.entity.RssiSampleEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RssiSnapshot(
    val sourceType: String,
    val address: String,
    val name: String?,
    val rssi: Int,
    val frequency: Int?
)

class RssiCollector(
    private val context: Context,
    private val rssiSampleDao: RssiSampleDao
) {
    private val _snapshots = MutableStateFlow<List<RssiSnapshot>>(emptyList())
    val snapshots: StateFlow<List<RssiSnapshot>> = _snapshots.asStateFlow()

    private var collectionJob: Job? = null

    @SuppressLint("MissingPermission")
    fun startCollecting(scope: CoroutineScope, intervalMs: Long = 2000) {
        collectionJob?.cancel()
        collectionJob = scope.launch {
            while (isActive) {
                val allSnapshots = mutableListOf<RssiSnapshot>()
                allSnapshots.addAll(collectWifi())
                allSnapshots.addAll(collectBle())
                _snapshots.value = allSnapshots

                val now = System.currentTimeMillis()
                allSnapshots.forEach { snap ->
                    rssiSampleDao.insert(
                        RssiSampleEntity(
                            sourceType = snap.sourceType,
                            sourceAddress = snap.address,
                            sourceName = snap.name,
                            rssi = snap.rssi,
                            frequency = snap.frequency,
                            timestamp = now
                        )
                    )
                }
                delay(intervalMs)
            }
        }
    }

    fun stopCollecting() {
        collectionJob?.cancel()
        collectionJob = null
    }

    @SuppressLint("MissingPermission")
    private fun collectWifi(): List<RssiSnapshot> {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return emptyList()
        val results = try {
            @Suppress("DEPRECATION")
            wifiManager.scanResults ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return results.map { result ->
            RssiSnapshot(
                sourceType = "WIFI",
                address = result.BSSID ?: "unknown",
                name = @Suppress("DEPRECATION") result.SSID?.ifEmpty { null },
                rssi = result.level,
                frequency = result.frequency
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun collectBle(): List<RssiSnapshot> {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return emptyList()
        val adapter = btManager.adapter ?: return emptyList()
        val scanner = adapter.bluetoothLeScanner ?: return emptyList()
        val results = mutableListOf<RssiSnapshot>()
        val latch = kotlinx.coroutines.CompletableDeferred<Unit>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                try {
                    val device = result.device
                    val address = device.address ?: return
                    results.add(
                        RssiSnapshot(
                            sourceType = "BLE",
                            address = address,
                            name = try { device.name } catch (_: SecurityException) { null },
                            rssi = result.rssi,
                            frequency = null
                        )
                    )
                } catch (_: SecurityException) { }
            }
            override fun onScanFailed(errorCode: Int) { latch.complete(Unit) }
        }

        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()
            scanner.startScan(null, settings, callback)
            Thread.sleep(3000)
            scanner.stopScan(callback)
        } catch (_: Exception) {
            try { scanner.stopScan(callback) } catch (_: Exception) {}
        }
        return results.distinctBy { it.address }
    }
}
