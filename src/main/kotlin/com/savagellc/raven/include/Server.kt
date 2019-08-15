package com.savagellc.raven.include

import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class Server(val obj: JSONObject) {
    val id = obj.getString("id")
    val name = obj.getString("name")
    val channels = Vector<ServerChannel>()
    val users = Vector<GuildMember>()
    var loadedChannels = false
    var loadedUsers = false


    override fun toString(): String {
        return name
    }
}

class ServerChannel(val obj: JSONObject, val server: Server) : Channel(obj) {
    val type = obj.getInt("type")
    val name = obj.getString("name")
    val guiObj = GuiServerChannel(id, name, this)
}
class GuildMember(private val obj:JSONObject) {
    val username = obj.getJSONObject("user").getString("username")

    override fun toString(): String {
        return username
    }
}
class ServerCategory(val name: String) {
    override fun toString(): String {
        return name
    }
}
