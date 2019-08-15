package com.savagellc.raven.gui.listitem.content

import com.savagellc.raven.gui.listitem.Message
import com.savagellc.raven.gui.renders.maxImageWidth
import javafx.scene.control.Label
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

open class TextItem : Label() {

    lateinit var parentMessage: Message

    init {
        maxWidth = maxImageWidth - 80
        isWrapText = true;

    }

    fun requestRefresh() {
        parentMessage.requestRefresh()
    }

    open fun onWidthChanged(newWidth: Double) {
        // nothing until overridden
    }

}