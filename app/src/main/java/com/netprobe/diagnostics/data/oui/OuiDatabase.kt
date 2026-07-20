package com.netprobe.diagnostics.data.oui

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.netprobe.diagnostics.data.model.OuiResult
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

class OuiDatabase(private val context: Context) {

    private var database: SQLiteDatabase? = null

    fun open() {
        val dbFile = context.getDatabasePath("oui.db")
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            try {
                context.assets.open("oui.db").use { gzipStream ->
                    GZIPInputStream(gzipStream).use { decompressed ->
                        FileOutputStream(dbFile).use { output ->
                            decompressed.copyTo(output)
                        }
                    }
                }
            } catch (_: Exception) {
                return
            }
        }

        try {
            database = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )
        } catch (_: Exception) {
        }
    }

    fun lookupPartial(mac: String): OuiResult? {
        val cleaned = mac.replace("[:\\-\\s]".toRegex(), "").uppercase()
        if (cleaned.length < 6) return null
        val prefix = "${cleaned.substring(0, 2)}:${cleaned.substring(2, 4)}:${cleaned.substring(4, 6)}"

        return try {
            database?.rawQuery(
                "SELECT prefix, vendor, is_private FROM oui WHERE prefix = ? LIMIT 1",
                arrayOf(prefix)
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    OuiResult(
                        prefix = cursor.getString(0),
                        vendor = cursor.getString(1),
                        isPrivate = cursor.getInt(2) == 1
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getCount(): Int {
        return try {
            database?.rawQuery("SELECT COUNT(*) FROM oui", null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun isAvailable(): Boolean = database != null

    fun close() {
        database?.close()
        database = null
    }
}
