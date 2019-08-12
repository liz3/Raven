package com.savagellc.raven.core

import com.savagellc.raven.Data
import com.savagellc.raven.discord.Api
import com.savagellc.raven.discord.ChannelType
import com.savagellc.raven.gui.Manager
import com.savagellc.raven.include.*
import javafx.application.Platform
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class CoreManager(val guiManager: Manager) {
    val api = Api(Data.token)
    lateinit var me: Me
    val chats = Vector<PrivateChat>()
    val servers = Vector<Server>()
    val messageIndex = HashMap<String, GuiMessage>()
    lateinit var activeChat: Channel
    lateinit var activeServer: Server

    init {
        api.webSocket.addEventListener("MESSAGE_CREATE") { json ->
            val id = json.getString("channel_id")
            if (activeChat.id == id) {
                val message = GuiMessage(json, this, activeChat)
                activeChat.messages.add(message)
                guiManager.appendMessage(message)
                if (activeChat is PrivateChat) {
                    guiManager.moving = true
                    val obj = (activeChat as PrivateChat).guiObj
                    Platform.runLater {
                        guiManager.controller.dmChannelsList.items.remove(obj)
                        Platform.runLater {
                            guiManager.controller.dmChannelsList.items.add(0, obj)
                            guiManager.controller.dmChannelsList.selectionModel.select(obj)
                            guiManager.moving = false
                        }
                    }
                }
                return@addEventListener
            }
            chats.forEach {
                if (it.id == id) {
                    if (it is PrivateChat) {
                        val obj = it.guiObj
                        Platform.runLater {
                            guiManager.controller.dmChannelsList.items.remove(obj)
                            guiManager.controller.dmChannelsList.items.add(0, obj)
                        }
                    }
                }
                if (it.id == id && it.loaded) {
                    val message = GuiMessage(json, this, it)
                    it.messages.add(message)
                    return@addEventListener
                }
            }
            servers.forEach { srv ->
                srv.channels.forEach {
                    if (it.loaded && it.id == id) {
                        val message = GuiMessage(json, this, it)
                        it.messages.add(message)
                        return@addEventListener
                    }
                }
            }
        }
        api.webSocket.addEventListener("MESSAGE_UPDATE") { json ->
            Thread {
                val id = json.getString("id")
                if (messageIndex.containsKey(id)) {
                    messageIndex[id]!!.pushContentUpdate(json.getString("content"))
                }

            }.start()
        }
        api.webSocket.addEventListener("MESSAGE_DELETE") { json ->
            val id = json.getString("id")
            if (messageIndex.containsKey(id)) {
                messageIndex[id]!!.pushRemove()
            }
        }
        api.webSocket.addEventListener("CHANNEL_CREATE") { json ->
            if (json.getInt("type") == ChannelType.DM.num) {
                val chat = PrivateChat(json)
                chats.add(chat)
                guiManager.addDmChat(chat)
            } else {
                if (json.has("guild_id") && !json.isNull("guild_id")) {
                    val guildId = json.getString("guild_id")
                    val server = servers.find { it.id == guildId }
                    server?.channels?.add(ServerChannel(json))
                    if (this::activeServer.isInitialized)
                        if (activeServer == server)
                            Platform.runLater {
                                guiManager.treeView.root = guiManager.renderServerChannels(server)
                            }
                }
            }
        }
        api.webSocket.addEventListener("CHANNEL_DELETE") { json ->
            val type = json.getInt("type")
            if (type == ChannelType.DM.num) {

            } else {
                if (json.has("guild_id") && !json.isNull("guild_id")) {
                    val guildId = json.getString("guild_id")
                    val server = servers.find { it.id == guildId }
                    if (server != null) {
                        val channel = server.channels.find { it.id == json.getString("id") }
                        server.channels.remove(channel)
                        if (this::activeServer.isInitialized)
                            if (activeServer == server)
                                Platform.runLater {
                                    guiManager.treeView.root = guiManager.renderServerChannels(server)
                                }
                    }

                }
            }
        }
        api.webSocket.addEventListener("GUILD_DELETE") { json ->
            val server = servers.find { it.id == json.getString("id") }
            if (server != null) {
                Platform.runLater {
                    guiManager.controller.serversList.items.remove(server)
                    servers.remove(server)
                }
            }
        }
        api.webSocket.addEventListener("GUILD_CREATE") { json ->
            val serverObj = Server(json)
            servers.add(serverObj)
            Platform.runLater {
                guiManager.controller.serversList.items.add(serverObj)
            }
        }
    }

    fun leaveServer(id: String) {
        Thread {
            api.leaveServer(id)
        }.start()
    }

    fun loadOlderMessages() {
        if (activeChat.hasLoadedAll) return
        Thread {
            val firstId = activeChat.messages[0].id
            val messages =
                api.getMessages(activeChat.id, firstId).map { GuiMessage(it as JSONObject, this, activeChat) }
            if (messages.isEmpty()) activeChat.hasLoadedAll = true;
            messages.forEach {
                activeChat.messages.add(0, it)
            }
            guiManager.prepend(messages)
        }.start()
    }

    fun createDm(target: String) {
        if (chats.find { it.id == target } != null) return
        Thread {
            api.createDm(target)
        }.start()
    }

    fun sendMessage(message: String, cb: (GuiMessage) -> Unit) {
        if (this::activeChat.isInitialized) {
            Thread {
                val sendObj = JSONObject(api.sendSimpleMessage(activeChat.id, message).data)
                val messageObj = GuiMessage(
                    sendObj, this, activeChat
                )
                cb(messageObj)
            }.start()
        }
    }

    fun loadServerChannels(server: Server, cb: () -> Unit) {
        if (server.loadedChannels) {
            cb()
            return
        }
        Thread {
            api.getGuildChannels(server.id).forEach {
                server.channels.add(ServerChannel(it as JSONObject))
            }
            server.loadedChannels = true
            cb()
        }.start()
    }

    fun initLoad() {
        me = Me(api.getSelf())
        api.getDmChannels().forEach {
            chats.add(PrivateChat(it as JSONObject))
        }
        api.getGuilds().forEach {
            it as JSONObject
            servers.add(Server(it))
        }
    }

    fun editMessage(message: GuiMessage, updatedContent: String, channelId: String, callback: () -> Unit) {
        Thread {
            api.editMessage(channelId, message.id, updatedContent)
            callback()
        }.start()
    }

    fun deleteMessage(message: GuiMessage, channelId: String, callback: () -> Unit) {
        Thread {
            api.deleteMessage(channelId, message.id)
            callback()
        }.start()
    }

    fun initChat(channel: Channel, callback: () -> Unit) {
        activeChat = channel
        Thread {
            if (!channel.loaded) channel.populateDataFromArray(api.getMessages(channel.id), this)
            callback()
        }.start()
    }
}