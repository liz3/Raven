package com.savagellc.raven.core.audio.utils

import com.savagellc.raven.IOUtils
import com.savagellc.raven.TweetNaclFast
import java.nio.Buffer
import java.nio.ByteBuffer

const val MAX_UINT_32 = 4294967295
class NetworkTranslate(private val secretKey:ByteArray, val ssrc:Int) {
    private var timestamp = 0
    var seq = 0.toChar()
    var count = 0L
    private val box = TweetNaclFast.SecretBox(secretKey)
    val nonceBuf = ByteArray(24)

    fun preparePacket(rawAudio: ByteBuffer): ByteBuffer {
        seq++
        if(count >= MAX_UINT_32)
            count = 0
        else
            count++
        timestamp += AudioStatic.SAMPLES_PER_PACKET
        IOUtils.setIntBigEndian(nonceBuf, 0, count.toInt())
        val data = rawAudio.array()
        val offset = rawAudio.arrayOffset() + rawAudio.position()
        val length = rawAudio.remaining()
        val encryptedAudio = box.box(data, offset, length, nonceBuf)
        val buffer = ByteBuffer.allocate(12 + encryptedAudio.size + 4)
        buffer.put(0x80.toByte())
        buffer.put(0x78.toByte())
        buffer.putChar(seq)
        buffer.putInt(timestamp)
        buffer.putInt(ssrc)
        val dataWrapped = ByteBuffer.wrap(encryptedAudio)
        buffer.put(dataWrapped)
        (dataWrapped as Buffer ).flip()
        buffer.put(nonceBuf, 0, 4)
        (buffer as Buffer).flip()
        return buffer
    }
    fun decodePacket(rawPacket: ByteArray): ByteBuffer {
        seq++
        val buffer = ByteBuffer.wrap(rawPacket)
        val sq = buffer.getChar(2)
        val timestamp = buffer.getInt(4)
        val ssrc = buffer.getInt(8)
        val type = buffer.get(1)
        println("$ssrc : ${this.ssrc}")
        val profile = buffer.get(0)
        val data = buffer.array()
        val hasExtension = IOUtils.hasExtension(profile)
        val cc = IOUtils.getCc(profile)
        val csrcLength = cc * 4
        val extension = if(hasExtension) IOUtils.getShortBigEndian(data, 12 + csrcLength) else 0
        var offset = 12 + csrcLength
        if(hasExtension && extension == NetworkStatic.RTP_DISCORD_EXTENSION)
            offset = NetworkStatic.getPayloadOffset(data, csrcLength)

        val extendedNonce = ByteArray(24)
        System.arraycopy(rawPacket, rawPacket.size - 4, extendedNonce, 0, 4)
        val encodedAudio = ByteBuffer.allocate(data.size - offset)
        encodedAudio.put(data, offset, encodedAudio.capacity())
        (encodedAudio as Buffer).flip()
        val length = encodedAudio.remaining() - 4
        val offset2 = encodedAudio.arrayOffset() + encodedAudio.position()
        val decryptedBuff = box.open(encodedAudio.array(), offset2, length, extendedNonce)
        println(decryptedBuff.take(125).map { it }.joinToString(","))
        val decoded = ByteBuffer.wrap(decryptedBuff)
        (decoded as Buffer).flip()
        return decoded
    }
}