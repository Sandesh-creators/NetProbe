package com.netprobe.diagnostics.data.oui

import android.content.Context
import android.util.LruCache
import com.netprobe.diagnostics.data.model.OuiResult

object OuiLookup {

    private var database: OuiDatabase? = null
    private val cache = object : LruCache<String, OuiResult?>(500) {
        override fun sizeOf(key: String, value: OuiResult?) = 1
    }

    fun init(context: Context) {
        if (database != null) return
        database = OuiDatabase(context).also { it.open() }
    }

    fun lookup(mac: String): OuiResult? {
        val cleaned = mac.replace("[:\\-\\s]".toRegex(), "").uppercase()
        if (cleaned.length < 6) return null
        val prefix = "${cleaned.substring(0, 2)}:${cleaned.substring(2, 4)}:${cleaned.substring(4, 6)}"

        cache.get(prefix)?.let { return it }

        val result = database?.lookupPartial(mac)
        cache.put(prefix, result)
        return result
    }

    fun getVendorName(mac: String): String? {
        return lookup(mac)?.vendor
    }

    fun isAvailable(): Boolean = database?.isAvailable() == true

    fun getCount(): Int = database?.getCount() ?: 0

    fun shutdown() {
        database?.close()
        database = null
        cache.evictAll()
    }
}
