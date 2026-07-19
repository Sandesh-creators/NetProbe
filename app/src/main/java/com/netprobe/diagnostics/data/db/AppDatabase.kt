package com.netprobe.diagnostics.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [PortEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun portDao(): PortDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "netprobe_database"
                )
                    .addCallback(PortDictionaryCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class PortDictionaryCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database.portDao())
                }
            }
        }

        suspend fun populateDatabase(portDao: PortDao) {
            val ports = listOf(
                PortEntity(20, "FTP-Data", "TCP", "File Transfer Protocol - Data"),
                PortEntity(21, "FTP", "TCP", "File Transfer Protocol - Control"),
                PortEntity(22, "SSH", "TCP", "Secure Shell"),
                PortEntity(23, "Telnet", "TCP", "Telnet Remote Access"),
                PortEntity(25, "SMTP", "TCP", "Simple Mail Transfer Protocol"),
                PortEntity(53, "DNS", "TCP/UDP", "Domain Name System"),
                PortEntity(67, "DHCP", "UDP", "Dynamic Host Configuration Protocol - Server"),
                PortEntity(68, "DHCP", "UDP", "Dynamic Host Configuration Protocol - Client"),
                PortEntity(69, "TFTP", "UDP", "Trivial File Transfer Protocol"),
                PortEntity(80, "HTTP", "TCP", "Hypertext Transfer Protocol"),
                PortEntity(110, "POP3", "TCP", "Post Office Protocol v3"),
                PortEntity(123, "NTP", "UDP", "Network Time Protocol"),
                PortEntity(135, "MS-RPC", "TCP", "Microsoft RPC Endpoint Mapper"),
                PortEntity(137, "NetBIOS-NS", "UDP", "NetBIOS Name Service"),
                PortEntity(138, "NetBIOS-DGM", "UDP", "NetBIOS Datagram Service"),
                PortEntity(139, "NetBIOS-SSN", "TCP", "NetBIOS Session Service"),
                PortEntity(143, "IMAP", "TCP", "Internet Message Access Protocol"),
                PortEntity(161, "SNMP", "UDP", "Simple Network Management Protocol"),
                PortEntity(162, "SNMP-Trap", "UDP", "SNMP Trap"),
                PortEntity(179, "BGP", "TCP", "Border Gateway Protocol"),
                PortEntity(194, "IRC", "TCP", "Internet Relay Chat"),
                PortEntity(389, "LDAP", "TCP", "Lightweight Directory Access Protocol"),
                PortEntity(443, "HTTPS", "TCP", "HTTP Secure (TLS/SSL)"),
                PortEntity(445, "SMB", "TCP", "Server Message Block"),
                PortEntity(465, "SMTPS", "TCP", "SMTP over TLS/SSL"),
                PortEntity(514, "Syslog", "UDP", "Syslog Protocol"),
                PortEntity(515, "LPD", "TCP", "Line Printer Daemon"),
                PortEntity(554, "RTSP", "TCP", "Real Time Streaming Protocol"),
                PortEntity(587, "SMTP-Sub", "TCP", "SMTP Submission"),
                PortEntity(631, "IPP", "TCP", "Internet Printing Protocol"),
                PortEntity(636, "LDAPS", "TCP", "LDAP over TLS/SSL"),
                PortEntity(993, "IMAPS", "TCP", "IMAP over TLS/SSL"),
                PortEntity(995, "POP3S", "TCP", "POP3 over TLS/SSL"),
                PortEntity(1080, "SOCKS", "TCP", "SOCKS Proxy"),
                PortEntity(1433, "MSSQL", "TCP", "Microsoft SQL Server"),
                PortEntity(1434, "MSSQL-M", "UDP", "Microsoft SQL Server Browser"),
                PortEntity(1521, "Oracle", "TCP", "Oracle Database"),
                PortEntity(1723, "PPTP", "TCP", "Point-to-Point Tunneling Protocol"),
                PortEntity(1883, "MQTT", "TCP", "Message Queuing Telemetry Transport"),
                PortEntity(2049, "NFS", "TCP/UDP", "Network File System"),
                PortEntity(2181, "ZooKeeper", "TCP", "Apache ZooKeeper"),
                PortEntity(3306, "MySQL", "TCP", "MySQL Database"),
                PortEntity(3389, "RDP", "TCP", "Remote Desktop Protocol"),
                PortEntity(5060, "SIP", "TCP/UDP", "Session Initiation Protocol"),
                PortEntity(5432, "PostgreSQL", "TCP", "PostgreSQL Database"),
                PortEntity(5672, "AMQP", "TCP", "Advanced Message Queuing Protocol"),
                PortEntity(5900, "VNC", "TCP", "Virtual Network Computing"),
                PortEntity(6379, "Redis", "TCP", "Redis In-Memory Data Store"),
                PortEntity(6443, "K8s-API", "TCP", "Kubernetes API Server"),
                PortEntity(8080, "HTTP-Alt", "TCP", "HTTP Alternate / Proxy"),
                PortEntity(8443, "HTTPS-Alt", "TCP", "HTTPS Alternate"),
                PortEntity(8888, "HTTP-Proxy", "TCP", "HTTP Proxy / Jupyter"),
                PortEntity(9090, "Prometheus", "TCP", "Prometheus Monitoring"),
                PortEntity(9200, "Elasticsearch", "TCP", "Elasticsearch REST API"),
                PortEntity(9418, "Git", "TCP", "Git Version Control"),
                PortEntity(11211, "Memcached", "UDP", "Memcached"),
                PortEntity(27017, "MongoDB", "TCP", "MongoDB Database"),
                PortEntity(51413, "BT-Track", "TCP", "BitTorrent Tracker")
            )
            portDao.insertAll(ports)
        }
    }
}
