package com.netprobe.diagnostics.pcap

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

data class DnsServer(
    val address: String,
    val isDefault: Boolean
)

object DnsActivityReader {
    fun read(): List<DnsServer> {
        val servers = mutableListOf<DnsServer>()
        try {
            val resolvConf = File("/system/etc/resolv.conf")
            if (!resolvConf.exists()) return readFromProperties()
            BufferedReader(FileReader(resolvConf)).useLines { lines ->
                lines.filter { it.trimStart().startsWith("nameserver") }
                    .forEachIndexed { index, line ->
                        val parts = line.trim().split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            servers.add(
                                DnsServer(
                                    address = parts[1],
                                    isDefault = index == 0
                                )
                            )
                        }
                    }
            }
        } catch (_: Exception) {
            return readFromProperties()
        }
        return servers
    }

    private fun readFromProperties(): List<DnsServer> {
        val servers = mutableListOf<DnsServer>()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", "net.dns1"))
            val dns1 = process.inputStream.bufferedReader().readText().trim()
            if (dns1.isNotEmpty() && !dns1.contains("not found")) {
                servers.add(DnsServer(dns1, isDefault = true))
            }
            val process2 = Runtime.getRuntime().exec(arrayOf("getprop", "net.dns2"))
            val dns2 = process2.inputStream.bufferedReader().readText().trim()
            if (dns2.isNotEmpty() && !dns2.contains("not found")) {
                servers.add(DnsServer(dns2, isDefault = false))
            }
        } catch (_: Exception) {}
        return servers
    }
}
