package com.savagellc.raven.core.audio

import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.discord.RavenVoiceWebSocket
import com.savagellc.raven.include.ServerChannel
import org.json.JSONObject
import java.nio.IntBuffer



private val SAMPLE_RATE = 48000
private val FRAME_SIZE = 480
class AudioManager(val coreManager: CoreManager) {

    init {

    }
    private fun handleConnect(
        voiceServerObj: JSONObject,
        voiceStateObj: JSONObject,
        channel: ServerChannel
    ) {

        println("called")
        val sock = RavenVoiceWebSocket(voiceServerObj.getString("endpoint"), voiceStateObj.getString("session_id"), voiceServerObj.getString("token"), coreManager.api, coreManager.me, channel)
        sock.addTempEventListener("READY") {
            println(it)
        }
       sock.connect()
        sock.handleAuthenticate()
    }
    fun loadChannel(channel:ServerChannel) {
        coreManager.api.connectToVoice(channel.id, channel.server.id) { voiceServerObj, voiceStateObj ->
           handleConnect(voiceServerObj, voiceStateObj, channel)
        }
    }
}