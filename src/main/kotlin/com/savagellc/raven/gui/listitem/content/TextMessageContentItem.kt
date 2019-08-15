package com.savagellc.raven.gui.listitem.content

import com.savagellc.raven.gui.renders.processMentions
import com.savagellc.raven.include.GuiMessage

class TextMessageContentItem(message: GuiMessage) : TextContentItem() {

    init {
        text = processMentions(message.rootObj.getJSONArray("mentions"), message.content)
        style = "-fx-font-size: 15;"
    }

}