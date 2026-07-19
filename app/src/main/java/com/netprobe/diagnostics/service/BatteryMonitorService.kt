package com.netprobe.diagnostics.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.netprobe.diagnostics.MainActivity
import com.netprobe.diagnostics.R

class BatteryMonitorService : Service() {

    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_DEVICE_NAME = "device_name"
        const val CHANNEL_ID = "battery_monitor_channel"
        const val NOTIFICATION_ID = 2001
        const val LOW_BATTERY_THRESHOLD = 15

        // Battery Service UUID (SIG Standard)
        val BATTERY_SERVICE_UUID = java.util.UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val BATTERY_LEVEL_UUID = java.util.UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var targetDeviceAddress: String = ""
    private var targetDeviceName: String = ""
    private var monitorRunnable: Runnable? = null

    private val bluetoothManager: BluetoothManager
        get() = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetDeviceAddress = intent?.getStringExtra(EXTRA_DEVICE_ADDRESS) ?: ""
        targetDeviceName = intent?.getStringExtra(EXTRA_DEVICE_NAME) ?: "Unknown"

        if (targetDeviceAddress.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Monitoring $targetDeviceName..."))
        connectToDevice()
        scheduleReconnect()

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {
        val adapter = bluetoothManager.adapter ?: run {
            showNotification("Bluetooth unavailable", true)
            stopSelf()
            return
        }

        val device = adapter.getRemoteDevice(targetDeviceAddress) ?: run {
            showNotification("Device not found", true)
            stopSelf()
            return
        }

        bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    showNotification("Connected to $targetDeviceName — discovering services")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    showNotification("Disconnected from $targetDeviceName — reconnecting...")
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val batteryService = gatt.getService(BATTERY_SERVICE_UUID)
                val batteryChar = batteryService?.getCharacteristic(BATTERY_LEVEL_UUID)

                if (batteryChar != null) {
                    gatt.readCharacteristic(batteryChar)
                } else {
                    showNotification("$targetDeviceName: No battery service available")
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BATTERY_LEVEL_UUID) {
                val batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                handleBatteryLevel(batteryLevel)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BATTERY_LEVEL_UUID) {
                val batteryLevel = value.firstOrNull()?.toInt() ?: return
                handleBatteryLevel(batteryLevel)
            }
        }
    }

    private fun handleBatteryLevel(level: Int) {
        val icon = if (level <= LOW_BATTERY_THRESHOLD) "!" else ""
        val msg = "$icon $targetDeviceName BATTERY: $level%"

        showNotification(msg)

        if (level <= LOW_BATTERY_THRESHOLD) {
            fireLowBatteryAlert(level)
        }

        // Re-read battery every 60 seconds
        android.os.Handler(mainLooper).postDelayed({
            try {
                bluetoothGatt?.let { gatt ->
                    val service = gatt.getService(BATTERY_SERVICE_UUID)
                    val char = service?.getCharacteristic(BATTERY_LEVEL_UUID)
                    if (char != null) gatt.readCharacteristic(char)
                }
            } catch (_: Exception) { }
        }, 60_000)
    }

    private fun fireLowBatteryAlert(level: Int) {
        val alertManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alert = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("LOW BATTERY ALERT")
            .setContentText("$targetDeviceName at $level% — may go out of range")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(0xFFFF1744.toInt())
            .build()

        alertManager.notify(NOTIFICATION_ID + 1, alert)
    }

    private fun scheduleReconnect() {
        monitorRunnable = Runnable {
            bluetoothGatt?.let { gatt ->
                val service = gatt.getService(BATTERY_SERVICE_UUID)
                val char = service?.getCharacteristic(BATTERY_LEVEL_UUID)
                if (char != null) {
                    gatt.readCharacteristic(char)
                } else {
                    // Reconnect if services were lost
                    bluetoothGatt?.close()
                    connectToDevice()
                }
            }
            android.os.Handler(mainLooper).postDelayed(monitorRunnable!!, 120_000)
        }
        android.os.Handler(mainLooper).postDelayed(monitorRunnable!!, 120_000)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Battery Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors BLE peripheral battery levels"
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("NETPROBE — Battery Monitor")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showNotification(text: String, stopAfter: Boolean = false) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
        if (stopAfter) stopSelf()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        monitorRunnable?.let { android.os.Handler(mainLooper).removeCallbacks(it) }
        bluetoothGatt?.close()
        bluetoothGatt = null
        super.onDestroy()
    }
}
