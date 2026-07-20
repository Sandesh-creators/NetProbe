package com.netprobe.diagnostics.pcap

import android.content.Context
import androidx.core.content.FileProvider
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object PcapExporter {

    private const val PCAP_MAGIC = 0xA1B2C3D4
    private const val PCAP_VERSION_MAJOR = 2
    private const val PCAP_VERSION_MINOR = 4
    private const val LINKTYPE_ETHERNET = 1
    private const val ETHERTYPE_ARP: Short = 0x0806

    fun exportToFile(context: Context, filename: String): File {
        val arpEntries = ArpTableReader.read()
        val dnsServers = DnsActivityReader.read()

        val outputDir = File(context.cacheDir, "exports")
        outputDir.mkdirs()
        val file = File(outputDir, filename)

        DataOutputStream(FileOutputStream(file)).use { dos ->
            writePcapHeader(dos)
            arpEntries.forEach { entry ->
                writeArpPacket(dos, entry, 0)
            }
            dnsServers.forEachIndexed { index, server ->
                writeDnsCommentPacket(dos, server, (arpEntries.size + index).toLong())
            }
        }
        return file
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/vnd.tcpdump.pcap"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "NetProbe ARP + DNS Capture")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share PCAP"))
    }

    private fun writePcapHeader(dos: DataOutputStream) {
        dos.writeInt(PCAP_MAGIC.toInt())
        dos.writeShort(PCAP_VERSION_MAJOR)
        dos.writeShort(PCAP_VERSION_MINOR)
        dos.writeInt(0)
        dos.writeInt(0)
        dos.writeInt(65535)
        dos.writeInt(LINKTYPE_ETHERNET)
    }

    private fun writeArpPacket(dos: DataOutputStream, entry: ArpEntry, timestamp: Long) {
        val payload = buildArpPayload(entry.ipAddress, entry.macAddress)
        val ethernetFrame = ByteArray(14 + payload.size)
        val dstMac = parseMac(entry.macAddress) ?: ByteArray(6) { 0xFF.toByte() }
        System.arraycopy(dstMac, 0, ethernetFrame, 0, 6)
        val srcMac = ByteArray(6) { 0xAA.toByte() }
        System.arraycopy(srcMac, 0, ethernetFrame, 6, 6)
        val typeBuffer = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
        typeBuffer.putShort(ETHERTYPE_ARP)
        System.arraycopy(typeBuffer.array(), 0, ethernetFrame, 12, 2)
        System.arraycopy(payload, 0, ethernetFrame, 14, payload.size)

        writePacket(dos, timestamp, ethernetFrame)
    }

    private fun writeDnsCommentPacket(dos: DataOutputStream, server: DnsServer, index: Long) {
        val comment = "DNS: ${server.address}${if (server.isDefault) " (default)" else ""}"
        val data = comment.toByteArray(Charsets.US_ASCII)
        val payload = ByteArray(data.size + 1)
        System.arraycopy(data, 0, payload, 0, data.size)
        payload[data.size] = 0

        val ethernetFrame = ByteArray(14 + payload.size)
        ethernetFrame[12] = 0x08
        ethernetFrame[13] = 0x00
        System.arraycopy(payload, 0, ethernetFrame, 14, payload.size)

        writePacket(dos, index * 1000, ethernetFrame)
    }

    private fun writePacket(dos: DataOutputStream, timestamp: Long, frame: ByteArray) {
        val tsSec = (timestamp / 1000).toInt()
        val tsUsec = ((timestamp % 1000) * 1000).toInt()
        dos.writeInt(tsSec)
        dos.writeInt(tsUsec)
        dos.writeInt(frame.size)
        dos.writeInt(frame.size)
        dos.write(frame)
    }

    private fun buildArpPayload(ipAddress: String, macAddress: String): ByteArray {
        val buf = ByteBuffer.allocate(28).order(ByteOrder.BIG_ENDIAN)
        buf.putShort(1)
        buf.putShort(0x0800)
        buf.put(6)
        buf.put(4)
        buf.putShort(2)
        val srcMac = parseMac(macAddress) ?: ByteArray(6)
        buf.put(srcMac)
        val srcIp = parseIp(ipAddress)
        buf.put(srcIp)
        buf.put(ByteArray(6))
        val zeroIp = ByteArray(4)
        buf.put(zeroIp)
        return buf.array()
    }

    private fun parseMac(mac: String): ByteArray? {
        val parts = mac.replace("[:-]", "").replace(" ", "")
            .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return if (parts.size == 6) parts else null
    }

    private fun parseIp(ip: String): ByteArray {
        val parts = ip.split(".")
        return ByteArray(4) { parts.getOrElse(it) { "0" }.toByte() }
    }
}
