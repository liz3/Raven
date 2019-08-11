package com.savagellc.raven.include

import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class PrivateChat(val obj: JSONObject) : Channel(obj) {
    val isDm = obj.getInt("type") == 1
    private val userObject = {
        val arr = obj.getJSONArray("recipients")
        if(arr.length() == 0)
             JSONObject().put("username", "")
        else
         arr.getJSONObject(0)
    }.invoke()
    val username = userObject.getString("username")
    val guiObj = GuiDmChannel(id, username, this)
}