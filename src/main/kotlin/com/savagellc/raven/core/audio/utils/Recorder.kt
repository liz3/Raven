package com.savagellc.raven.core.audio.utils

import club.minnced.opus.util.OpusLibrary
import tomp2p.opuswrapper.Opus
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.*
import javax.sound.sampled.AudioSystem

enum class RecorderAction {
    START_REC,
    PACK,
    STOP_REC
}

class Recorder {
    private val listeners = Vector<(RecorderAction, Any?) -> Unit>()
    private var recording = false
    private var lastAudioOutput = 0L
    var connected = true

    fun addListener(listener: (RecorderAction, Any?) -> Unit) {
        listeners.add(listener)
    }
    private fun emitStartRec() {
        recording = true
        listeners.forEach {
            it(RecorderAction.START_REC, null)
        }
    }
    private fun emitStopRec() {
        recording = false
        listeners.forEach {
            it(RecorderAction.STOP_REC, null)
        }
    }
    private fun emitPack(buff:ByteBuffer) {
        listeners.forEach {
            it(RecorderAction.PACK, buff)
        }
    }
    fun start() {
        OpusLibrary.loadFromJar()
        val audioInput = AudioSystem.getTargetDataLine(AudioStatic.format)
        val encError = IntBuffer.allocate(4)
        val opusEncoder =
            Opus.INSTANCE.opus_encoder_create(AudioStatic.SAMPLE_RATE.toInt(), 1, Opus.OPUS_APPLICATION_VOIP, encError)

        val t = Thread {
            audioInput.open(AudioStatic.format)
            audioInput.start()
            while (connected && audioInput.isOpen) {
                var rawAudioData = ByteArray(AudioStatic.AUDIO_BUFFER_SIZE)
                audioInput.read(rawAudioData, 0, rawAudioData.size)
                val calculatedVolume = AudioStatic.calculateVolume(rawAudioData)

                if (calculatedVolume < AudioStatic.ACTIVATION_THRESHOLD) {
                    if (System.currentTimeMillis() - lastAudioOutput > AudioStatic.DEACTIVATION_DELAY) {
                        if(recording) emitStopRec()
                        continue
                    }
                } else {
                    lastAudioOutput = System.currentTimeMillis()
                    if(!recording) {
                        emitStartRec()
                        continue
                    }
                }

                val pcm = AudioStatic.audioToPCM(rawAudioData)
                val voiceBuffer = ByteBuffer.allocate( 1024)
                val read =
                    Opus.INSTANCE.opus_encode(opusEncoder, pcm, AudioStatic.SAMPLES_PER_PACKET, voiceBuffer, voiceBuffer.remaining())
                voiceBuffer.position(read)
                voiceBuffer.flip()
                emitPack(voiceBuffer)
            }
        }
        t.name = "Audio recorder thread"
        t.start()
    }
}