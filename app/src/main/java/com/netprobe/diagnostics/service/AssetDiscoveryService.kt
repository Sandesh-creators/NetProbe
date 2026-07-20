package com.netprobe.diagnostics.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.netprobe.diagnostics.MainActivity
import com.netprobe.diagnostics.NetProbeApp
import com.netprobe.diagnostics.R
import com.netprobe.diagnostics.discovery.AssetDiscoveryEngine
import kotlinx.coroutines.*

class AssetDiscoveryService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scanJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startDiscovery()
            ACTION_STOP -> stopDiscovery()
        }
        return START_STICKY
    }

    private fun startDiscovery() {
        val notification = buildNotification("Scanning network...")
        startForeground(NOTIFICATION_ID, notification)

        val app = application as NetProbeApp
        val engine = AssetDiscoveryEngine(this, app.database.assetDao())

        scanJob = scope.launch {
            while (isActive) {
                try {
                    val count = engine.discover()
                    updateNotification("Found $count devices this cycle")
                } catch (_: Exception) {}
                delay(CYCLE_INTERVAL_MS)
            }
        }
    }

    private fun stopDiscovery() {
        scanJob?.cancel()
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AssetDiscoveryService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NetProbe Asset Discovery")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Asset Discovery",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background asset discovery service"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.netprobe.action.ASSET_DISCOVERY_START"
        const val ACTION_STOP = "com.netprobe.action.ASSET_DISCOVERY_STOP"
        private const val CHANNEL_ID = "asset_discovery_channel"
        private const val NOTIFICATION_ID = 2001
        private const val CYCLE_INTERVAL_MS = 30_000L
    }
}
