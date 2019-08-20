package com.savagellc.raven.core.audio

import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.core.audio.utils.NetworkTranslate
import com.savagellc.raven.core.audio.utils.Recorder
import com.savagellc.raven.core.audio.utils.RecorderAction
import com.savagellc.raven.discord.RavenVoiceWebSocket
import com.savagellc.raven.discord.VoiceServerConnection
import com.savagellc.raven.include.ServerChannel
import org.json.JSONObject
import java.nio.ByteBuffer


class AudioManager(val coreManager: CoreManager) {
    var ssrc = 0
    var port = 0
    var ip = ""
    lateinit var sock:RavenVoiceWebSocket
    lateinit var voiceServer:VoiceServerConnection
    lateinit var networkTranslate: NetworkTranslate
    lateinit var recorder: Recorder
    private fun handleVoiceConnect() {
        voiceServer = VoiceServerConnection(ip, port, ssrc, this)
        val ownTarget = voiceServer.handleDiscovery()
        val obj = JSONObject().put("protocol", "udp").put("data", JSONObject().put("address", ownTarget.first).put("port", ownTarget.second).put("mode", "xsalsa20_poly1305"))
        sock.addTempEventListener("SESSION_DEP") {json ->
          try {
              val secret = json.getJSONArray("secret_key").map { (it as Int).toByte() }.toByteArray()
              networkTranslate = NetworkTranslate(secret, ssrc)
              recorder = Recorder()
              recorder.addListener { recorderAction, data ->
                  when(recorderAction) {
                      RecorderAction.START_REC -> {
                          println("sending RECORD START")
                          sock.sendMessage(5, JSONObject().put("speaking", true).put("delay", 0).put("ssrc", ssrc))
                      }
                      RecorderAction.STOP_REC -> {
                          println("sending RECORD END")
                          sock.sendMessage(5, JSONObject().put("speaking", false).put("delay", 0).put("ssrc", ssrc))
                      }
                      RecorderAction.PACK -> {
                          println("sending PACKET")
                          val pack = data as ByteBuffer
                          val translated = networkTranslate.preparePacket(pack)
                          voiceServer.sendPacket(translated)
                      }
                  }
              }
              println("LOOOL")

              recorder.start()
              println("Starting recording")
          } catch(e:Exception) {
              e.printStackTrace()
          }
        }
        sock.sendMessage(1, obj)
    }
    private fun handleConnect(
        voiceServerObj: JSONObject,
        voiceStateObj: JSONObject,
        channel: ServerChannel
    ) {
         sock = RavenVoiceWebSocket(voiceServerObj.getString("endpoint"), voiceStateObj.getString("session_id"), voiceServerObj.getString("token"), coreManager.api, coreManager.me, channel)
        sock.addTempEventListener("READY") {
           ssrc = it.getInt("ssrc")
            port = it.getInt("port")
            ip = it.getString("ip")
            handleVoiceConnect()
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