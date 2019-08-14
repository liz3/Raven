package com.savagellc.raven.gui.listitem.content

import com.savagellc.raven.discord.ImageCache
import com.savagellc.raven.gui.listitem.content.shared.Thumbnail
import com.savagellc.raven.gui.renders.getLabel
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import org.json.JSONObject

class AttachmentContentItem(val attachment: JSONObject) : MessageContentItem() {

    private var thumbnail: Thumbnail? = null

    init {
        if (attachment.has("title"))
            children.add(getLabel(attachment.getString("title"), "-fx-font-size: 15;", true))
        if (attachment.has("description"))
            children.add(getLabel(attachment.getString("description")))

        if (attachment.has("url")) {
            val task = object : Task<Void>() {
                override fun call(): Void? {
                    val url = attachment.getString("url")
                    val lW = attachment.getInt("width").toDouble()
                    thumbnail = Thumbnail(SwingFXUtils.toFXImage(ImageCache.getImage(url), null), width, lW)
                    Platform.runLater {
                        children.add(thumbnail)
                        requestRefresh()
                    }
                    return null
                }
            }
            val thread = Thread(task)
            thread.isDaemon = true
            thread.start()
        }
    }

    override fun onWidthChanged(newWidth: Double) {
        thumbnail?.updateChatWidth(newWidth)
    }

}