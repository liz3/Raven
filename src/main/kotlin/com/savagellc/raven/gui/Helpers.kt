package com.savagellc.raven.gui

import javafx.scene.Cursor
import javafx.scene.Node
import java.awt.Desktop
import java.net.URI

fun cursourOnHover(node:Node, target:Cursor = Cursor.HAND, default: Cursor = Cursor.DEFAULT ) {
    node.setOnMouseEntered {
        node.scene.cursor = target
    }
    node.setOnMouseExited {
        node.scene.cursor = default
    }
}
fun browse(url:String) {
  Thread {
      Desktop.getDesktop().browse(URI(url))
  }.start()
}