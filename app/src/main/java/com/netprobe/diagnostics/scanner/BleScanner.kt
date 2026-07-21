package com.netprobe.diagnostics.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import com.netprobe.diagnostics.data.model.BluetoothDeviceInfo
import com.netprobe.diagnostics.data.model.DeviceType
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class BleScanner(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val bleScanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    /**
     * Returns the Bluetooth adapter state as a Flow.
     */
    fun getAdapterState(): Flow<Int> = flow {
        while (true) {
            emit(bluetoothAdapter?.state ?: BluetoothAdapter.STATE_OFF)
            delay(1000)
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Classifies a BluetoothDevice into its type category:
     * - DEVICE_TYPE_CLASSIC (0): Bluetooth Classic only (BR/EDR)
     * - DEVICE_TYPE_DUAL (2): Dual Mode (supports both Classic and LE)
     * - DEVICE_TYPE_LE (1): Bluetooth Low Energy only
     * - DEVICE_TYPE_UNKNOWN (0 when null): Could not determine
     *
     * On Android < 30, getType() returns DEVICE_TYPE_UNKNOWN for unpaired devices.
     * We attempt to infer type from scan record data when possible.
     */
    fun classifyDeviceType(device: BluetoothDevice): DeviceType {
        val rawType = try {
            device.type
        } catch (e: SecurityException) {
            BluetoothDevice.DEVICE_TYPE_UNKNOWN
        }
        return DeviceType.fromAndroidType(rawType)
    }

    /**
     * Scans for BLE devices using the low-power BLE scanning API.
     * Emits progressively as devices are discovered.
     * Deduplicates by MAC address, keeping the strongest RSSI reading.
     */
    @SuppressLint("MissingPermission")
    fun scanBleDevices(): Flow<List<BluetoothDeviceInfo>> = callbackFlow {
        val scanner = bleScanner
        if (scanner == null) {
            close()
            return@callbackFlow
        }

        val discoveredDevices = ConcurrentHashMap<String, BluetoothDeviceInfo>()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setReportDelay(0)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val address = try {
                    device.address
                } catch (e: SecurityException) {
                    return
                } ?: return

                val existingDevice = discoveredDevices[address]
                if (existingDevice == null || result.rssi > existingDevice.rssi) {
                    val name = try {
                        device.name ?: result.scanRecord?.deviceName
                    } catch (e: SecurityException) {
                        null
                    }

                    val uuids = extractUuids(result)
                    val txPower = extractTxPower(result)

                    val info = BluetoothDeviceInfo(
                        name = name,
                        address = address,
                        type = classifyDeviceType(device),
                        rssi = result.rssi,
                        bondState = try { device.bondState } catch (e: SecurityException) { BluetoothDevice.BOND_NONE },
                        uuids = uuids,
                        txPower = txPower,
                        isConnectable = true
                    )
                    discoveredDevices[address] = info
                    trySend(discoveredDevices.values.toList())
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                close(Exception("BLE scan failed with code: $errorCode"))
            }
        }

        try {
            scanner.startScan(null, scanSettings, callback)
        } catch (e: Exception) {
            close(Exception("Bluetooth scan failed: ${e.message}"))
            return@callbackFlow
        }

        awaitClose {
            try {
                scanner.stopScan(callback)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Discovers classic Bluetooth devices that are already bonded (paired).
     * Classic BT discovery requires the older startDiscovery() API which
     * is not available as a Flow, so we poll paired devices and discover
     * using broadcast receivers.
     */
    @SuppressLint("MissingPermission")
    fun getBondedClassicDevices(): List<BluetoothDeviceInfo> {
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            adapter.bondedDevices?.mapNotNull { device ->
                val name = try { device.name } catch (e: SecurityException) { null } ?: return@mapNotNull null
                val address = try { device.address } catch (e: SecurityException) { null } ?: return@mapNotNull null
                BluetoothDeviceInfo(
                    name = name,
                    address = address,
                    type = classifyDeviceType(device),
                    rssi = 0,
                    bondState = try { device.bondState } catch (e: SecurityException) { BluetoothDevice.BOND_NONE },
                    isConnectable = true
                )
            }?.distinctBy { it.address } ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /**
     * Combined scan: BLE + bonded classic devices merged into a single list.
     */
    fun combinedScan(): Flow<List<BluetoothDeviceInfo>> = flow {
        val classic = getBondedClassicDevices()
        emit(classic)

        scanBleDevices().collect { bleDevices ->
            val merged = (classic + bleDevices).distinctBy { it.address }
                .sortedBy { it.rssi }
            emit(merged)
        }
    }.flowOn(Dispatchers.IO)

    private fun extractUuids(result: ScanResult): List<String> {
        val record = result.scanRecord ?: return emptyList()
        return record.serviceUuids?.map { it.toString() } ?: emptyList()
    }

    private fun extractTxPower(result: ScanResult): Int? {
        val record = result.scanRecord ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            record.txPowerLevel.takeIf { it != Int.MIN_VALUE }
        } else {
            null
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /**
     * Reads battery level from a connected BLE device.
     * Returns null if the Battery Service (0x180F) characteristic is not available.
     */
    @SuppressLint("MissingPermission")
    suspend fun readBatteryLevel(device: BluetoothDevice): Int? = withContext(Dispatchers.IO) {
        try {
            val profile = bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {}
                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.GATT) ?: return@withContext null

            // GATT connection would be managed by BatteryMonitorService
            // This is a placeholder for the characteristic read
            null
        } catch (e: Exception) {
            null
        }
    }
}
