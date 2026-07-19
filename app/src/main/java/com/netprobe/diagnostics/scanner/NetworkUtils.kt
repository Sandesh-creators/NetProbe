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

    /**
     * Generates a Termux SSH intent URI for connecting to a host.
     * Returns the intent URI string, or null if Termux isn't installed.
     */
    fun termuxSshUri(host: String, port: Int = 22, username: String = "root"): String {
        return "ssh://$username@$host:$port"
    }
}
