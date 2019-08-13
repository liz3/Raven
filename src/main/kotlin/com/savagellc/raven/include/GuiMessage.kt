package com.savagellc.raven.include

import com.savagellc.raven.Data
import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.gui.OpenTab
import com.savagellc.raven.gui.renders.getLabel
import com.savagellc.raven.gui.renders.render
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
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
    private lateinit var hBox: Triple<HBox, Label, VBox>
    private lateinit var editorField: TextField
    var renderSeparator = false
    var isEditMode = false
    var revisions = 1
    var hasUpdate = false
    lateinit var cachedUpdates: Vector<Triple<HBox, Label, VBox>>
    fun pushContentUpdate(updatedContent: String?, embeds: JSONArray?, attachments: JSONArray?) {
        if (updatedContent != null) content = updatedContent
        if (embeds != null) this.embeds = embeds
        if (attachments != null) this.attachments = attachments
        if (!Data.options.preventMessageUpdate) {
            Platform.runLater {
                val result = render(this, guiObject.controller.messagesList, coreManager, renderSeparator)
                hBox.third.children.clear()
                hBox.third.children.addAll(result.third.children)
            }
        } else {
            Platform.runLater {
                if (!this::cachedUpdates.isInitialized) cachedUpdates = Vector()
                cachedUpdates.add(render(this, guiObject.controller.messagesList, coreManager, renderSeparator))
                hasUpdate = true
                hBox.first.border = Border(
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
            hBox.first.border = Border(
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
            guiObject.controller.messagesList.items.remove(hBox.first)
            guiObject.controller.messagesList.refresh()
        }

    }

    fun showUpdate(append: Boolean) {
        if (!hasUpdate) return
        hasUpdate = false
        if (!append) {
            hBox.third.children.clear()
            revisions++
            hBox.third.children.addAll(cachedUpdates.last().third.children)
        } else {
            cachedUpdates.forEach {
                revisions++
                it.third.children[0] = getLabel("Revision $revisions", isUnderLined = true)
                hBox.third.children.addAll(it.third.children)
            }

        }
        cachedUpdates.clear()
        hBox.first.border = null

    }

    fun editMode() {
        if (!isEditMode) {
            isEditMode = true
            Platform.runLater {
                editorField = TextField(content)
                editorField.setOnKeyPressed {
                    if (it.code == KeyCode.ENTER) {
                        coreManager.editMessage(this, editorField.text, channel.id) {
                            Platform.runLater {
                                hBox.third.children.remove(editorField)
                                isEditMode = false
                            }
                        }
                        return@setOnKeyPressed
                    }
                    if (it.code == KeyCode.ESCAPE) {
                        hBox.third.children.remove(editorField)
                        isEditMode = false
                    }
                }
                hBox.third.children.add(editorField)
            }
        } else {
            hBox.third.children.remove(editorField)
            isEditMode = false
        }
    }

    fun deleteMessage() {
        coreManager.deleteMessage(this, channel.id) {}
    }

    override fun toString(): String {
        return "$senderName> $content"
    }

    fun getRendered(messagesList: ListView<HBox>): HBox {
        hBox = render(this, messagesList, coreManager, renderSeparator)
        coreManager.messageIndex[this.id] = this
        return hBox.first
    }
}
