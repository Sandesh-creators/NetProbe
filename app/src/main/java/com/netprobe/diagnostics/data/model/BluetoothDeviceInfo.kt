package com.netprobe.diagnostics.data.model

import android.bluetooth.BluetoothDevice

data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val type: DeviceType,
    val rssi: Int,
    val bondState: Int = BluetoothDevice.BOND_NONE,
    val uuids: List<String> = emptyList(),
    val batteryLevel: Int? = null,
    val txPower: Int? = null,
    val isConnectable: Boolean = true
)

enum class DeviceType(val displayName: String) {
    CLASSIC("Classic BT"),
    DUAL("Dual Mode"),
    LE("BLE"),
    UNKNOWN("Unknown");

    companion object {
        fun fromAndroidType(androidType: Int): DeviceType = when (androidType) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> CLASSIC
            BluetoothDevice.DEVICE_TYPE_DUAL -> DUAL
            BluetoothDevice.DEVICE_TYPE_LE -> LE
            else -> UNKNOWN
        }
    }
}
