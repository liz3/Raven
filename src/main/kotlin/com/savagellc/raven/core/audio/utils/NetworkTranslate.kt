package com.savagellc.raven.core.audio.utils

import com.codahale.xsalsa20poly1305.SecretBox
import com.savagellc.raven.IOUtils
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
        val data = buffer.array()
        val profile = buffer.get(0)
        val hasExtension = IOUtils.hasExtension(profile)
        val cc = IOUtils.getCc(profile)
        val csrcLength = cc * 4
        val extension = if(hasExtension) IOUtils.getShortBigEndian(data, 12 + csrcLength) else 0
        var offset = 12 + csrcLength
        if(hasExtension && extension == NetworkStatic.RTP_DISCORD_EXTENSION)
            offset = NetworkStatic.getPayloadOffset(data, csrcLength)
        val encodedAudio = ByteBuffer.allocate(data.size - offset)
        encodedAudio.put(data, offset, encodedAudio.capacity())
        encodedAudio.flip()
        val nonceBuff = NetworkStatic.getNoncePadded(data)
        val decoded = ByteBuffer.wrap(box.open(nonceBuff, encodedAudio.array()).get())
        decoded.flip()
        return decoded
    }
}