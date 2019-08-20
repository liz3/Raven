package com.savagellc.raven.discord

import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import com.savagellc.raven.IntUtils
import com.savagellc.raven.include.Channel
import com.savagellc.raven.include.Me
import com.savagellc.raven.include.ServerChannel
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.InflaterOutputStream
import java.util.zip.Inflater
import java.util.*

const val ZLIB_SUFFIX = 0x0000FFFF

enum class OpCode(val num: Int) {
    DISPATCH(0),
    HEART_BEAT(1),
    IDENTIFY(2),
    STATUS_UPDATE(3),
    VOICE_STATE_UPDATE(4),
    RESUME(6),
    RECONNECT(7),
    REQUEST_GUILD_MEMBERS(8),
    INVALID_SESSION(9),
    HELLO(10),
    HEARTBEAT_ACK(11),
    CHANNEL_SWITCH(13)
}

class RavenWebSocket(val token: String, val api: Api) {
    lateinit var websocket: WebSocket
    val zlibContext = Inflater()
    private val eventListeners = Vector<Pair<String, (JSONObject) -> Unit>>()
    private val tempListener = Vector<Pair<String, (JSONObject) -> Unit>>()
    private var heart_beat_interval = 0L
    var disposed = false
    var requests = 0
    var readBuffer: ByteArrayOutputStream? = null
    var heartbeatThread = Thread {
        while (heart_beat_interval != 0L && !disposed) {
            sendMessage(OpCode.HEART_BEAT, requests)
            try {
                Thread.sleep(heart_beat_interval)
            } catch (e: Exception) {
                break
            }
        }
    }

    fun addEventListener(name: String, cb: (JSONObject) -> Unit) {
        eventListeners.add(Pair(name, cb))
    }

    fun addTempEventListener(name: String, cb: (JSONObject) -> Unit) {
        tempListener.add(Pair(name, cb))
    }

    fun clearEventListeners() {
        eventListeners.clear()
    }

    fun sendMessage(code: OpCode, data: Any?) {
        val obj = JSONObject()
        obj.put("op", code.num)
        obj.put("d", data)
        if (api.hasDebugger) api.debugger.pushSockUp(code, data)


        websocket.sendText(obj.toString())

    }

    private fun allocateBuffer(binary: ByteArray) {
        this.readBuffer = ByteArrayOutputStream(binary.size * 2)
        this.readBuffer!!.write(binary)
    }

    private fun extendBuffer(binary: ByteArray) {
        if (this.readBuffer != null)
            this.readBuffer!!.write(binary)
    }

    fun onBufferMessage(binary: ByteArray): Boolean {
        if (binary.size >= 4 && IntUtils.getInt(binary, binary.size - 4) == ZLIB_SUFFIX) {
            extendBuffer(binary)
            return true
        }

        if (readBuffer != null)
            extendBuffer(binary)
        else
            allocateBuffer(binary)

        return false
    }

    fun connect() {

        websocket = WebSocketFactory().createSocket("wss://gateway.discord.gg/?v=6&encoding=json&compress=zlib-stream")
        websocket.addListener(object : WebSocketAdapter() {
            @Throws(Exception::class)
            override fun onTextMessage(websocket: WebSocket, message: String) {
                onMessage(JSONObject(message))
            }

            override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray) {
                if (!onBufferMessage(binary)) return

                val decompressedBuffer = ByteArrayOutputStream()
                val outStream = InflaterOutputStream(decompressedBuffer, zlibContext)
                if (readBuffer != null)
                    readBuffer!!.writeTo(outStream)
                else
                    outStream.write(binary)
                onMessage(JSONObject(decompressedBuffer.toString("UTF-8")))
                readBuffer = null
            }

            override fun onConnected(
                websocket: WebSocket,
                headers: MutableMap<String, MutableList<String>>?
            ) {

            }
        })

        websocket.connect()
    }

    private fun handleAuthenticate(message: JSONObject) {
        heart_beat_interval = message.getLong("heartbeat_interval")
        heartbeatThread.start()
        val obj = JSONObject()
        obj.put("token", token)
            .put("compress", true)
            .put("v", 6)
            .put("large_threshold", 250)
        val props = JSONObject()
        props.put("os", System.getProperty("os.name"))
        props.put("browser", "Raven")
        props.put("device", "Raven")
        val presence = JSONObject()
        presence.put("activities", JSONArray()).put("afk", false).put("since", 0).put("status", "online")
        obj.put("properties", props).put("presence", presence)
        sendMessage(OpCode.IDENTIFY, obj)
    }

    fun onMessage(raw: JSONObject) {
        requests++
        val code = raw.getInt("op")
        if (api.hasDebugger) api.debugger.pushSockDown(code, raw)
        when (code) {
            OpCode.HEARTBEAT_ACK.num -> {
            }
            OpCode.DISPATCH.num -> {
                val message = raw.getJSONObject("d")
                val type = raw.getString("t")
                eventListeners.forEach {
                    if (it.first == type) it.second(message)
                }
                val toRemove = Vector<Pair<String, (JSONObject) -> Unit>>()
                tempListener.filter { it.first == type }.forEach {
                    it.second(message)
                    toRemove.add(it)
                }
                toRemove.forEach {
                    tempListener.remove(it)
                }
            }
            OpCode.HELLO.num -> {
                val message = raw.getJSONObject("d")
                handleAuthenticate(message)
            }
        }
    }
}

