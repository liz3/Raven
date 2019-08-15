package com.savagellc.raven.gui.renders

import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.gui.cursorOnHover
import com.savagellc.raven.gui.listitem.Message
import com.savagellc.raven.gui.listitem.content.AttachmentContentItem
import com.savagellc.raven.gui.listitem.content.EmbeddedContentItem
import com.savagellc.raven.gui.listitem.content.StatusMessageContentItem
import com.savagellc.raven.gui.listitem.content.TextMessageContentItem
import com.savagellc.raven.include.GuiMessage
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import org.json.JSONArray
import org.json.JSONObject


const val maxImageWidth = 1000.0
fun getLabel(content: String, style: String = "", isUnderLined: Boolean = false): Label {
    val label = Label(content)
    label.isWrapText = true;
    if (style.isNotEmpty()) label.style = style
    label.isUnderline = isUnderLined
    label.maxWidth = maxImageWidth - 80
    return label
}

fun getLabel(content: String, style: String = "", isUnderLined: Boolean = false, cb: () -> Unit): Label {
    val label = Label(content)
    cursorOnHover(label)
    label.setOnMouseClicked {
        cb()
    }
    label.isWrapText = true;
    if (style.isNotEmpty()) label.style = style
    label.isUnderline = isUnderLined
    label.maxWidth = maxImageWidth - 80
    return label
}

fun appendClick(label: Label, cb: () -> Unit) {
    cursorOnHover(label)
    label.setOnMouseClicked {
        cb()
    }
}

fun processMentions(mentions: JSONArray, content: String): String {
    var cpy = content
    mentions.forEach {
        it as JSONObject
        val id = it.getString("id")
        val name = it.getString("username")
        cpy = cpy.replace("<@$id>", "@$name")
    }
    return cpy
}

fun render(
    message: GuiMessage,
    messagesList: ListView<HBox>,
    coreManager: CoreManager,
    renderSeparator: Boolean
): Message {
    val m = Message(message, messagesList, renderSeparator)

    if (message.content != "") {
        m.addContentItem(TextMessageContentItem(message))
    }

    if (message.type == 3) {
        m.addContentItem(StatusMessageContentItem())
    }

    message.attachments.forEach {
        m.addContentItem(AttachmentContentItem(it as JSONObject, messagesList))
    }

    message.embeds.forEach {
        m.addContentItem(EmbeddedContentItem(it as JSONObject, coreManager.mediaProxyServer.port, messagesList))
    }

    messagesList.widthProperty().addListener { _, _, newValue ->
        m.updateWidth(newValue.toDouble())
    }


    return m // TODO
}
