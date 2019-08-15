package com.savagellc.raven.gui.listitem.content.shared

import com.savagellc.raven.gui.renders.maxImageWidth
import javafx.scene.image.ImageView
import javafx.scene.image.WritableImage
import kotlin.math.min

class Thumbnail(image: WritableImage, var chatWidth: Double, val preferredImageWidth: Double) : ImageView(image) {

    init {
        isPreserveRatio = true
        computeWidth()
    }

    fun updateChatWidth(newWidth: Double) {
        chatWidth = newWidth
        computeWidth()
    }

    private fun computeWidth() {
        var computed = min(maxImageWidth - 100, chatWidth - 100)
        computed = min(computed, preferredImageWidth - 100)
        fitWidth = computed
    }

}