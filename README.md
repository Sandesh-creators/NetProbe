# NetProbe

A network diagnostics toolkit for Android. Scans LAN devices, discovers Bluetooth peripherals, analyzes Wi-Fi channels, maps your network topology, and more — all from a single app with a dark terminal-style UI.

**Min SDK:** 26 (Android 8.0) · **Target SDK:** 35 (Android 15) · **Version:** 3.0.0

---

## Features

### LAN Scan
Scans the local subnet for active devices using ARP requests. Displays hostname, IP, MAC address, vendor (via built-in OUI database), and vendor logo. Supports Wake-on-LAN to wake sleeping devices remotely.

### Bluetooth Explorer
Scans for BLE and classic Bluetooth devices. Shows device name, MAC address, signal strength (RSSI), bond state, UUIDs, TX power, and vendor. Includes a foreground-service battery monitor that connects to paired BLE devices and reads their battery level in real time.

### Channel Map
Analyzes nearby Wi-Fi networks and plots them on a frequency/channel graph. Shows SSID, BSSID, frequency, signal strength, and band (2.4 GHz / 5 GHz / 6 GHz). Helps identify channel congestion and optimal placement.

### Network Info
Displays current connection details: local IP, gateway, subnet mask, DNS servers, and public IP. Shows Wi-Fi SSID, BSSID, link speed, and frequency.

### Tools Tab
Contains advanced diagnostic sub-tools:

#### Passive Asset Discovery
Runs a background service that periodically scans ARP tables and performs short BLE scans to discover devices on the network. Flags unknown/new devices and persists a history of discovered assets to a local database.

#### P2P Signal Heatmap
Collects Wi-Fi and BLE RSSI readings over time and renders a color-coded grid heatmap. Includes a time-series chart for tracking signal fluctuations. Configurable sampling interval.

#### Network Topology Map
Builds a force-directed graph of your network: gateway as the central node, connected devices as surrounding nodes, edges weighted by signal strength or latency. Tap any node for detailed info.

#### Offline OUI Vendor Lookup
A built-in SQLite database of ~1,750 MAC address prefixes (OUI entries) identifies hardware vendors from MAC addresses without an internet connection. An LRU cache keeps lookups fast.

---

## Permissions

NetProbe requests the following permissions at runtime:

| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | Wi-Fi SSID/frequency scanning (required on Android 10+) |
| `BLUETOOTH_SCAN` | BLE device scanning (Android 12+) |
| `BLUETOOTH_CONNECT` | Access paired device info, read battery level (Android 12+) |
| `NEARBY_WIFI_DEVICES` | Wi-Fi scanning (Android 13+) |
| `POST_NOTIFICATIONS` | Battery monitor foreground service notification (Android 13+) |
| `FOREGROUND_SERVICE` | Background asset discovery and battery monitoring |

No data is sent off-device. All scans and lookups happen locally.

---

## Installation

### From GitHub Releases

1. Go to [Releases](https://github.com/Sandesh-creators/NetProbe/releases)
2. Download `app-debug.apk` from the latest release
3. Open the APK on your Android device and allow installation from unknown sources

### Building from Source

```bash
git clone https://github.com/Sandesh-creators/NetProbe.git
cd NetProbe

# Requires Android SDK (compileSdk 35, build-tools 34.0.0+)
export JAVA_HOME=/path/to/jdk-21
export KEYSTORE_PASS=your_keystore_pass

./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM with AndroidViewModel + StateFlow
- **Database:** Room (asset history, OUI cache)
- **Background:** Foreground services for asset discovery and battery monitoring
- **Build:** Gradle 8.11.1, AGP 8.7.0, Kotlin 2.1.0, Compose BOM 2024.12.01

---

## Bug Reports

Found a bug or have a feature request? Email us at:

**[sandeshalt111@gmail.com](mailto:sandeshalt111@gmail.com)**

When reporting a bug, please include:
- Device model and Android version
- Steps to reproduce
- What you expected vs. what happened
- Screenshots or screen recordings if applicable

---

## License

This project is open source. See the repository for license details.
