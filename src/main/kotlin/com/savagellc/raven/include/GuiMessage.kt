package com.savagellc.raven.include

import com.savagellc.raven.Data
import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.gui.OpenTab
import com.savagellc.raven.gui.listitem.Message
import com.savagellc.raven.gui.listitem.content.MetaContentItem
import com.savagellc.raven.gui.renders.render
import javafx.application.Platform
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.*
import javafx.scene.paint.Color
import org.json.JSONArray
import org.json.JSONObject
import java.util.*


class GuiMessage(
    val rootObj: JSONObject,
    val coreManager: CoreManager,
    val guiObject: OpenTab
) {
    val channel = guiObject.channel
    val id = rootObj.getString("id")
    val author = rootObj.getJSONObject("author")
    val senderId = author.getString("id")
    val type = rootObj.getInt("type")
    val senderName = author.getString("username")
    var content = rootObj.getString("content")
    var attachments = rootObj.getJSONArray("attachments")
    var embeds = rootObj.getJSONArray("embeds")

    private lateinit var message: Message
    private lateinit var editorField: TextField
    var renderSeparator = false
    var isEditMode = false
    var revisions = 1
    var hasUpdate = false

    lateinit var cachedUpdates: Vector<Message>


    fun pushContentUpdate(updatedContent: String?, embeds: JSONArray?, attachments: JSONArray?) {
        if (updatedContent != null) content = updatedContent
        if (embeds != null) this.embeds = embeds
        if (attachments != null) this.attachments = attachments

        if (!Data.options.preventMessageUpdate) {
            Platform.runLater {
                val result = render(this, guiObject.controller.messagesList, coreManager, renderSeparator)
                message.clearContentItems()
                message.addAllContentItems(result.getContentItems())
            }
        } else {
            Platform.runLater {
                if (!this::cachedUpdates.isInitialized) cachedUpdates = Vector()
                cachedUpdates.add(render(this, guiObject.controller.messagesList, coreManager, renderSeparator))
                hasUpdate = true
                message.border = Border(
                    BorderStroke(
                        Color.BLUE,
                        BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT
                    )
                )
            }
        }

    }

    fun pushRemove() {
        if (Data.options.preventMessageDelete) {
            message.border = Border(
                BorderStroke(
                    Color.RED,
                    BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT
                )
            )
            return
        }
        coreManager.messageIndex.remove(id)
        channel.messages.remove(this)

        Platform.runLater {
            guiObject.controller.messagesList.items.remove(message)
            guiObject.controller.messagesList.refresh()
        }

    }

    fun showUpdate(append: Boolean) {
        if (!hasUpdate) return
        hasUpdate = false
        if (!append) {
            message.clearContentItems()
            revisions++
            message.addAllContentItems(cachedUpdates.last().getContentItems())
        } else {
            cachedUpdates.forEach {
                revisions++
                it.getContentItems()[0] = MetaContentItem(true, revisions)
                message.addAllContentItems(it.getContentItems())
            }

        }
        cachedUpdates.clear()
        message.border = null

    }

    fun editMode() {
        /* TODO
        if (!isEditMode) {
            isEditMode = true
            Platform.runLater {
                editorField = TextField(content)
                editorField.setOnKeyPressed {
                    if (it.code == KeyCode.ENTER) {
                        coreManager.editMessage(this, editorField.text, channel.id) {
                            Platform.runLater {
                                message.third.children.remove(editorField)
                                isEditMode = false
                            }
                        }
                        return@setOnKeyPressed
                    }
                    if (it.code == KeyCode.ESCAPE) {
                        message.third.children.remove(editorField)
                        isEditMode = false
                    }
                }
                message.third.children.add(editorField)
                editorField.requestFocus()
            }
        } else {
            message.third.children.remove(editorField)
            isEditMode = false
        }*/
    }

    fun deleteMessage() {
        coreManager.deleteMessage(this, channel.id) {}
    }

    override fun toString(): String {
        return "$senderName> $content"
    }

    fun getRendered(messagesList: ListView<HBox>): Message {
        message = render(this, messagesList, coreManager, renderSeparator)
        coreManager.messageIndex[this.id] = this
        return message
    }
}
