package com.netprobe.diagnostics.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PortDao {

    @Query("SELECT * FROM port_dictionary WHERE port = :portNumber LIMIT 1")
    suspend fun getPortInfo(portNumber: Int): PortEntity?

    @Query("SELECT * FROM port_dictionary ORDER BY port ASC")
    suspend fun getAllPorts(): List<PortEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ports: List<PortEntity>)

    @Query("SELECT COUNT(*) FROM port_dictionary")
    suspend fun getCount(): Int
}
