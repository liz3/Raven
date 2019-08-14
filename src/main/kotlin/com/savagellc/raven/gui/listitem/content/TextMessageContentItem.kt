package com.savagellc.raven.gui.listitem.content

import com.savagellc.raven.gui.renders.getLabel
import com.savagellc.raven.gui.renders.processMentions
import com.savagellc.raven.include.GuiMessage

class TextMessageContentItem(message: GuiMessage) : MessageContentItem() {

    init {
        val contentLabel =
            getLabel(processMentions(message.rootObj.getJSONArray("mentions"), message.content), "-fx-font-size: 15;")
        children.add(contentLabel)
    }

}