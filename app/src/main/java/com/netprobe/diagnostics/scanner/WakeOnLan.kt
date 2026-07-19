package com.netprobe.diagnostics.scanner

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WakeOnLan {

    /**
     * Sends a Wake-on-LAN magic packet to the target MAC address.
     * Broadcasts on UDP port 9 (standard WoL port).
     *
     * Magic packet format: 6 bytes of 0xFF + target MAC repeated 16 times
     */
    fun sendMagicPacket(macAddress: String): Result<String> {
        return try {
            val macBytes = parseMac(macAddress)
                ?: return Result.failure(IllegalArgumentException("Invalid MAC: $macAddress"))

            val magicPacket = ByteArray(6 + 16 * 6)
            // First 6 bytes: 0xFF
            for (i in 0..5) magicPacket[i] = 0xFF.toByte()
            // 16 repetitions of MAC
            for (i in 0..15) {
                System.arraycopy(macBytes, 0, magicPacket, 6 + i * 6, 6)
            }

            val broadcast = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(magicPacket, magicPacket.size, broadcast, 9)

            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.send(packet)
            }

            Result.success("Magic packet sent to $macAddress")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseMac(mac: String): ByteArray? {
        val cleaned = mac.replace("[:\\-\\s]".toRegex(), "").uppercase()
        if (cleaned.length != 12) return null
        return try {
            ByteArray(6) { i ->
                cleaned.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (_: Exception) {
            null
        }
    }
}