class RavenVoiceWebSocket(
    val targetUrl: String,
    val sessionId: String,
    val token: String,
    val api: Api,
    val me: Me,
    val channel: Channel
) {
    lateinit var websocket: WebSocket
    val zlibContext = Inflater()
    private val eventListeners = Vector<Pair<String, (JSONObject) -> Unit>>()
    private val tempListener = Vector<Pair<String, (JSONObject) -> Unit>>()
     var heart_beat_interval = 0L
    var disposed = false
    var requests = 0
    var readBuffer: ByteArrayOutputStream? = null
    var heartbeatThread = Thread {
        while (heart_beat_interval != 0L && !disposed) {
            sendMessage(1, System.currentTimeMillis())
            try {
                Thread.sleep(heart_beat_interval)
            } catch (e: Exception) {
                break
            }
        }
    }

    fun addEventListener(name: String, cb: (JSONObject) -> Unit) {
        eventListeners.add(Pair(name, cb))
    }

    fun addTempEventListener(name: String, cb: (JSONObject) -> Unit) {
        tempListener.add(Pair(name, cb))
    }

    fun clearEventListeners() {
        eventListeners.clear()
    }

    fun sendMessage(code: Int, data: Any?) {
        val obj = JSONObject()
        obj.put("op", code)
        obj.put("d", data)
        //   if (api.hasDebugger) api.debugger.pushSockUp(code, data)
        websocket.sendText(obj.toString())

    }

    private fun allocateBuffer(binary: ByteArray) {
        this.readBuffer = ByteArrayOutputStream(binary.size * 2)
        this.readBuffer!!.write(binary)
    }

    private fun extendBuffer(binary: ByteArray) {
        if (this.readBuffer != null)
            this.readBuffer!!.write(binary)
    }

    fun onBufferMessage(binary: ByteArray): Boolean {
        if (binary.size >= 4 && IntUtils.getInt(binary, binary.size - 4) == ZLIB_SUFFIX) {
            extendBuffer(binary)
            return true
        }

        if (readBuffer != null)
            extendBuffer(binary)
        else
            allocateBuffer(binary)

        return false
    }

    fun connect() {

        websocket = WebSocketFactory().createSocket("ws://$targetUrl/")
        websocket.addListener(object : WebSocketAdapter() {
            @Throws(Exception::class)
            override fun onTextMessage(websocket: WebSocket, message: String) {
                onMessage(JSONObject(message))
            }

            override fun onBinaryMessage(websocket: WebSocket?, binary: ByteArray) {
                if (!onBufferMessage(binary)) return

                val decompressedBuffer = ByteArrayOutputStream()
                val outStream = InflaterOutputStream(decompressedBuffer, zlibContext)
                if (readBuffer != null)
                    readBuffer!!.writeTo(outStream)
                else
                    outStream.write(binary)
                onMessage(JSONObject(decompressedBuffer.toString("UTF-8")))
                readBuffer = null
            }

            override fun onConnected(
                websocket: WebSocket,
                headers: MutableMap<String, MutableList<String>>?
            ) {

            }
        })

        websocket.connect()
    }

    fun handleAuthenticate() {

        val obj = JSONObject()
        if (channel is ServerChannel) {
            obj.put("token", token)
                .put("server_id", channel.server.id)
                .put("user_id", me.id)
                .put("session_id", sessionId)
        } else {
            obj.put("token", token)
                .put("user_id", me.id)
                .put("session_id", sessionId)
        }
        sendMessage(0, obj)
    }

    fun onMessage(raw: JSONObject) {
        println(raw)
        requests++
        val code = raw.getInt("op")
        if (api.hasDebugger) api.debugger.pushSockDown(code, raw)
        when (code) {
            2, 4 -> {
                val message = raw.getJSONObject("d")
                val type = if (code == 2) "READY" else if (code == 4) "SESSION_DEP" else "GENERIC"

                eventListeners.forEach {
                    if (it.first == type) it.second(message)
                }
                val toRemove = Vector<Pair<String, (JSONObject) -> Unit>>()
                tempListener.filter { it.first == type }.forEach {
                    it.second(message)
                    toRemove.add(it)
                }
                toRemove.forEach {
                    tempListener.remove(it)
                }
            }
            8 -> {
                val message = raw.getJSONObject("d")
                heart_beat_interval = (message.getLong("heartbeat_interval") * .75).toLong()
                heartbeatThread.start()
            }
        }
    }
}
