package com.savagellc.raven.include

import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class Server(val obj:JSONObject) {
    val id = obj.getString("id")
    val name = obj.getString("name")
    val channels = Vector<ServerChannel>()
    var loadedChannels = false


    override fun toString(): String {
        return name
    }
}
class ServerChannel(val obj: JSONObject) : Channel(obj) {
    val type = obj.getInt("type")
    val name = obj.getString("name")
    val guiObj = GuiServerChannel(id, name, this)
}
class ServerCategory(val name:String) {
    override fun toString(): String {
        return name
    }
}