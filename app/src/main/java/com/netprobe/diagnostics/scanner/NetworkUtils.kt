package com.netprobe.diagnostics.scanner

import java.net.InetAddress

object NetworkUtils {

    fun resolveHostname(ip: String): String? {
        return try {
            val addr = InetAddress.getByName(ip)
            val name = addr.canonicalHostName
            if (name != ip) name else null
        } catch (_: Exception) {
            null
        }
    }

    fun formatSshCommand(host: String, port: Int = 22, username: String = ""): String {
        val user = if (username.isNotEmpty()) "$username@$host" else host
        return if (port == 22) "ssh $user" else "ssh -p $port $user"
    }
}
