package com.savagellc.raven.gui.listitem

import com.savagellc.raven.discord.ImageCache
import com.savagellc.raven.gui.MessageMenu
import com.savagellc.raven.gui.listitem.content.ComplexContentItem
import com.savagellc.raven.gui.listitem.content.MessageContentItem
import com.savagellc.raven.gui.listitem.content.TextContentItem
import com.savagellc.raven.gui.renders.getLabel
import com.savagellc.raven.gui.renders.maxImageWidth
import com.savagellc.raven.include.GuiMessage
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.scene.control.ListView
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

open class Message(
    message: GuiMessage,
    private val messagesList: ListView<HBox>,
    renderSeparator: Boolean
) : HBox() {

     val content = VBox()

    init {
        setOnMouseClicked {
            MessageMenu.openMenu(
                message,
                it.screenX,
                it.screenY,
                message.coreManager,
                messagesList,
                it.button == MouseButton.SECONDARY
            )
        }
        children.add(content)
        content.style = "-fx-padding: 0 0 0 5;"
        if (renderSeparator)
            style += "-fx-border-color: grey; -fx-border-width: 2 0 0 0;"

        maxWidth = maxImageWidth
        if (message.author.get("avatar") is String) {
            val task = object : Task<Void>() {
                override fun call(): Void? {
                    val image =
                        SwingFXUtils.toFXImage(
                            ImageCache.getImage(
                                "https://cdn.discordapp.com/avatars/${message.author.getString("id")}/${message.author.getString(
                                    "avatar"
                                )}"
                            ),
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
        } else {
            style += "-fx-padding: 0 0 0 25";
        }

        val nameLabel = getLabel(message.senderName, "-fx-font-size: 16;")
        content.children.add(nameLabel)
    }

    fun addContentItem(contentItem: MessageContentItem) {
        when (contentItem) {
            is ComplexContentItem -> {
                contentItem.parentMessage = this
                content.children.add(contentItem)
            }
            is TextContentItem -> {
                contentItem.parentMessage = this
                content.children.add(contentItem)
            }
            else -> {

            }
        }
    }

    fun addAllContentItems(contentItems: List<Any>) {
        contentItems.forEach {
            if (it is MessageContentItem) addContentItem(it)
        }
    }

    fun clearContentItems() {
        content.children.removeIf { it is MessageContentItem }
    }

    fun getContentItems(): MutableList<MessageContentItem> {
        return content.children.filterIsInstance<MessageContentItem>() as MutableList<MessageContentItem>
    }

    fun updateWidth(newWidth: Double) {
        if(width > maxImageWidth) return
        prefWidth = newWidth - 80
        content.children.filterIsInstance<MessageContentItem>().forEach {
            it.updateWidth(newWidth)
        }
    }

    fun requestRefresh() {
        Platform.runLater {
            messagesList.refresh()
        }
    }
}