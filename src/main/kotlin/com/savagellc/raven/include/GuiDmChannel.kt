package com.savagellc.raven.include

import org.json.JSONObject

class GuiDmChannel(val id: String, val name: String, val privateChat: PrivateChat) {
    override fun toString(): String {

        if (privateChat.obj.has("name") && privateChat.obj.get("name") is String)
            return "> ${privateChat.obj.getString("name")}"
        return if (privateChat.isDm)
            "> $name"
        else
            "> [${privateChat.obj.getJSONArray("recipients").joinToString(",") { (it as JSONObject).getString("username") }}]"
    }
}

class GuiServerChannel(val id: String, val name: String, val privateChat: ServerChannel) {
    override fun toString(): String {
        return "#$name"
    }
}
