package com.savagellc.raven.discord

import com.savagellc.raven.IOUtils
import com.savagellc.raven.core.audio.AudioManager
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.net.DatagramPacket
import java.net.InetSocketAddress


class VoiceServerConnection(
    val ip: String,
    val port: Int,
    val ssrc: Int,
    audioManager: AudioManager
) {
    private val tAddress = InetSocketAddress(ip, port)
    private lateinit var socketServer: DatagramSocket

    fun handleDiscovery(): Pair<String, Int> {
        if (this::socketServer.isInitialized)
            socketServer.close()
        socketServer = DatagramSocket()
        socketServer.send(getDiscoveryPacket())
        val recBytes = receiveDiscoveryPacket()
        val selfIp = String(recBytes, 4, recBytes.size - 6).trim().takeWhile { it.toByte() != 0.toByte() }
        val port = IOUtils.getShortLittleEndian(recBytes, recBytes.size - 2).toInt() and 0xFFFF

        return Pair(selfIp, port)
    }
    fun sendPacket(buff:ByteBuffer) {
        val data = buff.array()
        val packet = DatagramPacket(data, data.size, tAddress)
        socketServer.send(packet)
    }
    private fun getDiscoveryPacket(): DatagramPacket {
        val buff = ByteBuffer.allocate(70)
        buff.putInt(ssrc)
        return DatagramPacket(buff.array(), buff.array().size, tAddress);
    }
    private fun receiveDiscoveryPacket(): ByteArray {
        val pack = DatagramPacket(ByteArray(70), 70)
        socketServer.receive(pack)
        return pack.data
    }
}