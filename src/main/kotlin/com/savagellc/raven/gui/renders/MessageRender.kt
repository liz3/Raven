package com.savagellc.raven.gui.renders

import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.discord.ImageCache
import com.savagellc.raven.gui.MessageMenu
import com.savagellc.raven.include.GuiMessage
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.json.JSONObject

const val maxImageWidth = 750.0

private fun getLabel(content:String, style:String = "", isUnderLined:Boolean = false): Label {
    val label = Label(content)
    label.isWrapText = true;
    if(style.isNotEmpty()) label.style = style
    label.isUnderline = isUnderLined
    return label
}
private fun addUserImage(
    id: String,
    avatar: String,
    rootBox: HBox,
    messagesList: ListView<HBox>
) {
    val task = object : Task<Void>() {
        override fun call(): Void? {
            val image = SwingFXUtils.toFXImage(ImageCache.getImage("https://cdn.discordapp.com/avatars/$id/$avatar"), null)
           Platform.runLater {
               val view = ImageView(image)
               view.isPreserveRatio = true
               view.fitWidth = 25.0
               rootBox.children.add(0, view)
               messagesList.refresh()
           }
            return null
        }
    }
    val loader = Thread(task)
    loader.isDaemon = true
    loader.start()
}
fun render(
    message: GuiMessage,
    messagesList: ListView<HBox>,
    coreManager: CoreManager
): Triple<HBox, Label, VBox> {
    val rootBox = HBox()
    rootBox.setOnMouseClicked {
            MessageMenu.openMenu(message, it.screenX, it.screenY, coreManager, messagesList, it.button == MouseButton.SECONDARY)
    }
    rootBox.maxWidth = maxImageWidth
    val contentRow = VBox()
    contentRow.padding = Insets(2.0, 0.0, 2.0, 5.0)
    rootBox.prefWidth = messagesList.width
    messagesList.widthProperty().addListener { observable, oldValue, newValue ->
        if(newValue.toDouble() < maxImageWidth)
        rootBox.prefWidth = newValue.toDouble()
    }
    if(message.author.get("avatar") is String)
    Platform.runLater {
        addUserImage(message.author.getString("id"), message.author.getString("avatar"), rootBox, messagesList)
    }
    HBox.setHgrow(contentRow, Priority.ALWAYS)
    val nameLabel = getLabel(message.senderName, "-fx-font-size: 16;")
    contentRow.children.add(nameLabel)
        val contentLabel = getLabel(message.content, "-fx-font-size: 15;")
    if(message.content != "") {
        contentRow.children.add(contentLabel)
    }
    if(message.type == 3) {
        val contentLabel = getLabel("> Started call")
        contentRow.children.add(contentLabel)
    }
    message.attachments.forEach {
        it as JSONObject
        val childBox = VBox()
        childBox.padding = Insets(5.0, 5.0, 5.0, 10.0)
        if(it.has("title")) childBox.children.add(getLabel(it.getString("title"), "-fx-font-size: 15;", true))
        if(it.has("description"))  childBox.children.add(getLabel(it.getString("description")))
        if(it.has("url")) {
            val task = object : Task<Void>() {
                override fun call(): Void? {
                    val url = it.getString("url")
                    val imageView = ImageView(SwingFXUtils.toFXImage(ImageCache.getImage(url), null))

                    imageView.isPreserveRatio = true
                    Platform.runLater {
                        imageView.fitWidth = if(messagesList.width < maxImageWidth) messagesList.width else maxImageWidth
                        messagesList.widthProperty().addListener { observable, oldValue, newValue ->
                            if(newValue.toDouble() < maxImageWidth)
                                imageView.fitWidth = newValue.toDouble()
                        }
                        childBox.children.add(imageView)

                        messagesList.refresh()
                    }
                    return null
                }
            }
            val thread = Thread(task)
            thread.isDaemon = true
            thread.start()
        }
        contentRow.children.add(childBox)
    }
    message.embeds.forEach {
        it as JSONObject
        val childBox = VBox()
        childBox.padding = Insets(5.0, 5.0, 5.0, 10.0)

        childBox.padding = Insets(5.0, 5.0, 5.0, 10.0)
        if(it.has("title")) childBox.children.add(getLabel(it.getString("title"), "-fx-font-size: 15;", true))
        if(it.has("description"))  childBox.children.add(getLabel(it.getString("description")))
        if(it.has("thumbnail")) {
            val task = object : Task<Void>() {
                override fun call(): Void? {
                    val url = it.getJSONObject("thumbnail").getString("url")
                    val imageView = ImageView(SwingFXUtils.toFXImage(ImageCache.getImage(url), null))

                    imageView.isPreserveRatio = true
                    Platform.runLater {
                        imageView.fitWidth = if(messagesList.width < maxImageWidth) messagesList.width else maxImageWidth
                        messagesList.widthProperty().addListener { observable, oldValue, newValue ->
                            if(newValue.toDouble() < maxImageWidth)
                                imageView.fitWidth = newValue.toDouble()
                        }
                        childBox.children.add(imageView)
                        messagesList.refresh()

                    }
                    return null
                }
            }
            val thread = Thread(task)
            thread.isDaemon = true
            thread.start()
        }
        contentRow.children.add(childBox)
    }
    rootBox.children.add(contentRow)

    return Triple(rootBox, contentLabel, contentRow)
}