package com.savagellc.raven.core.audio.utils

import tomp2p.opuswrapper.Opus
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import javax.sound.sampled.AudioSystem

class PlayBack {

    private val audioOutput = AudioSystem.getSourceDataLine(AudioStatic.playFormat)
    private val decError = IntBuffer.allocate(4)
    private val opusDecoder = Opus.INSTANCE.opus_decoder_create(AudioStatic.SAMPLE_RATE.toInt(), 2, decError)


    fun start() {
        try {
            audioOutput.open(AudioStatic.playFormat)
            audioOutput.start()

        }catch(e:Exception) {
            e.printStackTrace()
        }
    }
    fun pushPacket(data:ByteBuffer) {
        val transferredBytes = data.remainingArray()
        val pcm = ShortBuffer.allocate(AudioStatic.AUDIO_BUFFER_SIZE)
        val decoded = Opus.INSTANCE.opus_decode(
            opusDecoder, transferredBytes, transferredBytes.size,
            pcm, AudioStatic.SAMPLES_PER_PACKET, 0
        )
        println(decoded)
        pcm.position(decoded)
        pcm.flip()
        val audio = AudioStatic.pcmToAudio(pcm)
        audioOutput.write(audio, 0, audio.size)
    }
}