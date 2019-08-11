package com.savagellc.raven.discord

import okhttp3.Headers
import okhttp3.MediaType
import org.json.JSONArray
import org.json.JSONObject
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO
import javax.net.ssl.HttpsURLConnection
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType




data class Response(
    val code: Int,
    val hasData: Boolean,
    val respMessage: String,
    val headers: Headers,
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
    val client = OkHttpClient()


    init {
        webSocket.connect()
    }
    private fun request(
        path: String,
        contentType: String = "application/json",
        method: String = "GET",
        data: String? = null
    ): Response {
        val request = Request.Builder()
            .url("https://discordapp.com/api$path")
            .addHeader("User-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.145 Safari/537.36 Vivaldi/2.6.1566.49")
            .addHeader("Authorization", token)
            .method(method, data?.toRequestBody(contentType.toMediaType()))
            .build()
            val resp = client.newCall(request).execute()
        return Response(
            resp.code,
            resp.code == 200,
            resp.message,
            resp.headers,
            resp.body!!.string())
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
    fun editMessage(channelId: String, messageId:String, content: String): Response {
        return request("/channels/$channelId/messages/$messageId", method = "PATCH", data = JSONObject().put("content", content).toString())
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