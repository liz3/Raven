package com.savagellc.raven.discord

import com.savagellc.raven.gui.EventLogger
import okhttp3.Headers
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

const val USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) discord/0.0.9 Chrome/69.0.3497.128 Electron/4.0.8 Safari/537.36" // UA of Discord 0.0.9 on Linux

enum class RelationStatus(val n: Int) {
    FRIENDS(1),
    INCOMING(3),
    OUTGOING(4)
}

enum class ChannelType(val num: Int) {
    GUILD_TEXT(0),
    DM(1),
    GUILD_VOICE(2),
    GROUP_DM(3),
    GUILD_CATEGORY(4),
    GUILD_NEWS(5),
    GUILD_STORE(6)
}

//277767292404891648
enum class OnlineStatus(val value: String, val fName: String) {
    IDLE("idle", "Idle"),
    DO_NOT_DISTURB("dnd", "Do not disturb"),
    OFFLINE("invisible", "Invisible"),
    ONLINE("online", "Online");

    override fun toString(): String {
        return fName
    }
}

data class Response(
    val code: Int,
    val hasData: Boolean,
    val respMessage: String,
    val headers: Headers,
    val data: String
)
fun computeAvatarImagePath(obj:JSONObject): String {
return "https://cdn.discordapp.com/avatars/${obj.getString("id")}/${if(obj.has("avatar")) obj.getString("avatar") else ""}"
}
object ImageCache {
    private val saved = HashMap<String, BufferedImage>()
    fun getImage(url: String): BufferedImage? {
        if (saved.containsKey(url)) return saved[url]
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.doOutput = true
        connection.setRequestProperty(
            "User-Agent",
            USER_AGENT
        )
        val img = ImageIO.read(connection.inputStream)
        saved[url] = img
        return saved[url]
    }

    fun disposeCache() {
        saved.clear()
    }
}

class Api(private val token: String, holdConnect: Boolean = false) {
    val webSocket = RavenWebSocket(token, this)
    val client = OkHttpClient()
    lateinit var debugger: EventLogger
    var hasDebugger = false


    init {
        if (!holdConnect)
            webSocket.connect()
    }

    private fun request(
        path: String,
        contentType: String = "application/json",
        method: String = "GET",
        data: String? = null,
        authorized: Boolean = true
    ): Response {

        val request = if (!authorized) Request.Builder()
            .url("https://discordapp.com/api$path")
            .addHeader(
                "User-Agent",
                USER_AGENT
            )
            .method(method, data?.toRequestBody(contentType.toMediaType()))
            .build()
        else
            Request.Builder()
                .url("https://discordapp.com/api$path")
                .addHeader(
                    "User-Agent",
                    USER_AGENT
                )
                .addHeader("Authorization", token)
                .method(method, data?.toRequestBody(contentType.toMediaType()))
                .build()
        val resp = client.newCall(request).execute()
        val b = resp.body!!.string()

        val respObj = Response(
            resp.code,
            resp.code == 200,
            b,
            resp.headers,
            b
        )
        if (hasDebugger) debugger.pushApiUpdate(path, method, respObj, data)

        return respObj
    }

    fun attachDebugger(logger: EventLogger) {
        debugger = logger
        hasDebugger = true
    }

    fun sendMessageAckByChannelSwitch(channelId: String, messageId: String) {
        webSocket.sendMessage(OpCode.CHANNEL_SWITCH, JSONObject().put("channel_id", channelId))
        request(
            "/channels/$channelId/messages/$messageId/ack",
            method = "POST",
            data = JSONObject().put("token", JSONObject.NULL).toString()
        )
    }

    fun getDmChannels(): JSONArray {
        val response = request("/users/@me/channels")
        return JSONArray(response.data)
    }

    fun sendTotp(code: String, ticket: String): Response {
        return request(
            "/v6/auth/mfa/totp",
            method = "POST",
            data = JSONObject().put("code", code).put("ticket", ticket).put(
                "gift_code_sku_id",
                JSONObject.NULL
            ).put("login_source", JSONObject.NULL).toString(),
            authorized = false
        )
    }

    fun getGuilds(): JSONArray {
        val response = request("/users/@me/guilds")
        return JSONArray(response.data)
    }
    fun loadGuildUsers(id:String) {

        webSocket.sendMessage(OpCode.REQUEST_GUILD_MEMBERS, JSONObject().put("guild_id", id).put("query", "").put("limit", 0))
    }
    fun getGuildChannels(id: String): JSONArray {
        val response = request("/guilds/$id/channels")
        return JSONArray(response.data)
    }

    fun getSelf(): JSONObject {
        val response = request("/users/@me")
        return JSONObject(response.data)
    }

    fun login(email: String, password: String): Response {
        val obj = JSONObject().put("email", email).put("password", password).put("undelete", false)
        return request("/v6/auth/login", data = obj.toString(), method = "POST", authorized = false)
    }

    fun getMessages(channelId: String): Response {
        return request("/channels/$channelId/messages")

    }

    fun sendLogout(): Response {
        return request(
            "/v6/auth/logout",
            method = "POST",
            data = JSONObject().put("provider", JSONObject.NULL).put("voip_provider", JSONObject.NULL).toString()
        )
    }

    fun editMessage(channelId: String, messageId: String, content: String): Response {
        return request(
            "/channels/$channelId/messages/$messageId",
            method = "PATCH",
            data = JSONObject().put("content", content).toString()
        )
    }

    fun deleteMessage(channelId: String, messageId: String): Response {
        return request("/channels/$channelId/messages/$messageId", method = "DELETE")
    }

    fun createDm(target: String): Response {
        return request(
            "/users/@me/channels",
            method = "POST",
            data = JSONObject().put("recipient_id", target).toString()
        )
    }

    fun sendRequest(targetId: String): Response {
        return request("/users/@me/relationships/$targetId", method = "PUT")
    }

    fun removeFriend(targetId: String): Response {
        return request("/users/@me/relationships/$targetId", method = "DELETE")
    }

    fun getFrieends(): Response {
        return request("/users/@me/relationships")
    }

    fun leaveServer(id: String): Response {
        return request("/users/@me/guilds/$id", method = "DELETE")
    }

    fun getMessages(channelId: String, before: String): JSONArray {
        val response = request("/channels/$channelId/messages?before=$before")
        return JSONArray(response.data)
    }

    fun acceptInvite(id: String): Response {
        return request("/invites/$id", method = "POST", data = "")
    }

    fun updateSettings(obj: JSONObject): Response {
        return request("/users/@me/settings", method = "PATCH", data = obj.toString())
    }

    fun updateOnlineStatus(status: OnlineStatus) {
        val obj =
            JSONObject().put("status", status.value).put("since", 0).put("activities", JSONArray()).put("afk", false)
        webSocket.sendMessage(OpCode.STATUS_UPDATE, obj)
        updateSettings(JSONObject().put("status", status.value))
    }

    fun sendSimpleMessage(channelId: String, message: String): Response {
        val obj = JSONObject()
        obj.put("content", message)
        obj.put("tts", false)
        obj.put("none", "${System.currentTimeMillis()}")
        return request("/channels/$channelId/messages", method = "POST", data = obj.toString())
    }
}
