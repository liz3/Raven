package com.savagellc.raven.gui.listitem.content

import com.savagellc.raven.gui.renders.getLabel

class MetaContentItem(edited: Boolean, revision: Int) : TextItem() {

    init {

        if (revision > 0)
            text += "Revision $revision"
        style = "-fx-font-size: 12;"
        isUnderline = true
    }

}
