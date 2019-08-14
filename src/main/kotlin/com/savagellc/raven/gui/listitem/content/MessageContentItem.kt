package com.savagellc.raven.gui.listitem.content

import com.savagellc.raven.gui.listitem.Message
import javafx.scene.layout.VBox

open class MessageContentItem : VBox() {

    lateinit var parentMessage: Message


    fun requestRefresh() {
        parentMessage.requestRefresh()
    }

    open fun onWidthChanged(newWidth: Double) {
        // nothing until overridden
    }

}