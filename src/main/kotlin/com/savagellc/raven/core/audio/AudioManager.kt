package com.savagellc.raven.core.audio

import club.minnced.opus.util.OpusLibrary
import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.core.audio.utils.*
import com.savagellc.raven.discord.RavenVoiceWebSocket
import com.savagellc.raven.discord.VoiceServerConnection
import com.savagellc.raven.include.Channel
import com.savagellc.raven.include.PrivateChat
import com.savagellc.raven.include.ServerChannel
import com.savagellc.raven.utils.writeFile
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashMap


class AudioManager(val coreManager: CoreManager) {
    var ssrc = 0
    var port = 0
    var ip = ""
    lateinit var sock: RavenVoiceWebSocket
    lateinit var voiceServer: VoiceServerConnection
    lateinit var networkTranslate: NetworkTranslate
    lateinit var recorder: Recorder
    lateinit var playBack: PlayBack
    private val decoders = HashMap<Int, PlayBackEngine>()
    private val speaking = HashMap<Int, String>()
    val executor = Executors.newCachedThreadPool()
    private fun handleVoiceConnect() {
        OpusLibrary.loadFromJar()
        voiceServer = VoiceServerConnection(ip, port, ssrc, this)
        val ownTarget = voiceServer.handleDiscovery()
        val obj = JSONObject().put("protocol", "udp").put(
            "data",
            JSONObject().put("address", ownTarget.first).put("port", ownTarget.second).put(
                "mode",
                "xsalsa20_poly1305_lite"
            ).put(
                "codecs",
                JSONArray().put(
                    JSONObject().put("name", "opus").put("type", "audio").put(
                        "priority",
                        1000
                    ).put("payload_type", 120)
                )
            ).put("port", ownTarget.second).put("address", ownTarget.first)
        )
        sock.addEventListener("VOICE_STATE_UPDATE") {speakingJson ->
            val delay = speakingJson.getInt("delay") * 100
            if(delay == 0) {
                speaking[speakingJson.getInt("ssrc")] = if (speakingJson.getBoolean("speaking")) "true" else "false"
            } else {
                if(speaking.containsKey(speakingJson.getInt("ssrc")) && speaking[speakingJson.getInt("ssrc")] == "pending") return@addEventListener
                speaking[speakingJson.getInt("ssrc")] = "pending"
                executor.submit {
                    Thread.sleep((delay - 120).toLong())
                    speaking[speakingJson.getInt("ssrc")] = if (speakingJson.getBoolean("speaking")) "true" else "false"
                }
            }
        }
        sock.addTempEventListener("SESSION_DEP") { json ->
            try {
                val secret = json.getJSONArray("secret_key").map { (it as Int).toByte() }.toByteArray()
                networkTranslate = NetworkTranslate(secret, ssrc)
                recorder = Recorder()
                recorder.addListener { recorderAction, data ->
                    when (recorderAction) {
                        RecorderAction.START_REC -> {
                            println("sending RECORD START")
                            sock.sendMessage(5, JSONObject().put("speaking", 1).put("delay", 0).put("ssrc", ssrc))
                        }
                        RecorderAction.STOP_REC -> {
                            println("sending RECORD END")
                            sock.sendMessage(5, JSONObject().put("speaking", 0).put("delay", 0).put("ssrc", ssrc))
                        }
                        RecorderAction.PACK -> {
                            val pack = data as ByteBuffer
                            val translated = networkTranslate.preparePacket(pack)
                            voiceServer.sendPacket(translated)
                        }
                    }
                }
                playBack = PlayBack()
                recorder.start()
                playBack.start()
                voiceServer.startHeartBeat(sock.heart_beat_interval)
                voiceServer.receivePacks {
                    val translated = networkTranslate.decodePacket(it)
                    //   if(!translated.valid || !speaking.containsKey(translated.ssrc) || speaking[translated.ssrc] != "true") return@receivePacks
                    playBack.pushPacket(getEngine(translated.ssrc).decodePacket(translated.data))
                }

                println("Starting recording")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        sock.sendMessage(1, obj)
        sock.sendMessage(5, JSONObject().put("speaking", 0).put("delay", 0).put("ssrc", ssrc))
        sock.sendMessage(12, JSONObject().put("audio_ssrc", ssrc).put("video_ssrc", 0).put("rtx_ssrc", 0))
    }
    fun getEngine(ssrc:Int): PlayBackEngine {
        if(decoders.containsKey(ssrc)) return decoders[ssrc]!!
        decoders[ssrc] = PlayBackEngine(ssrc)
        return decoders[ssrc]!!
    }
    private fun handleConnect(
        voiceServerObj: JSONObject,
        voiceStateObj: JSONObject,
        channel: Channel
    ) {
        sock = RavenVoiceWebSocket(
            voiceServerObj.getString("endpoint"),
            voiceStateObj.getString("session_id"),
            voiceServerObj.getString("token"),
            coreManager.api,
            coreManager.me,
            channel
        )
        sock.addTempEventListener("READY") {
            ssrc = it.getInt("ssrc")
            println("Own SSRC: $ssrc")
            port = it.getInt("port")
            ip = it.getString("ip")

            handleVoiceConnect()
        }
        sock.connect()
        sock.handleAuthenticate()
    }

    fun loadChannel(channel: ServerChannel) {
        coreManager.api.connectToVoice(channel.id, channel.server.id) { voiceServerObj, voiceStateObj ->
            handleConnect(voiceServerObj, voiceStateObj, channel)
        }
    }

    fun loadDmChannel(channel: PrivateChat) {
        coreManager.api.connectToVoice(channel.id) { voiceServerObj, voiceStateObj ->
            handleConnect(voiceServerObj, voiceStateObj, channel)
        }
    }
}