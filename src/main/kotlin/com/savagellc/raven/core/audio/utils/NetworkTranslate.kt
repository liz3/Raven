package com.savagellc.raven.core.audio.utils

import com.codahale.xsalsa20poly1305.SecretBox
import java.nio.ByteBuffer

class NetworkTranslate(private val secretKey:ByteArray, val ssrc:Int) {
    private var timestamp = 0
    var seq = 0;
    private val box = SecretBox(secretKey)

    fun preparePacket(buffer: ByteBuffer): ByteBuffer {
        timestamp += AudioStatic.SAMPLES_PER_PACKET
        val nonceBuff = ByteBuffer.allocate(NetworkStatic.PACKET_PADDING)
        nonceBuff.put(0x80.toByte())
        nonceBuff.put(0x78.toByte())
        nonceBuff.putChar(seq.toChar())
        nonceBuff.putInt(timestamp)
        nonceBuff.putInt(ssrc)
        val audioBuff = box.seal(nonceBuff.array(), buffer.array())
        val wrapped = ByteBuffer.wrap(audioBuff)
        val fBuff = ByteBuffer.allocate(12 + audioBuff.size)
        fBuff.put(0x80.toByte())
        fBuff.put(0x78.toByte())
        fBuff.putChar(seq.toChar())
        fBuff.putInt(timestamp)
        fBuff.putInt(ssrc)
        fBuff.put(wrapped)
        wrapped.flip()
        return fBuff
    }
    fun decodePacket(buffer: ByteBuffer): ByteBuffer {
        val nonceBuff = ByteBuffer.allocate(NetworkStatic.PACKET_PADDING)
        nonceBuff.put(0x80.toByte()) // type
        nonceBuff.put(0x78.toByte()) // version
        nonceBuff.putChar(buffer.getChar(2)) // sequence
        nonceBuff.putInt(buffer.getInt(4)) // timestamp
        nonceBuff.putInt(buffer.getInt(8)) // SSRC
        val encodedBuff = ByteArray(buffer.array().size - 12)
        buffer.get(encodedBuff, 12, encodedBuff.size)
        val audioBuff = box.open(nonceBuff.array(), encodedBuff)
        return ByteBuffer.wrap(audioBuff.get())
    }
}