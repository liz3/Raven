package com.savagellc.raven.include

import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.gui.renders.render
import javafx.application.Platform
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import org.json.JSONObject

class GuiMessage(
    val rootObj: JSONObject,
    val coreManager: CoreManager,
    val channel: Channel
) {
    val id = rootObj.getString("id")
    val author = rootObj.getJSONObject("author")
    val senderId = author.getString("id")
    val type = rootObj.getInt("type")
    val senderName = author.getString("username")
    var content = rootObj.getString("content")
    val attachments = rootObj.getJSONArray("attachments")
    val embeds = rootObj.getJSONArray("embeds")
    private lateinit var hBox: Triple<HBox, Label, VBox>
    private lateinit var editorField: TextField
    var isEditMode = false
    fun pushContentUpdate(updatedContent:String) {
        content = updatedContent
      Platform.runLater {
          hBox.second.text = updatedContent
      }
    }
    fun pushRemove() {
        coreManager.messageIndex.remove(id)
        channel.messages.remove(this)
        if(coreManager.activeChat == channel) {
            Platform.runLater {
                coreManager.guiManager.controller.messagesList.items.remove(hBox.first)
                coreManager.guiManager.controller.messagesList.refresh()
            }
        }
    }

    init {
        coreManager.messageIndex[id] = this
    }
    fun editMode() {
        if(!isEditMode) {
            isEditMode = true
            Platform.runLater {
                editorField = TextField(content)
                editorField.setOnKeyPressed {
                    if(it.code == KeyCode.ENTER) {
                        coreManager.editMessage(this, editorField.text, channel.id) {
                            Platform.runLater {
                                hBox.third.children.remove(editorField)
                                isEditMode = false
                            }
                        }
                        return@setOnKeyPressed
                    }
                    if(it.code == KeyCode.ESCAPE) {
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
        hBox = render(this, messagesList, coreManager)
        return hBox.first
    }
}