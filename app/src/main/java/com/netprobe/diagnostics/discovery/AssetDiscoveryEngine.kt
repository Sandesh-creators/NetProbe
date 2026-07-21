package com.netprobe.diagnostics.discovery

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.net.wifi.WifiManager
import com.netprobe.diagnostics.data.db.dao.AssetDao
import com.netprobe.diagnostics.data.db.entity.AssetEventEntity
import com.netprobe.diagnostics.data.db.entity.DiscoveredAssetEntity
import com.netprobe.diagnostics.data.oui.OuiLookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AssetDiscoveryEngine(
    private val context: Context,
    private val assetDao: AssetDao
) {
    data class ArpDevice(
        val ip: String,
        val mac: String
    )

    data class BleDevice(
        val address: String,
        val name: String?,
        val rssi: Int
    )

    suspend fun discover() = withContext(Dispatchers.IO) {
        val arpDevices = readArpTable()
        val bleDevices = scanBle()
        val now = System.currentTimeMillis()

        arpDevices.forEach { device ->
            if (device.mac == "00:00:00:00:00:00") return@forEach
            val existing = assetDao.findByMac(device.mac.uppercase())
            val vendor = OuiLookup.getVendorName(device.mac)
            if (existing != null) {
                val updated = existing.copy(
                    ipAddress = device.ip,
                    lastSeen = now,
                    ouiVendor = vendor ?: existing.ouiVendor
                )
                assetDao.upsertAsset(updated)
                assetDao.insertEvent(
                    AssetEventEntity(
                        assetId = updated.id,
                        eventType = "ARP_SEEN",
                        ipAddress = device.ip,
                        rssi = null,
                        eventTime = now
                    )
                )
            } else {
                val newAsset = DiscoveredAssetEntity(
                    macAddress = device.mac.uppercase(),
                    ipAddress = device.ip,
                    hostname = null,
                    deviceName = null,
                    source = "ARP",
                    deviceType = "LAN",
                    firstSeen = now,
                    lastSeen = now,
                    isKnown = false,
                    isFlagged = false,
                    ouiVendor = vendor
                )
                val id = assetDao.upsertAsset(newAsset)
                assetDao.insertEvent(
                    AssetEventEntity(
                        assetId = id,
                        eventType = "DISCOVERED",
                        ipAddress = device.ip,
                        rssi = null,
                        eventTime = now
                    )
                )
            }
        }

        bleDevices.forEach { device ->
            val existing = assetDao.findByMac(device.address.uppercase())
            val vendor = OuiLookup.getVendorName(device.address)
            if (existing != null) {
                val updated = existing.copy(
                    lastSeen = now,
                    deviceName = device.name ?: existing.deviceName,
                    ouiVendor = vendor ?: existing.ouiVendor
                )
                assetDao.upsertAsset(updated)
                assetDao.insertEvent(
                    AssetEventEntity(
                        assetId = updated.id,
                        eventType = "BLE_SEEN",
                        ipAddress = null,
                        rssi = device.rssi,
                        eventTime = now
                    )
                )
            } else {
                val newAsset = DiscoveredAssetEntity(
                    macAddress = device.address.uppercase(),
                    ipAddress = null,
                    hostname = null,
                    deviceName = device.name,
                    source = "BLE",
                    deviceType = "WIRELESS",
                    firstSeen = now,
                    lastSeen = now,
                    isKnown = false,
                    isFlagged = false,
                    ouiVendor = vendor
                )
                val id = assetDao.upsertAsset(newAsset)
                assetDao.insertEvent(
                    AssetEventEntity(
                        assetId = id,
                        eventType = "DISCOVERED",
                        ipAddress = null,
                        rssi = device.rssi,
                        eventTime = now
                    )
                )
            }
        }

        arpDevices.size + bleDevices.size
    }

    private fun readArpTable(): List<ArpDevice> {
        val devices = mutableListOf<ArpDevice>()
        try {
            BufferedReader(FileReader("/proc/net/arp")).useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        val mac = parts[3]
                        if (mac != "00:00:00:00:00:00") {
                            devices.add(ArpDevice(ip = parts[0], mac = mac))
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return devices
    }

    private fun scanBle(): List<BleDevice> {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter ?: return emptyList()
        val scanner = adapter.bluetoothLeScanner ?: return emptyList()
        val devices = mutableListOf<BleDevice>()
        val latch = CountDownLatch(1)

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                try {
                    val device = result.device
                    val address = device.address ?: return
                    devices.add(
                        BleDevice(
                            address = address,
                            name = try { device.name } catch (_: SecurityException) { null } ?: "Unknown",
                            rssi = result.rssi
                        )
                    )
                } catch (_: SecurityException) { }
            }
            override fun onScanFailed(errorCode: Int) {
                latch.countDown()
            }
        }

        try {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()
            scanner.startScan(null, settings, callback)
            latch.await(5, TimeUnit.SECONDS)
            scanner.stopScan(callback)
        } catch (_: Exception) {
            try { scanner.stopScan(callback) } catch (_: Exception) {}
        }
        return devices.distinctBy { it.address }
    }
}
