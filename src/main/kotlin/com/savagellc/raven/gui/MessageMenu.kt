package com.savagellc.raven.gui

import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.include.Channel
import com.savagellc.raven.include.GuiMessage
import com.savagellc.raven.include.Server
import com.savagellc.raven.tasks.CopyChannelTask
import javafx.application.Platform
import javafx.scene.control.*
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
        if (!b) return
        val myId = coreManager.me.id
        menu = ContextMenu()

        if (myId == message.author.getString("id")) {
            val edit = MenuItem("Edit")
            edit.setOnAction {
                message.editMode()
            }
            val delete = MenuItem("Delete")
            delete.setOnAction {
                message.deleteMessage()
            }
            menu.items.addAll(edit, delete)
        } else {
            val id = message.author.getString("id")
            val createDm = MenuItem("Create Direct Message Channel")
            createDm.setOnAction {
                coreManager.createDm(id);
            }
            menu.items.add(createDm)
        }
        if (message.hasUpdate) {
            val showUpdate = MenuItem("Show updated message")
            showUpdate.setOnAction {
                message.showUpdate(false)
            }
            val showUpdateAppend = MenuItem("Append Message Revision")
            showUpdateAppend.setOnAction {
                message.showUpdate(true)
            }
            menu.items.addAll(showUpdate, showUpdateAppend)
        }

        menu.show(messagesList, x, y)
        visible = true

    }
}

object ServerMenu {
    private var visible = false
    lateinit var menu: ContextMenu
    fun openMenu(
        server: Server,
        x: Double,
        y: Double,
        coreManager: CoreManager,
        list: ListView<Server>,
        b: Boolean
    ) {
        if (visible) {
            visible = false
            menu.hide()
        }
        if (!b) return
        menu = ContextMenu()

        val deleteServer = MenuItem("Leave Server")
        deleteServer.setOnAction {
            coreManager.leaveServer(server.id)
        }
        menu.items.add(deleteServer)


        Platform.runLater {
            menu.show(list, x, y)
            visible = true
        }
    }
}

object ChannelMenu {

    fun getMenu(
        channel: Channel,
        coreManager: CoreManager
    ): ContextMenu {

        val menu = ContextMenu()

        val copyChannel = Menu("Copy Channel")
        menu.setOnShowing {
            copyChannel.items.clear()
            for (otherChannel in coreManager.guiManager.openChats.values.filter { it.channel != channel }) {
                val item = MenuItem(otherChannel.guiTab.text)
                item.setOnAction {
                    CopyChannelTask(coreManager).copy(channel, otherChannel.channel)
                }
                copyChannel.items.add(item)
            }
        }
        menu.items.add(copyChannel)

        return menu
    }
}
