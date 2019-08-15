package com.savagellc.raven.gui.listitem.content

class MetaContentItem(edited: Boolean, revision: Int) : TextContentItem() {

    init {

        if (revision > 0)
            text += "Revision $revision"
        style = "-fx-font-size: 12;"
        isUnderline = true
    }

}
