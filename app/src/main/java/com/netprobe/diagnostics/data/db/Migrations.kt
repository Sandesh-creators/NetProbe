package com.netprobe.diagnostics.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `rssi_samples` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sourceType` TEXT NOT NULL,
                `sourceAddress` TEXT NOT NULL,
                `sourceName` TEXT,
                `rssi` INTEGER NOT NULL,
                `frequency` INTEGER,
                `timestamp` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rssi_samples_sourceType` ON `rssi_samples` (`sourceType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rssi_samples_sourceAddress` ON `rssi_samples` (`sourceAddress`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rssi_samples_timestamp` ON `rssi_samples` (`timestamp`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `discovered_assets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `macAddress` TEXT NOT NULL,
                `ipAddress` TEXT,
                `hostname` TEXT,
                `deviceName` TEXT,
                `source` TEXT NOT NULL,
                `deviceType` TEXT NOT NULL,
                `firstSeen` INTEGER NOT NULL,
                `lastSeen` INTEGER NOT NULL,
                `isKnown` INTEGER NOT NULL DEFAULT 0,
                `isFlagged` INTEGER NOT NULL DEFAULT 0,
                `ouiVendor` TEXT,
                `notes` TEXT
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_discovered_assets_macAddress` ON `discovered_assets` (`macAddress`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_discovered_assets_isKnown` ON `discovered_assets` (`isKnown`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_discovered_assets_isFlagged` ON `discovered_assets` (`isFlagged`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `asset_events` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `assetId` INTEGER NOT NULL,
                `eventType` TEXT NOT NULL,
                `ipAddress` TEXT,
                `rssi` INTEGER,
                `eventTime` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_events_assetId` ON `asset_events` (`assetId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_asset_events_eventTime` ON `asset_events` (`eventTime`)")
    }
}
