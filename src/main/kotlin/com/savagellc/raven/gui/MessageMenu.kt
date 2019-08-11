package com.savagellc.raven.gui

import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.include.GuiMessage
import javafx.application.Platform
import javafx.scene.control.ContextMenu
import javafx.scene.control.ListView
import javafx.scene.control.MenuItem
import javafx.scene.layout.HBox

object MessageMenu {
    private var visible = false
    lateinit var menu: ContextMenu
    fun openMenu(
        message: GuiMessage,
        x: Double,
        y: Double,
        coreManager: CoreManager,
        messagesList: ListView<HBox>,
        b: Boolean
    ) {
        if (visible) {
            visible = false
            menu.hide()
        }
        if(!b) return
        val myId = coreManager.me.id
        menu = ContextMenu()

        if(myId == message.author.getString("id")) {
            val edit = MenuItem("Edit")
            edit.setOnAction {
                message.editMode()
            }
            val delete = MenuItem("Delete")
            delete.setOnAction {
                message.deleteMessage()
            }
            menu.items.addAll(edit, delete)
        }

        Platform.runLater {
            menu.show(messagesList, x,y)
            visible = true
        }
    }
}