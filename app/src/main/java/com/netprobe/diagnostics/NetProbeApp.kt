package com.netprobe.diagnostics

import android.app.Application
import com.netprobe.diagnostics.data.db.AppDatabase

class NetProbeApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NetProbeApp
            private set
    }
}
