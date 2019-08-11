package com.savagellc.raven.include

import com.savagellc.raven.gui.renders.render
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import org.json.JSONArray
import org.json.JSONObject

class GuiMessage(
val rootObj:JSONObject
) {
    val id = rootObj.getString("id")
    val author = rootObj.getJSONObject("author")
    val senderId = author.getString("id")
    val type = rootObj.getInt("type")
    val senderName = author.getString("username")
    val content: String = rootObj.getString("content")
    val attachments = rootObj.getJSONArray("attachments")
    val embeds = rootObj.getJSONArray("embeds")

    override fun toString(): String {
        return "$senderName> $content"
    }
    fun getRendered(messagesList: ListView<HBox>): HBox {
            return render(this, messagesList)
    }
}