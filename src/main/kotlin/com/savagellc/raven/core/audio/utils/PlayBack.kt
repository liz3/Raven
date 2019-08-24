package com.savagellc.raven.core.audio.utils

import com.savagellc.raven.IOUtils
import com.savagellc.raven.utils.writeFile
import tomp2p.opuswrapper.Opus
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import javax.sound.sampled.AudioSystem

class PlayBackEngine(val ssrc:Int) {

    private val decError = IntBuffer.allocate(1)
    private val opusDecoder = Opus.INSTANCE.opus_decoder_create(AudioStatic.SAMPLE_RATE.toInt(), 1, decError)

    fun decodePacket(encodedAudio: ByteBuffer): ByteArray {

        val length = encodedAudio.remaining()
        val offset = encodedAudio.arrayOffset() + encodedAudio.position()
        val decoded = ShortBuffer.allocate(4096)
        val buf = ByteArray(length)
        val data = encodedAudio.array()
        System.arraycopy(data, offset, buf, 0, length)
        val result = Opus.INSTANCE.opus_decode(
            opusDecoder, buf, buf.size,
            decoded, AudioStatic.SAMPLES_PER_PACKET, 0
        )
        if(result < 0) return ByteArray(0)
        val audio = ShortArray(result * 2)
        decoded.get(audio)
        return IOUtils.getAudioData(audio, 1.0)

//        val decoded = ShortBuffer.allocate(AudioStatic.AUDIO_BUFFER_SIZE)
//        val transferredBytes = encodedAudio.remainingArray()
//        val result = Opus.INSTANCE.opus_decode(
//            opusDecoder, transferredBytes, transferredBytes.size,
//            decoded, AudioStatic.SAMPLES_PER_PACKET, 0
//        )
//       if(result < 0) return ByteArray(0)
//        decoded.position(result)
//        decoded.flip()
//
//        return AudioStatic.pcmToAudio(decoded)
    }
}

class PlayBack {

    private val audioOutput = AudioSystem.getSourceDataLine(AudioStatic.playFormat)

    fun start() {
        try {
            audioOutput.open(AudioStatic.playFormat)
            audioOutput.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pushPacket(audio: ByteArray) {
       // writeFile(audio, "out2.wav", true, true)

         audioOutput.write(audio, 0, audio.size)
    }
}