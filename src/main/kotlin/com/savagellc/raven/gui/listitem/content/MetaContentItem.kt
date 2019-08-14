package com.savagellc.raven.gui.listitem.content

import com.savagellc.raven.gui.renders.getLabel

class MetaContentItem(edited: Boolean, revision: Int) : MessageContentItem() {

    init {
        var text = ""
        if (edited)
            text += "(edited)"
        if (revision > 0)
            text += "Revision $revision"
        if (!text.isEmpty())
            children.add(getLabel(text, "-fx-font-size: 12; -fx-font-style: italic"))
    }

}
