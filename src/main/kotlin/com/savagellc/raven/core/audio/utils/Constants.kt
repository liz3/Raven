package com.savagellc.raven.core.audio.utils

import com.codahale.xsalsa20poly1305.SecretBox
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.sql.Timestamp
import javax.sound.sampled.AudioFormat
import kotlin.math.sqrt

object AudioStatic {
    const val SAMPLE_RATE = 48000.0f
    const val SAMPLES_PER_PACKET = 960 // 20ms at 48kHz
    const val AUDIO_BUFFER_SIZE = SAMPLES_PER_PACKET * 2

    const val ACTIVATION_THRESHOLD = 20.0
    const val DEACTIVATION_DELAY = 200
    val format = AudioFormat(SAMPLE_RATE, 16, 1, true, true)

    fun ByteBuffer.remainingArray(): ByteArray {
        val array = ByteArray(remaining())
        get(array)
        return array
    }

    fun audioToPCM(rawAudioData: ByteArray): ShortBuffer {
        val pcm = ShortBuffer.allocate(rawAudioData.size / 2)
        for (i in 0 until rawAudioData.size / 2) {
            val b1 = rawAudioData[i * 2 + 1].toInt() and 0xff
            val b2 = rawAudioData[i * 2].toInt() shl 8
            pcm.put((b1 or b2).toShort())
        }
        pcm.flip()
        return pcm
    }

    fun pcmToAudio(pcm: ShortBuffer): ByteArray {
        val buffer = ByteArray(pcm.remaining() * 2)
        for (i in 0 until pcm.remaining()) {
            buffer[i * 2 + 1] = pcm[i].toByte()
            buffer[i * 2] = (pcm[i].toInt() shr 8).toByte()
        }
        return buffer
    }

    fun calculateVolume(data: ByteArray): Double {
        var sum: Long = 0
        val start = 0
        val end = data.size
        for (i in start until end) {
            sum += data[i].toLong()
        }
        val average = sum.toDouble() / end

        var sumMeanSquare = 0.0

        for (i in start until end) {
            val f = data[i] - average
            sumMeanSquare += f * f
        }
        val averageMeanSquare = sumMeanSquare / end

        return sqrt(averageMeanSquare)
    }
}

object NetworkStatic {
    const val PACKET_PADDING = 24
    val UDP_KEEP_ALIVE = byteArrayOf(0xC9.toByte(), 0, 0, 0, 0, 0, 0, 0, 0)

}