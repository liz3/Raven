package com.savagellc.raven.gui.listitem.content

import com.savagellc.raven.gui.renders.getLabel

class StatusMessageContentItem() : MessageContentItem() {

    init {
        // TODO more status items
        val contentLabel = getLabel("Started call", style = "-fx-font-size: 15; -fx-font-style: italic;")
        children.add(contentLabel)
    }

}