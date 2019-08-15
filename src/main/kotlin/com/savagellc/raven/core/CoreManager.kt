package com.savagellc.raven.core

import com.savagellc.raven.Data
import com.savagellc.raven.discord.Api
import com.savagellc.raven.discord.ChannelType
import com.savagellc.raven.discord.computeAvatarImagePath
import com.savagellc.raven.gui.Manager
import com.savagellc.raven.gui.OpenTab
import com.savagellc.raven.gui.Prompts
import com.savagellc.raven.gui.renders.processMentions
import com.savagellc.raven.gui.sendNotification
import com.savagellc.raven.include.*
import com.savagellc.raven.utils.MediaProxyServer
import javafx.application.Platform
import javafx.scene.control.Alert
import okhttp3.internal.notify
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class CoreManager(val guiManager: Manager) {
    val api = Api(Data.token)
    val mediaProxyServer = MediaProxyServer()
    val presenceManager = PresenceManager(this)
    lateinit var me: Me
    val chats = Vector<PrivateChat>()
    val servers = Vector<Server>()
    val messageIndex = HashMap<String, GuiMessage>()
    lateinit var activeServer: Server

    init {
        api.webSocket.addEventListener("READY") {json ->
            me = Me(json.getJSONObject("user"))
            presenceManager.populateOnlineStatus(json.getJSONArray("presences"))
            json.getJSONArray("private_channels").forEach {
                chats.add(PrivateChat(it as JSONObject))
            }
            json.getJSONArray("guilds").forEach {
                servers.add(Server(it as JSONObject))
            }
        }
        api.webSocket.addEventListener("MESSAGE_CREATE") { json ->
            val id = json.getString("channel_id")
            if (json.getJSONArray("mentions").find { (it as JSONObject).getString("id") == me.id } != null || json.getBoolean(
                    "mention_everyone"
                ) ||
                (chats.find { it.id == id } != null && (guiManager.controller.openChatsTabView.selectionModel.selectedItem == null
                        || guiManager.openChats.values.find { it.guiTab == guiManager.controller.openChatsTabView.selectionModel.selectedItem }?.channel!!.id != id))) {
                sendNotification(
                    json.getJSONObject("author").getString("username"),
                    processMentions(json.getJSONArray("mentions"), json.getString("content").replace("\n", "")),
                    computeAvatarImagePath(json.getJSONObject("author"))
                )

            }
            if (guiManager.openChats.containsKey(id)) {
                val activeChat = guiManager.openChats[id]!!
                val message = GuiMessage(json, this, activeChat)

                activeChat.channel.messages.add(message)
                activeChat.appendMessage(message)
                if (activeChat.channel is PrivateChat) {
                    guiManager.moving = true
                    val obj = activeChat.channel.guiObj
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
                    val message = GuiMessage(json, this, it.guiReference)
                    it.messages.add(message)
                    return@addEventListener
                }
            }
            servers.forEach { srv ->
                srv.channels.forEach {
                    if (it.loaded && it.id == id) {
                        val message = GuiMessage(json, this, it.guiReference)
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
                    val saved = messageIndex[id]
                    saved!!.pushContentUpdate(
                        if (json.has("content") && !json.isNull("content")) json.getString("content") else null,
                        if (json.has("embeds") && !json.isNull("embeds")) json.getJSONArray("embeds") else null,
                        if (json.has("attachments") && !json.isNull("attachments")) json.getJSONArray("attachments") else null
                    )
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

    fun loadOlderMessages(channel: Channel, tab: OpenTab) {
        if (channel.hasLoadedAll) return
        Thread {
            val firstId = channel.messages[0].id
            val messages =
                api.getMessages(channel.id, firstId).map { GuiMessage(it as JSONObject, this, tab) }
            if (messages.isEmpty()) channel.hasLoadedAll = true;
            messages.forEach {
                channel.messages.add(0, it)
            }
            tab.prepend(messages)
        }.start()
    }

    fun createDm(target: String) {
        if (chats.find { it.id == target } != null) return
        Thread {
            api.createDm(target)
        }.start()
    }

    fun sendMessage(message: String, channel: OpenTab, cb: (GuiMessage) -> Unit) {
        Thread {
            val sendObj = JSONObject(api.sendSimpleMessage(channel.channel.id, message).data)
            val messageObj = GuiMessage(
                sendObj, this, channel
            )
            cb(messageObj)
        }.start()
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

    fun initChat(channel: Channel, callback: (Boolean) -> Unit) {
        Thread {
            val response = api.getMessages(channel.id)
            if (response.hasData) {
                if (!channel.loaded) channel.populateDataFromArray(JSONArray(response.data), this)
                callback(true)
            } else {
                Platform.runLater {
                    callback(false)
                    Prompts.infoCheck(
                        "Failed to load Channel",
                        "Cant load channel",
                        "Failed to retrieve messages (${response.code} ${response.respMessage})",
                        Alert.AlertType.ERROR
                    )
                }
            }
        }.start()
    }
}
