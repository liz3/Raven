package com.savagellc.raven.core

import com.savagellc.raven.discord.OnlineStatus
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class PresenceManager(val coreManager: CoreManager) {
    val onlineStatus = HashMap<String, OnlineStatus>()
    private val eventListenerGuild = Vector<Pair<String, (JSONObject) -> Unit>>()
    private val eventListenerUser = Vector<Pair<String, (JSONObject) -> Unit>>()

    init {
        coreManager.api.webSocket.addEventListener("PRESENCE_UPDATE") {json ->
            if(json.has("user") && !json.isNull("user")) {
                val id = json.getJSONObject("user").getString("id")
                val status = json.getString("status")
                onlineStatus[id] = OnlineStatus.values().find { it.value == status }!!
            }
            if(json.has("guild_id") && !json.isNull("guild_id")) {
                val guildId = json.getString("guild_id")
                eventListenerGuild.filter { it.first == guildId }.forEach {
                    it.second(json)
                }
            } else if(json.has("user") && !json.isNull("user")) {
                val id = json.getJSONObject("user").getString("id")
                eventListenerUser.filter { it.first == id }.forEach {
                    it.second(json)
                }
            }
        }
    }
    fun addUserListener(id:String, callback: (JSONObject) -> Unit) {
        eventListenerUser.add(Pair(id, callback))
    }
    fun addGuildListener(id:String, callback: (JSONObject) -> Unit) {
        eventListenerGuild.add(Pair(id, callback))
    }
    fun populateOnlineStatus(arr:JSONArray) {
        arr.forEach { json ->
            json as JSONObject
            val id = json.getJSONObject("user").getString("id")
          if(json.has("status") && !json.isNull("status")) {
              val status = json.getString("status")
              onlineStatus[id] = OnlineStatus.values().find { it.value == status }!!
          }
        }
    }
}