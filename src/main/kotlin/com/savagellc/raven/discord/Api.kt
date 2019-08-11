package com.savagellc.raven.discord

import org.json.JSONArray
import org.json.JSONObject
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO
import javax.net.ssl.HttpsURLConnection


data class Response(
    val code: Int,
    val hasData: Boolean,
    val respMessage: String,
    val headers: MutableMap<String, MutableList<String>>,
    val data: String
)

object ImageCache {
    val saved = HashMap<String, BufferedImage>()
    fun getImage(url:String): BufferedImage? {
        if(saved.containsKey(url)) return saved[url]
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.doOutput = true
        connection.setRequestProperty(
            "User-agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.145 Safari/537.36 Vivaldi/2.6.1566.49"
        )
        val img = ImageIO.read(connection.inputStream)
        saved[url] = img
        return saved[url]
    }
}

class Api(private val token: String) {
    val webSocket = RavenWebSocket(token)

    init {
        webSocket.connect()
    }
    private fun request(
        path: String,
        contentType: String = "application/json",
        method: String = "GET",
        data: String? = null
    ): Response {
        val connection = URL("https://discordapp.com/api$path").openConnection() as HttpsURLConnection
        connection.requestMethod = method
        if (method != "GET") connection.setRequestProperty("Content-type", contentType)
        connection.setRequestProperty("Accept", "*/*")
        connection.addRequestProperty("Authorization", token)
        connection.setRequestProperty(
            "User-agent",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.145 Safari/537.36 Vivaldi/2.6.1566.49"
        )
        connection.doInput = true
        connection.doOutput = true
        connection.connect()
        if (data != null) {
            connection.outputStream.write(data.toByteArray())
        }
        val code = connection.responseCode
        val message = connection.responseMessage
        return Response(
            code,
            code == 200,
            message,
            connection.headerFields,
            connection.inputStream.bufferedReader().use { it.readText() })
    }

    fun getDmChannels(): JSONArray {
        val response = request("/users/@me/channels")
        return JSONArray(response.data)
    }
    fun getGuilds(): JSONArray {
        val response = request("/users/@me/guilds")
        return JSONArray(response.data)
    }
    fun getGuildChannels(id:String): JSONArray {
        val response = request("/guilds/$id/channels")
        return JSONArray(response.data)
    }

    fun getSelf(): JSONObject {
        val response = request("/users/@me")
        return JSONObject(response.data)
    }

    fun getMessages(channelId: String): JSONArray {
        val response = request("/channels/$channelId/messages")
        return JSONArray(response.data)
    }
    fun getMessages(channelId: String, before: String): JSONArray {
        val response = request("/channels/$channelId/messages?before=$before")
        return JSONArray(response.data)
    }

    fun sendSimpleMessage(channelId: String, message: String): Response {
        val obj = JSONObject()
        obj.put("content", message)
        obj.put("tts", false)
        obj.put("none", "${System.currentTimeMillis()}")
        return request("/channels/$channelId/messages", method = "POST", data = obj.toString())
    }
}