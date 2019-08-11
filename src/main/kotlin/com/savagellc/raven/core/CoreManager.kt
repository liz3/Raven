package com.savagellc.raven.core

import com.savagellc.raven.Data
import com.savagellc.raven.discord.Api
import com.savagellc.raven.gui.Manager
import com.savagellc.raven.include.*
import org.json.JSONObject
import java.util.*

class CoreManager(val guiManager: Manager) {
    val api = Api(Data.token)
    lateinit var me:Me
    val chats = Vector<PrivateChat>()
    val servers = Vector<Server>()
    val editedMessage = Vector<GuiMessage>()
    lateinit var activeChat:Channel
    init {
      api.webSocket.addEventListener("MESSAGE_CREATE") {json ->
          val id = json.getString("channel_id")
          if(activeChat.id == id) {
              val message = GuiMessage(json, this, activeChat)
              activeChat.messages.add(message)
              guiManager.appendMessage(message)
              return@addEventListener
          }
          chats.forEach {
              if(it.id == id && it.loaded) {
                  val message = GuiMessage(json, this, it)
                  it.messages.add(message)
                  return@addEventListener
              }
          }
          servers.forEach {
              it.channels.forEach {
                  if(it.loaded && it.id == id) {
                      val message = GuiMessage(json, this, it)
                      it.messages.add(message)
                      return@addEventListener
                  }
              }
          }
      }
        api.webSocket.addEventListener("MESSAGE_UPDATE") {json ->

        }
    }
    fun loadOlderMessages() {
        if(activeChat.hasLoadedAll) return
        Thread {
            val firstId = activeChat.messages[0].id
            val messages = api.getMessages(activeChat.id, firstId).map { GuiMessage(it as JSONObject, this, activeChat) }
            if(messages.isEmpty()) activeChat.hasLoadedAll = true;
            messages.forEach {
                activeChat.messages.add(0, it)
            }
            guiManager.prepend(messages)
        }.start()
    }
    fun sendMessage(message:String, cb: (GuiMessage) -> Unit) {
        if(this::activeChat.isInitialized) {
           Thread {
               val sendObj = JSONObject(api.sendSimpleMessage(activeChat.id, message).data)
               val messageObj = GuiMessage(
                   sendObj, this, activeChat
               )
                activeChat.messages.add(messageObj)
               cb(messageObj)
           }.start()
        }
    }
    fun loadServerChannels(server:Server,cb:() -> Unit) {
        if(server.loadedChannels) {
            cb()
            return
        }
        Thread {
          api.getGuildChannels(server.id).forEach {
              server.channels.add(ServerChannel(it as JSONObject))
          }
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
    fun editMessage(message: GuiMessage, updatedContent:String, channelId:String, callback: () -> Unit) {
        Thread {
            api.editMessage(channelId, message.id, updatedContent)
            callback()
        }.start()
    }
    fun initChat(channel: Channel, callback: () -> Unit) {
        activeChat = channel
        Thread {
           if(!channel.loaded) channel.populateDataFromArray(api.getMessages(channel.id), this)
            callback()
        }.start()
    }
}