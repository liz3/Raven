package com.savagellc.raven.include

import com.savagellc.raven.core.CoreManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

open class Channel(val rootObject:JSONObject) {
    val id = rootObject.getString("id")
    val messages = Vector<GuiMessage>()
    var hasLoadedAll = false
    var loaded = false

    fun populateDataFromArray(data: JSONArray, coreManager: CoreManager) {
        messages.clear()
        loaded = true
        data.reversed().forEach {
            it as JSONObject
            messages.add(
                GuiMessage(
                    it, coreManager, this
                )
            )
        }
    }
}