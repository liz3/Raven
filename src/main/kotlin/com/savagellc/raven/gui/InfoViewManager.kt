package com.savagellc.raven.gui

import com.savagellc.raven.discord.ImageCache
import com.savagellc.raven.gui.controller.DirectMessageViewController
import com.savagellc.raven.gui.controller.ServerViewController
import com.savagellc.raven.gui.renders.getLabel
import com.savagellc.raven.include.Channel
import com.savagellc.raven.include.PrivateChat
import com.savagellc.raven.include.ServerChannel
import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXMLLoader
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.VBox
import org.json.JSONObject

class InfoViewManager(val guiManager: Manager) {
    private val rootPane = guiManager.controller.informationContainer
    private val dmPane: VBox
    private val dmController: DirectMessageViewController
    private val serverPane: VBox
    private val serverController: ServerViewController
    lateinit var channel: OpenTab
    var last = 0

    init {
        val dm = load("/fxml/DirectMessageChannelInfo.fxml")
        dmPane = dm.first as VBox
        dmController = dm.second as DirectMessageViewController
        val server = load("/fxml/ServerMessageChannelInfo.fxml")
        serverPane = server.first as VBox
        serverController = server.second as ServerViewController

        setupGuiListener()
    }

    private fun setupGuiListener() {
        AnchorPane.setTopAnchor(dmPane, 0.0)
        AnchorPane.setRightAnchor(dmPane, 0.0)
        AnchorPane.setBottomAnchor(dmPane, 0.0)
        AnchorPane.setLeftAnchor(dmPane, 0.0)
        AnchorPane.setTopAnchor(serverPane, 0.0)
        AnchorPane.setRightAnchor(serverPane, 0.0)
        AnchorPane.setBottomAnchor(serverPane, 0.0)
        AnchorPane.setLeftAnchor(serverPane, 0.0)
        dmController.closeBtn.setOnAction {
            toggle()
        }
        serverController.closeBtn.setOnAction {
            toggle()
        }
    }

    private fun loadDmChannel(channel: PrivateChat) {
        Platform.runLater {
            dmController.infoContainer.children.clear()
            rootPane.children.clear()
            if (channel.isDm) {
                dmController.infoContainer.children.add(
                    getLabel(
                        "Status: ${if (guiManager.coreManager.presenceManager.onlineStatus.containsKey(
                                channel.userObject.getString("id")
                            )
                        ) guiManager.coreManager.presenceManager.onlineStatus[channel.userObject.getString("id")]!!.fName else ""}"
                    )
                )
            }
            dmController.userNameLabel.text = {
                if (channel.obj.has("name") && channel.obj.get("name") is String)
                    "> ${channel.obj.getString("name")}"
                else if (channel.isDm)
                    "> ${channel.username}"
                else
                    "> [${channel.obj.getJSONArray("recipients").joinToString(",") { (it as JSONObject).getString("username") }}]"
            }.invoke()
            if (channel.obj.getInt("type") == 3) {
                if (channel.obj.has("icon") && !channel.obj.isNull("icon")) {
                    dmController.profile.isPreserveRatio = true

                    appendToImageViewAsync(
                        dmController.profile,
                        "https://cdn.discordapp.com/channel-icons/${channel.id}/${channel.obj.getString("icon")}"
                    )
                }
            } else if (channel.isDm && channel.userObject.has("avatar") && !channel.userObject.isNull("avatar")) {
                dmController.profile.isPreserveRatio = true
                appendToImageViewAsync(
                    dmController.profile,
                    "https://cdn.discordapp.com/avatars/${channel.userObject.getString("id")}/${channel.userObject.getString(
                        "avatar"
                    )}"
                )
            }
            rootPane.children.add(dmPane)
        }
    }

    private fun loadServerChannel(channel: ServerChannel) {
        Platform.runLater {
            serverController.serverUsersList.items.clear()
            rootPane.children.clear()
            serverController.profile.image = null
            serverController.userNameLabel.text = channel.server.name
            println(channel.server.users)
            rootPane.children.add(serverPane)
            serverController.serverUsersList.items.addAll(channel.server.users)
        }
    }

    fun toggle() {
        if (rootPane.children.size == 0) {
            if (last == 0) rootPane.children.add(dmPane)
            if (last == 1) rootPane.children.add(serverPane)
            rootPane.prefWidth = 215.0
        } else {
            rootPane.children.clear()
            rootPane.prefWidth = 0.0
        }
    }

    fun loadChannel(channel: OpenTab) {
        this.channel = channel
        if (channel.channel is PrivateChat) {
            last = 0
            loadDmChannel(channel.channel)
        } else if (channel.channel is ServerChannel) {
            last = 1
            loadServerChannel(channel.channel)
        }
    }

    private fun load(path: String): Pair<Any, Any> {
        val loader = FXMLLoader()
        val pane = loader.load<Any>(javaClass.getResourceAsStream(path))
        val controller = loader.getController<Any>()

        return Pair(pane, controller)
    }
}