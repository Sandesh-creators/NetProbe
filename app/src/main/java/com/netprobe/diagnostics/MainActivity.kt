package com.netprobe.diagnostics

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.netprobe.diagnostics.ui.screens.MainScreen
import com.netprobe.diagnostics.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val requiredPermissions: Array<String>
        get() {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms.add(Manifest.permission.BLUETOOTH_SCAN)
                perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                perms.add(Manifest.permission.BLUETOOTH)
                perms.add(Manifest.permission.BLUETOOTH_ADMIN)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            return perms.toTypedArray()
        }

    private var showRationale = mutableStateOf(false)
    private var pendingPermissions = mutableStateOf(emptyArray<String>())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filter { !it.value }.keys
        if (denied.isNotEmpty()) {
            Toast.makeText(
                this,
                "Some features may be limited without required permissions",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NetProbeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SurfaceDark
                ) {
                    val showRat = showRationale.value

                    if (showRat) {
                        PermissionRationaleDialog(
                            permissions = pendingPermissions.value,
                            onConfirm = {
                                showRationale.value = false
                                permissionLauncher.launch(pendingPermissions.value)
                            },
                            onDismiss = {
                                showRationale.value = false
                                Toast.makeText(
                                    this@MainActivity,
                                    "Some features may be limited",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    MainScreen()
                }
            }
        }

        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {
        val ungranted = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isEmpty()) return

        val needsRationale = ungranted.any {
            shouldShowRequestPermissionRationale(it)
        }

        if (needsRationale) {
            pendingPermissions.value = ungranted.toTypedArray()
            showRationale.value = true
        } else {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    permissions: Array<String>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCardDark,
        titleContentColor = TerminalAmber,
        textContentColor = TextSecondary,
        title = { Text("Permissions Required") },
        text = {
            Column {
                Text(
                    "NetProbe needs the following permissions for full functionality:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (permissions.any { it.contains("LOCATION") }) {
                    PermissionRow("Location", "Wi-Fi scanning, channel analysis")
                }
                if (permissions.any { it.contains("BLUETOOTH") }) {
                    PermissionRow("Bluetooth", "Device discovery, BLE scanning")
                }
                if (permissions.any { it.contains("WIFI") }) {
                    PermissionRow("Nearby Wi-Fi", "Wi-Fi network scanning")
                }
                if (permissions.any { it.contains("NOTIFICATION") }) {
                    PermissionRow("Notifications", "Battery monitor alerts")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("GRANT", color = TerminalGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("SKIP", color = TextDisabled)
            }
        }
    )
}

@Composable
private fun PermissionRow(name: String, reason: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("\u2022 ", color = TerminalAmber)
        Column {
            Text(name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(reason, color = TextDisabled, style = MaterialTheme.typography.bodySmall)
        }
    }
}
