package com.savagellc.raven.gui.listitem.content

import com.savagellc.raven.gui.listitem.Message
import com.savagellc.raven.gui.renders.maxImageWidth
import javafx.scene.control.Label
import javafx.scene.layout.VBox

interface MessageContentItem {
    fun requestRefresh()

    fun updateWidth(newWidth: Double)
}

open class ComplexContentItem : MessageContentItem, VBox() {

    lateinit var parentMessage: Message


    final override fun requestRefresh() {
        parentMessage.requestRefresh()
    }

    override fun updateWidth(newWidth: Double) {
        // nothing until overridden
    }

}

open class TextContentItem : MessageContentItem, Label() {

    lateinit var parentMessage: Message

    init {
        maxWidth = maxImageWidth - 80
        isWrapText = true
    }

    final override fun requestRefresh() {
        parentMessage.requestRefresh()
    }

    override fun updateWidth(newWidth: Double) {
        // nothing until overridden
    }

}