package com.netprobe.diagnostics.pcap

import java.io.BufferedReader
import java.io.FileReader

data class ArpEntry(
    val ipAddress: String,
    val macAddress: String,
    val interface_: String
)

object ArpTableReader {
    fun read(): List<ArpEntry> {
        val entries = mutableListOf<ArpEntry>()
        try {
            BufferedReader(FileReader("/proc/net/arp")).useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        entries.add(
                            ArpEntry(
                                ipAddress = parts[0],
                                macAddress = parts[3],
                                interface_ = parts[5]
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return entries
    }
}
