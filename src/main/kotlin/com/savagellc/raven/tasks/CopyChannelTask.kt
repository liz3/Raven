package com.savagellc.raven.tasks

import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.include.Channel
import org.json.JSONObject
import java.util.*

class CopyChannelTask(val coreManager: CoreManager) {

    fun copy(sourceChannel: Channel, targetChannel: Channel) {
        Thread {
            val allMessages = Vector<JSONObject>()
            allMessages += sourceChannel.messages.map { it.rootObj }
            while (true) {
                println("Retrieving more messages")
                val firstId = allMessages[0].getString("id")
               try {
                   val messages = coreManager.api.getMessages(sourceChannel.id, firstId)
                   println(messages.length())
                   if (messages.length() == 0) break
                   messages.forEach {
                       allMessages.add(0, it as JSONObject?)
                   }
                   Thread.sleep(250)
               }catch (e:Exception) {
                   break
               }
            }
            println("Writing messages")
            for (message in allMessages) {
                val author = message.getJSONObject("author").getString("username")
                val content = message.getString("content")
                val attachments = message.getJSONArray("attachments").filter { it is JSONObject && it.has("url") }.map {
                    "Attachment ${if ((it as JSONObject).has("filename")) (it as JSONObject).getString("filename") else "-"}: ${(it as JSONObject).getString(
                        "url"
                    )}"
                }.joinToString("\n")

                var final = "**$author**:\n$content\n$attachments"
                if (final.length > 2000) final = "**$author**:\n$content\n"
                coreManager.api.sendSimpleMessage(targetChannel.id, final)
                Thread.sleep(100)
            }
        }.start()
    }
}