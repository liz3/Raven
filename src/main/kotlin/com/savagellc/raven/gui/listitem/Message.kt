package com.savagellc.raven.gui.listitem

import com.savagellc.raven.discord.ImageCache
import com.savagellc.raven.gui.listitem.content.MessageContentItem
import com.savagellc.raven.gui.renders.getLabel
import com.savagellc.raven.gui.renders.maxImageWidth
import com.savagellc.raven.include.GuiMessage
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.ListView
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

open class Message(message: GuiMessage, private val messagesList: ListView<HBox>) : HBox() {

    protected val content = VBox()

    init {
        children.add(content)

        style = "-fx-padding: 0 15 0 0;"
        //TODO: Render seperator

        maxWidth = maxImageWidth

        try {
            if (message.author.get("avatar") is String) {
                val task = object : Task<Void>() {
                    override fun call(): Void? {
                        val image =
                            SwingFXUtils.toFXImage(
                                ImageCache.getImage("https://cdn.discordapp.com/avatars/${message.author.getString("id")}/${message.author.getString("avatar")}"),
                                null
                            )
                        val view = ImageView(image)
                        view.isPreserveRatio = true
                        view.fitWidth = 25.0
                        Platform.runLater {
                            children.add(0, view)
                            requestRefresh()
                        }
                        return null
                    }
                }
                val loader = Thread(task)
                loader.isDaemon = true
                loader.start()
            }
        } catch (e: Exception) {
                e.printStackTrace()
        }


        val nameLabel = getLabel(message.senderName, "-fx-font-size: 16;")
        content.children.add(nameLabel)
    }

    fun addContentItem(contentItem: MessageContentItem) {
        contentItem.parentMessage = this
        content.children.add(contentItem)
    }

    fun onWidthChanged(newWidth: Double) {
        content.children.filterIsInstance<MessageContentItem>().forEach {
            it.onWidthChanged(newWidth)
        }
    }

    fun requestRefresh() {
        Platform.runLater {
            messagesList.refresh()
        }
    }

}